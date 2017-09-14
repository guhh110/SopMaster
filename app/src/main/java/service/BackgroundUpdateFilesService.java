package service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.guhh.sopmaster.LoginActivity;
import com.guhh.sopmaster.MainActivity;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import entity.FilesEntity;
import util.DataProtocol;
import util.RequestCmd;
import util.UserData;
import util.Util;

public class BackgroundUpdateFilesService extends Service {
    private String TAG = "BUFS";

    private boolean isRunning = false;
    private Socket socket;
    private InputStream is;
    private PrintWriter printWriter;
    private ScheduledExecutorService singleThreadPool;
    private GetFilesUrlRunnable getFilesUrlRunnable;

    private Util util;
    public BackgroundUpdateFilesService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        if(util == null)
            util = new Util(getBaseContext());
        Log.i(TAG,"onCreate");
        //初始化线程池
        if(singleThreadPool != null){
            singleThreadPool.shutdownNow();
            singleThreadPool = null;
        }
        singleThreadPool = Executors.newSingleThreadScheduledExecutor();

        //初始化定时执行的Runnable
        if(getFilesUrlRunnable==null)
            getFilesUrlRunnable = new GetFilesUrlRunnable();

        singleThreadPool.scheduleAtFixedRate(getFilesUrlRunnable,0,UserData.getFilesDelay, TimeUnit.MILLISECONDS);//开启定时器  3秒执行一次GetFilesUrlRunnable
        isRunning = true;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        Log.i(TAG,"onDestroy");
        if(singleThreadPool!=null){
            List<Runnable> a = singleThreadPool.shutdownNow();
            Log.i(TAG, a.size()+"-");
            singleThreadPool = null;
        }
        closeSocket();
        super.onDestroy();
    }

    private void closeSocket(){
        Log.i(TAG,"关闭socket");
        try {
            if(is!=null){
                is.close();
            }
            if(printWriter!=null){
                printWriter.close();
            }
            if(socket!=null){
                socket.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void connectSocket(){
        try {
            Log.i(TAG,"连接socket");
            InetSocketAddress socketAddress = new InetSocketAddress(UserData.ip,UserData.port);
            socket = new Socket();
            socket.connect(socketAddress,UserData.socketConnectTime);//设置连接超时时间
            socket.setSoTimeout(UserData.socketSoTime);//设置读取数据超时时间
            is = socket.getInputStream();
            printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            boolean loginResult = login();//重新登录
            if(loginResult){
                //连接服务器成功
                Intent intent = new Intent(UserData.CONNECT_SERVER_SUCCESS_ACTION);
                sendBroadcast(intent);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG,"连接socket失败"+e.toString());
        }
    }

    //重连之后需要重新登录
    private boolean login() throws IOException {
        String login_cmd = DataProtocol.makeCmd(RequestCmd.Command.LOGIN,UserData.enCodeStation);//拼装登录命令
        String result_login = util.sendCmdAndGetResult(is,printWriter,login_cmd);
        String[] results = result_login.split(" ");
        if(results.length>=5){
            if(results[0].equals("LOGINRESULT") && results[1].equals("0200")){//登录成功  保存数据
                return true;
            }else{//登录失败  并提示原因
                try {
                    String msg = new String(Base64.decodeBase64(results[2].getBytes()));
                    JSONObject root = new JSONObject(msg);
                    String reason = root.getString("msg");
                    Log.e(TAG,reason);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG,"解析json数据错误");
                }
                return false;
            }
        }
        return false;
    }

    private class GetFilesUrlRunnable implements Runnable{

        @Override
        public void run() {
            Log.i(TAG,"run");
            try {
                //初始化socket
                if(isRunning){//service 执行中
                    if(socket==null || socket.isClosed() || !socket.isConnected()){
                        connectSocket();
                    }

                    String cmd_getFile = DataProtocol.makeCmd(RequestCmd.Command.REQUESTWORK,UserData.enCodeStation);
                    String result_getFile =  util.sendCmdAndGetResult(is,printWriter,cmd_getFile);
                    if(result_getFile == null || result_getFile.equals("")){//socket已断开  重新连接
                        closeSocket();//先关闭之前socket
                        connectSocket();//重新连接socket 并 进行登录
                        return;
                    }
                    String[] result_getFile_arr = result_getFile.split(" ");
                    if(result_getFile_arr.length==5){//确定分割后的长度
                        String status_code = result_getFile_arr[1];
                        if(status_code.equals("0200")){//获取文件成功
                            String fileUrls = new String(Base64.decodeBase64(result_getFile_arr[2].getBytes()));
                            try {
                                List<FilesEntity> entitys = JSON.parseArray(fileUrls, FilesEntity.class);
                                    boolean isSample = entitys.equals(UserData.filesEntities);
                                    if(!isSample){//不同->发送广播->让MainActivity更新文件
                                        //到这里说明文件url有效 并且是最新的  所以保存在本地  用于没有网络的时候显示
                                        Log.i(TAG,fileUrls+"-===");
                                        util.saveFileUrls(fileUrls);//保存到本地
                                        UserData.filesEntities = entitys;//保存到全局变量
                                        Intent intent = new Intent(UserData.NEW_FILE_ACTION);
                                        sendBroadcast(intent);//通知MainActivity更新文件
                                    }
                                    Log.i(TAG,isSample+"--");
                            }catch (Exception e){
                                e.printStackTrace();
                                Log.i(TAG,e+"--");
                            }
                        }else if(status_code.equals("0701")){//因为重连之后有重新登录的操作 一般不会执行这一块
                            login();//重新登录
                            Log.e(TAG,"获取文件失败，未登录");
                        }
                    }
                    Log.i(TAG,result_getFile+"-");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
