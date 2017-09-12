package util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.danikula.videocache.HttpProxyCacheServer;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;

/**
 * Created by sunpn on 2017/8/31.
 */

public class Util {
    private Context context;

    public Util(Context context){
        this.context = context;
    }

    public Util(){

    }

    public boolean ipCheck(String text) {
        if (text != null && !text.isEmpty()) {
            // 定义正则表达式
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                    +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            // 判断ip地址是否与正则表达式匹配
            if (text.matches(regex)) {
                // 返回判断信息
                return true;
            } else {
                // 返回判断信息
                return false;
            }
        }
        return false;
    }

    public void saveLoginData(String ip,String port,String station){
        //保存到本地
        SharedPreferences.Editor editor = context.getSharedPreferences("loginData",Context.MODE_PRIVATE).edit();
        editor.putString("ip",ip);
        editor.putString("port",port);
        editor.putString("station",station);
        editor.apply();
        //保存到全局变量
        UserData.ip = ip;
        UserData.port = Integer.parseInt(port);
        UserData.station = station;
        UserData.enCodeStation = new String(Base64.encodeBase64(station.getBytes()));//加密工站编号
    }

    public HashMap<String,String> getLocalLoginData(){
        SharedPreferences sp = context.getSharedPreferences("loginData",Context.MODE_PRIVATE);
        HashMap<String,String> loginData = new HashMap<>();
        String station = sp.getString("station","");
        String ip = sp.getString("ip","");
        String port = sp.getString("port","");
        loginData.put("station",station);
        loginData.put("ip",ip);
        loginData.put("port",port);
        return loginData;
    }

    public String sendCmdAndGetResult(String ip, int port, String cmd) throws IOException {
        Socket socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(ip, port);
        socket.connect(socketAddress,1000);
        socket.setSoTimeout(10000);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        InputStream is = socket.getInputStream();
        pw.print(cmd);
        pw.flush();
        String result = getResult(is);

        pw.close();
        is.close();
        socket.close();
        return result;
    }

    public String sendCmdAndGetResult(InputStream is,PrintWriter printWriter, String cmd) throws IOException {
        printWriter.print(cmd);
        printWriter.flush();
        Log.i("Send",cmd);
        String result = getResult(is);
        return result;
    }

    private String getResult(InputStream is) {
        String receive = "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(640);
        byte[] buffer = new byte[128];
        String result = "";
        int length;

        // read()会一直阻塞
        try {
            while((length = is.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
                // 转码后读取数据
                receive = byteArrayOutputStream.toString("UTF-8");
                byteArrayOutputStream = new ByteArrayOutputStream();
                result+=receive;
                if(result.contains("<END>")){
                    return result;
                }
            }
        } catch (SocketTimeoutException e) {
            Log.i("sssddd",e.toString());
            return result;
        }catch (IOException e) {
            Log.i("sssddd",e.toString());
            return result;
        }
        return result;
    }

    public boolean isMP4(String url) {
        return url.endsWith("mp4")||url.endsWith("MP4");
    }
}
