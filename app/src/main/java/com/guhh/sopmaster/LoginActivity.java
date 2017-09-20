package com.guhh.sopmaster;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import dialog.SettingDialog;
import entity.FilesEntity;
import util.DataProtocol;
import util.RequestCmd;
import util.UserData;
import util.Util;
import dialog.CustomProgress;

public class LoginActivity extends AppCompatActivity {
    private String TAG = "LOGIN";
    private CustomProgress loading_dialog;

    private Util util;
    private Button login_btn;
    private EditText station_et;
    private EditText ip_et;
    private EditText port_et;

    //是否自动登录
    private boolean needAutoLogin = true;

    private ImageButton changScreenOrientation_btn;
    private ImageButton more_btn;

    //
    private SettingDialog settingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        hideBottomUIMenu();

        util = new Util(getBaseContext());
        needAutoLogin = getIntent().getBooleanExtra("needAutoLogin", true);
        iniView();
        //设置登录按钮监听
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取用户输入的内容
                String station = station_et.getText().toString().trim();
                String ip = ip_et.getText().toString().trim();
                String port = port_et.getText().toString().trim();

                //验证用户输入的内容是否正确
                if ("".equals(station)) {
                    station_et.requestFocus();
                    station_et.setError("请输入工站编号");
                } else if ("".equals(ip)) {
                    ip_et.setError("请输入IP");
                    ip_et.requestFocus();
                } else if (!util.ipCheck(ip)) {
                    ip_et.setError("IP不合法");
                    ip_et.requestFocus();
                } else if ("".equals(port)) {
                    port_et.setError("请输入端口号");
                    port_et.requestFocus();
                } else {//到这里说明用户输入的内容全部正确

                    //保存登录数据到全局变量
                    UserData.ip = ip_et.getText().toString().trim();
                    UserData.port = Integer.parseInt(port_et.getText().toString());
                    UserData.station = station_et.getText().toString().trim();
                    UserData.enCodeStation = new String(Base64.encodeBase64(station_et.getText().toString().trim().getBytes()));//加密工站编号

                    //获取本地保存的文件数据 并保存到全局变量
                    String fileUrls = util.getFilUrls();
                    List<FilesEntity> entitys = JSON.parseArray(fileUrls, FilesEntity.class);
                    UserData.filesEntities = entitys;

                    //判断网络状态
                    if (util.isNetWorkConnect()) {//网络已连接
                        //开启一个线程去登录
                        LoginThread loginThread = new LoginThread();
                        loginThread.execute(ip, port, station);
                        loading_dialog = CustomProgress.show(LoginActivity.this, "登录中...", false, null);
                    } else {//没有网络

                        //跳转到界面 在MainActivity开启后台更新文件服务
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        LoginActivity.this.finish();
                        Toast.makeText(getBaseContext(), "没有网络，显示已缓存的文件！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });



        changScreenOrientation_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentOrientation = getResources().getConfiguration().orientation;
                if(currentOrientation == Configuration.ORIENTATION_PORTRAIT){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                }else if(currentOrientation == Configuration.ORIENTATION_LANDSCAPE){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                }
            }
        });

        more_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingDialog = SettingDialog.show(LoginActivity.this,true);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        //控件赋值
        HashMap<String, String> loginData = util.getLocalLoginData();
        String ip = loginData.get("ip");
        String port = loginData.get("port");
        String station = loginData.get("station");
        if (!ip.equals("") && !port.equals("") && !station.equals("")) {
            ip_et.setText(ip);
            port_et.setText(port);
            station_et.setText(station);
            if (needAutoLogin) {
                login_btn.performClick();
                Toast.makeText(getBaseContext(), "已存在登录信息，自动登录", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //初始化控件
    private void iniView() {
        more_btn = (ImageButton) findViewById(R.id.more_btn);
        changScreenOrientation_btn = (ImageButton) findViewById(R.id.changScreenOrientation_btn);

        login_btn = (Button) findViewById(R.id.login_btn);
        station_et = (EditText) findViewById(R.id.station_et);
        ip_et = (EditText) findViewById(R.id.ip_et);
        port_et = (EditText) findViewById(R.id.port_et);
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);

            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    private class LoginThread extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String ip = params[0];
            int port = Integer.parseInt(params[1]);
            String station = new String(Base64.encodeBase64(params[2].getBytes()));//加密工站编号

            String login_cmd = DataProtocol.makeCmd(RequestCmd.Command.LOGIN, station);//拼装登录命令
            try {
                //发送登录命令并接收返回
                return util.sendCmdAndGetResult(ip, port, login_cmd);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String loginResult) {
            if (loading_dialog != null) {
                loading_dialog.dismiss();
            }
            if (loginResult == null || loginResult.equals("")) {
                Toast.makeText(getBaseContext(), "登录失败，服务器返回数据为空！", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.i(TAG, loginResult);
            String[] results = loginResult.split(" ");
            if (results.length >= 5) {
                String result_head = results[0];
                String result_code = results[1];
                if (result_head.equals("LOGINRESULT")) {//首先确认是正确的数据
                    if (result_code.equals("0200")) {//登录成功  保存数据
                        util.saveLoginData(ip_et.getText().toString().trim(), port_et.getText().toString(), station_et.getText().toString().trim());

                        //跳转到界面 在MainActivity开启后台更新文件服务
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        LoginActivity.this.finish();
                    }else if(result_code.equals("0801")){//登录失败，超过点数
                        Toast.makeText(getBaseContext(),"登录失败，已经超过点数！",Toast.LENGTH_SHORT).show();

                    } else {//登录失败  并提示原因
                        try {
                            String msg = new String(Base64.decodeBase64(results[2].getBytes()));
                            JSONObject root = new JSONObject(msg);
                            String reason = root.getString("msg");
                            Toast.makeText(getBaseContext(), "登录失败" + reason, Toast.LENGTH_SHORT).show();
                            Log.i("sssddd", reason);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getBaseContext(), "解析json数据错误", Toast.LENGTH_SHORT).show();
                        }

                    }
                }else{
                    Toast.makeText(getBaseContext(), "登录失败，服务器返回数据不正确！", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
