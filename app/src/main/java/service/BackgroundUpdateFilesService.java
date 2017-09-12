package service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import org.apache.commons.codec.binary.Base64;

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

    private Util util = new Util();
    public BackgroundUpdateFilesService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"onCreate");
        //初始化线程池
        if(singleThreadPool != null){
            singleThreadPool.shutdownNow();
            singleThreadPool = null;
        }
        singleThreadPool = Executors.newScheduledThreadPool(1);

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
        super.onDestroy();
        if(singleThreadPool!=null){
            List<Runnable> a = singleThreadPool.shutdownNow();
            Log.i(TAG, a.size()+"-");
            singleThreadPool = null;
        }
        closeSocket();
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
            socket.connect(socketAddress,1000);//设置连接超时时间
            socket.setSoTimeout(10000);//设置读取数据超时时间
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG,"连接socket失败"+e.toString());
        }
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
                    if(is == null)
                        is = socket.getInputStream();
                    if(printWriter == null)
                        printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

                    String cmd_getFile = DataProtocol.makeCmd(RequestCmd.Command.REQUESTWORK,UserData.enCodeStation);
                    String result_getFile =  util.sendCmdAndGetResult(is,printWriter,cmd_getFile);
                    String[] result_getFile_arr = result_getFile.split(" ");
                    if(result_getFile_arr.length==5){//确定分割后的长度
                        if(result_getFile_arr[1].equals("0200")){//获取文件成功
                            String fileUrls = new String(Base64.decodeBase64(result_getFile_arr[2].getBytes()));
                            try {
                                List<FilesEntity> entitys = JSON.parseArray(fileUrls, FilesEntity.class);
                                if(UserData.filesEntities==null){//为空说明第一次获取到文件  直接更新
                                    UserData.filesEntities = entitys;
                                    Intent intent = new Intent(UserData.NEW_FILE_ACTION);
                                    sendBroadcast(intent);
                                }else{//不为空 比较文件顺序 和 文件个数
                                    boolean isSample = UserData.filesEntities.equals(entitys);
                                    if(!isSample){//不同 发送广播 让MainActivity更新文件
                                        UserData.filesEntities = entitys;
                                        Intent intent = new Intent(UserData.NEW_FILE_ACTION);
                                        sendBroadcast(intent);
                                    }
                                    Log.i(TAG,isSample+"--");
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                                Log.i(TAG,e+"--");
                            }
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
