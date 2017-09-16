package dialog;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.guhh.sopmaster.R;
import com.xw.repo.BubbleSeekBar;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.carbswang.android.numberpickerview.library.NumberPickerView;
import util.UserData;
import util.Util;

/**
 * Created by sunpn on 2017/9/5.
 */

public class SettingDialog extends Dialog {
    private Util util;

    private static String TAG = "SettingDialog";
    private TextView timeSet_tv;
    private TextView wifiSet_tv;
    private TextView netSet_tv;
    private TextView update_tv;
    private TextView languageSet_tv;

    private NumberPickerView hour_npv;
    private NumberPickerView minute_npv;
    private NumberPickerView second_npv;


    private static Context context;

//    private BubbleSeekBar bubbleSeekBar;

    //更新软件相关
    private SearchFileDialog searchFileDialog;
    private U_DiskAttachBroadCastReceiver udabcr;

    public SettingDialog(Context context) {
        super(context);
    }

    public SettingDialog(Context context, int theme) {
        super(context, theme);
    }

//    /**
//     * 当窗口焦点改变时调用
//     */
//    public void onWindowFocusChanged(boolean hasFocus) {
//        ImageView imageView = (ImageView) findViewById(R.id.spinnerImageView);
//        // 获取ImageView上的动画背景
//        AnimationDrawable spinner = (AnimationDrawable) imageView.getBackground();
//        // 开始动画
//        spinner.start();
//    }

    /**
     * 初始化控件
     *
     */
    public void intView() {
        HashMap<String, Integer> delay = util.getPageChangeDelayFormat();
//        bubbleSeekBar = (BubbleSeekBar) findViewById(R.id.my_seekbar);
//        bubbleSeekBar.setProgress(delay);

        timeSet_tv = (TextView) findViewById(R.id.timeSet_tv);
        wifiSet_tv = (TextView) findViewById(R.id.wifiSet_tv);
        netSet_tv = (TextView) findViewById(R.id.netSet_tv);
        update_tv = (TextView) findViewById(R.id.updateSet_tv);
        languageSet_tv = (TextView) findViewById(R.id.languageSet_tv);
        hour_npv = (NumberPickerView) findViewById(R.id.hour_npv);
        minute_npv = (NumberPickerView) findViewById(R.id.minute_npv);
        second_npv = (NumberPickerView) findViewById(R.id.second_npv);

        setData(hour_npv,0,23,delay.get("hour"));
        setData(minute_npv,0,59,delay.get("minute"));
        setData(second_npv,0,23,delay.get("second"));

        MyNumberPickerValueChangeListener myNumberPickerValueChangeListener = new MyNumberPickerValueChangeListener();
        hour_npv.setOnValueChangedListener(myNumberPickerValueChangeListener);
        minute_npv.setOnValueChangedListener(myNumberPickerValueChangeListener);
        second_npv.setOnValueChangedListener(myNumberPickerValueChangeListener);



//        bubbleSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
//            @Override
//            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
//
//            }
//
//            @Override
//            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
//
//            }
//
//            @Override
//            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
//                util.savePageChangeDelay(progress);//保存到本地
//                Intent intent = new Intent(UserData.PAGE_CHANGE_DELAY_CHANGED);
//                context.sendBroadcast(intent);//通知MainActivity 切换时间改变了
//            }
//        });

        timeSet_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings. ACTION_DATE_SETTINGS);
                context.startActivity(intent);
                dismiss();
            }
        });

        languageSet_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings. ACTION_INPUT_METHOD_SETTINGS);
                context.startActivity(intent);
                dismiss();
            }
        });

        wifiSet_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                context.startActivity(intent);
                dismiss();
            }
        });
        netSet_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                context.startActivity(intent);
                dismiss();
            }
        });
        update_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //显示dialog
                if(searchFileDialog == null){
                    searchFileDialog = SearchFileDialog.show(context,true);
                    searchFileDialog.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if(udabcr!=null){
                                context.unregisterReceiver(udabcr);
                            }
                        }
                    });
                }else{
                    searchFileDialog.setMessage("请插入U盘");
                    searchFileDialog.show();
                }

                //注册U盘广播
                if(udabcr == null){
                    udabcr = new U_DiskAttachBroadCastReceiver();
                }
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);//插
                intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);//拔
                intentFilter.addDataScheme("file");//没有这行监听不起作用
                context.registerReceiver(udabcr, intentFilter);
            }
        });
    }

    private void setData(NumberPickerView picker, int minValue, int maxValue, int value){
        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        picker.setValue(value);
    }

//    private File searchFiles() {
//        //初始化handle
//        if(handler == null){
//            handler = new Handler(){
//                @Override
//                public void handleMessage(Message msg) {
//                    switch (msg.what){
//                        case 1:
//                            if(searchFileDialog != null){
//                                searchFileDialog.setMessage("正在搜索文件...");
//                            }
//                            break;
//                        case 2:
//                            if(searchFileDialog != null){
//                                searchFileDialog.setMessage("搜索完成！没有发现更新包！");
//                            }
//                            break;
//                        case 3:
//                            if(searchFileDialog != null){
//                                searchFileDialog.setMessage("搜索完成！");
//                            }
//                            break;
//
//                    }
//                    String msg_str = (String) msg.obj;
//
//                }
//            };
//        }
//
//        //显示dialog
//        if(searchFileDialog == null){
//            searchFileDialog = SearchFileDialog.show(context,true);
//        }else{
//            searchFileDialog.show();
//        }
//
//        //注册U盘插入广播
//        File file = new File(Environment.getExternalStorageDirectory()+"/"+"SopMaster.apk");
//        if(file.exists()){
//            Intent intent = getApkFileIntent(file);
//            context.startActivity(intent);
//        }
//
//        return null;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        util = new Util(getContext());
        intView();
    }

    /**
     * 弹出自定义ProgressDialog
     *
     * @param context
     *            上下文
     * @return
     */
    public static SettingDialog show(Context context,boolean cancelable) {
        SettingDialog.context = context;
        SettingDialog dialog = new SettingDialog(context, R.style.Custom_Progress);
        dialog.setTitle("");
        dialog.setContentView(R.layout.dialog_setting);

        // 按返回键是否取消
        dialog.setCancelable(cancelable);
        // 监听返回键处理
//        dialog.setOnCancelListener(cancelListener);
        // 设置居中

        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        // 设置背景层透明度
        lp.dimAmount = 0.2f;
        dialog.getWindow().setAttributes(lp);
        // dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        dialog.show();
        return dialog;
    }

    class U_DiskAttachBroadCastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG,action);
            if(action.equals(Intent.ACTION_MEDIA_MOUNTED)){
                searchFileDialog.setMessage("已检测到U盘！");
                String path = intent.getDataString();
                Log.i(TAG,path+"--path");
                String pathString = path.split("file://")[1] + "/";//U盘路径
                int currentVersionCode = getVersionCode();//获取当前版本号
                File[] apkFiles = new File(pathString).listFiles(new ApkFileFilter());//获取根目录下的所有apk
                Log.i(TAG,Arrays.toString(apkFiles));
                HashMap<String,String> newVersionApkInfo = getNewVersionApk(apkFiles,currentVersionCode);//获取根目录下的最新的SOPMaster安装包

                if(newVersionApkInfo != null){
                    String newVersionSopPath = newVersionApkInfo.get("path");
                    String newVersionCode = newVersionApkInfo.get("versionCode");
                    Toast.makeText(getContext(),"已经找到新版本的SOP，版本号为"+newVersionCode,Toast.LENGTH_SHORT).show();
                    File file = new File(newVersionSopPath);
                    Intent installApkIntent = getApkFileIntent(file);
                    context.startActivity(installApkIntent);
                    searchFileDialog.dismiss();

                }else{
                    searchFileDialog.setMessage("没有找到新版本的SOPMaster！");

                }

            }else if(action.equals(Intent.ACTION_MEDIA_UNMOUNTED)){
                searchFileDialog.setMessage("U盘已经移除！");
            }
        }
    }

    class MyNumberPickerValueChangeListener implements NumberPickerView.OnValueChangeListener {

        @Override
        public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
            long changePageDelay = (hour_npv.getValue() * 60 * 60  + minute_npv.getValue() * 60 + second_npv.getValue()) * 1000;
            Toast.makeText(getContext(),String.valueOf(changePageDelay),Toast.LENGTH_SHORT).show();
            util.savePageChangeDelay(changePageDelay);
            Intent intent = new Intent(UserData.PAGE_CHANGE_DELAY_CHANGED);
            context.sendBroadcast(intent);//通知MainActivity 切换时间改变了
//            int id = picker.getId();
//            switch (id){
//                case R.id.hour_npv:
//
//                    break;
//                case R.id.minute_npv:
//
//                    break;
//                case R.id.second_npv:
//
//                    break;
//            }
        }
    }

    /**
     * 获取版本号
     *
     * @return 当前应用的版本号
     */
    private int getVersionCode() {
        int vid = 0;
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            vid = info.versionCode;
        } catch (Exception e) {
            vid = -1;
        }
        return vid;
    }

    public class ApkFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            String filename = name.toLowerCase();
            if(filename.endsWith(".apk")){
                return true;
            }else{
                return false;
            }
        }
    }

    //Android获取一个用于打开APK文件的intent
    private Intent getApkFileIntent( File file ) {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri,"application/vnd.android.package-archive");
        return intent;
    }

    private HashMap<String,String> getNewVersionApk(File[] apks,int currentVersion){
        int newCode = 0; //找到的最新版本代码
        String newPath = "";
        for (File fs : apks) {
            if(searchFileDialog!=null){
                searchFileDialog.setMessage(fs.getAbsolutePath());
            }

            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(fs.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            if (info != null) {
                ApplicationInfo appInfo = info.applicationInfo;
                String appName = pm.getApplicationLabel(appInfo).toString();
                String packageName = appInfo.packageName;  //得到安装包名称
                //String version = info.versionName;       //得到版本信息
                int versioncode = info.versionCode;
                //String pkgInfoStr = String.format("PackageName:%s, Vesion: %s, AppName: %s ,VerCode: %s", packageName, version, appName, vercode);
                //Toast.makeText(this, pkgInfoStr, Toast.LENGTH_LONG).show();
                //Toast.makeText(this, "vercode:" + vercode + ",currCode:" + currCode + ",appName:" + appName + ",packageName:" + packageName, Toast.LENGTH_SHORT);
                if (versioncode >= currentVersion && appName.equals("SOPMaster") && packageName.equals("com.guhh.sopmaster")) {
                    if (versioncode > newCode) {
                        Log.i(TAG,versioncode+"-"+appName+"-"+packageName+"-"+fs);
                        newCode = versioncode;
                        newPath = fs.getAbsolutePath();
                    }
                }
            }else{
                Log.i(TAG,fs+"-null");
            }
        }

        if (newCode <= 0 || newCode < currentVersion){//没有找到新版本
            return null;
        }else {
            HashMap<String,String> newVersionApkInfo = new HashMap<>();
            newVersionApkInfo.put("path",newPath);
            newVersionApkInfo.put("versionCode", String.valueOf(newCode));
            return newVersionApkInfo;
        }
    }
}
