package util;

import android.content.Context;
import android.content.Entity;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by guhh on 2017/8/31.
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

    //保存登录信息
    public void saveLoginData(String ip,String port,String station){
        //保存到本地
        SharedPreferences.Editor editor = context.getSharedPreferences("loginData",Context.MODE_PRIVATE).edit();
        editor.putString("ip",ip);
        editor.putString("port",port);
        editor.putString("station",station);
        editor.apply();

    }

    //获取本地保存的登录信息
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


    //保存最新的文件urk
    public void saveFileUrls(String urls){
        //保存到本地
        SharedPreferences.Editor editor = context.getSharedPreferences("fileUrlsData",Context.MODE_PRIVATE).edit();
        editor.putString("urls",urls);
        editor.apply();
    }

    //获取最新的文件url
    public String getFilUrls(){
        SharedPreferences sp = context.getSharedPreferences("fileUrlsData",Context.MODE_PRIVATE);
        return  sp.getString("urls","");
    }

    //保存图片轮换时间
    public void savePageChangeDelay(long delay){
        //保存到本地
        SharedPreferences.Editor editor = context.getSharedPreferences("pageChangeDelay",Context.MODE_PRIVATE).edit();
        editor.putLong("delay",delay);
        editor.apply();
    }

    //获取图片轮换时间
    public long getPageChangeDelay(){
        SharedPreferences sp = context.getSharedPreferences("pageChangeDelay",Context.MODE_PRIVATE);
        return  sp.getLong("delay",5000);
    }

    //获取图片轮换时间
    public HashMap<String,Integer> getPageChangeDelayFormat(){
        SharedPreferences sp = context.getSharedPreferences("pageChangeDelay",Context.MODE_PRIVATE);
        long changeDelay = sp.getLong("delay",5000);
        int ss = 1000;
        int mi = ss * 60;
        int hh = mi * 60;
        int dd = hh * 24;

        int day = (int) (changeDelay / dd);
        int hour = (int) ((changeDelay - day * dd) / hh);
        int minute = (int) ((changeDelay - day * dd - hour * hh) / mi);
        int second = (int) ((changeDelay - day * dd - hour * hh - minute * mi) / ss);


        HashMap<String,Integer> date = new HashMap<>();
        date.put("hour",hour);
        date.put("minute",minute);
        date.put("second",second);
        return date;
    }

    public String sendCmdAndGetResult(String ip, int port, String cmd) throws IOException {
        Socket socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(ip, port);
        socket.connect(socketAddress,UserData.socketConnectTime);
        socket.setSoTimeout(UserData.socketSoTime);
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

    public boolean isNetWorkConnect(){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if(networkInfo != null)
            return networkInfo.isAvailable();

        return false;
    }
}
