package dialog;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.guhh.sopmaster.R;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sunpn on 2017/9/5.
 */

public class SettingDialog extends Dialog {
    private static String TAG = "SettingDialog";
    private TextView sysSet_tv;
    private TextView wifiSet_tv;
    private TextView netSet_tv;
    private TextView update_tv;
    private static Context context;

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
        sysSet_tv = (TextView) findViewById(R.id.sysSet_tv);
        wifiSet_tv = (TextView) findViewById(R.id.wifiSet_tv);
        netSet_tv = (TextView) findViewById(R.id.netSet_tv);
        update_tv = (TextView) findViewById(R.id.updateSet_tv);

        sysSet_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
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
            if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){
                String path = intent.getDataString();
                String pathString = path.split("file://")[1] + "/";//U盘路径
                File file = new File(pathString+"SopMaster.apk");
                if(file.exists()){
                    Intent intent_openApk = getApkFileIntent(file);
                    context.startActivity(intent_openApk);
                    searchFileDialog.dismiss();
                }else{
                    searchFileDialog.setMessage("没有找到更新文件！");
                }
            }else{

            }
            context.unregisterReceiver(udabcr);//反注册广播
        }
    }


    public class ApkFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            String filename = pathname.getName().toLowerCase();
            if(filename.endsWith(".apk")){
                return true;
            }else{
                return false;
            }
        }
    }

    //Android获取一个用于打开APK文件的intent
    public static Intent getApkFileIntent( File file ) {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri,"application/vnd.android.package-archive");
        return intent;
    }
}
