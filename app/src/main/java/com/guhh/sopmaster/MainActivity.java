package com.guhh.sopmaster;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.convenientbanner.ConvenientBanner;
import com.bigkoo.convenientbanner.holder.CBViewHolderCreator;
import com.bigkoo.convenientbanner.holder.Holder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.danikula.videocache.HttpProxyCacheServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.ManagerFactoryParameters;

import entity.FilesEntity;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import service.BackgroundUpdateFilesService;
import tcking.github.com.giraffeplayer.GiraffePlayer;
import tcking.github.com.giraffeplayer.GiraffePlayerActivity;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import util.UserData;
import util.Util;
import dialog.SettingDialog;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    private long offlineTime = 0;//记录离线时间
    private int videoPlayerSeekPosition = 0;

    private Util util;

    private static String TAG = "MainActivity";

    private MyBroadCastReceiver myBroadCastReceiver;

    private ConvenientBanner banner;
    private List<String> pages;
    private RequestOptions options = new RequestOptions()
            .centerInside()
            .placeholder(R.mipmap.loading1)
            .error(R.mipmap.error1)
            .priority(Priority.LOW);
    private GiraffePlayer videoPlayer;
    private View video_view;
    private TextView noPage_tv;

    //控制面板
    private boolean isControlShowing = false;//记录控制栏的状态
    private LinearLayout ctrl_ll;
    private ImageButton prev_ib;
    private ImageButton next_ib;
    private ImageButton pause_ib;
    private TextView pageNumber_tv;
    private Handler handler_showOrHideCtrl = new Handler();
    private Runnable runnable_showOrHideCtrl = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG,"hide");
            isControlShowing = false;
            showOrHideControl(isControlShowing);
        }
    };

    //toolBar
    private LinearLayout toolBar_ll;
    private ImageButton exitLogin_btn;
    private ImageButton changScreenOrientation_btn;
    private ImageButton more_btn;

    //
    private SettingDialog settingDialog;

    //检查离线时间超时timer
    private Timer checkOfflineTimeoutTimer;
    private TimerTask checkOfflineTimeoutTimeTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        util = new Util(getBaseContext());
//        hideBottomUIMenu();//全屏
        iniView();
        iniPlayer();//初始化播放器



        pages = getUrls();
        banner.setPages(new CBViewHolderCreator<ImageViewHolder>() {
            @Override
            public ImageViewHolder createHolder() {
                return new ImageViewHolder();
            }
        },pages);
        banner.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(final int position) {
                Log.i(TAG,position+"-----onpageselect");
                if(UserData.filesEntities!=null && position<UserData.filesEntities.size()){//以防下标越界 空指针
                    //更新页码指示器
                    pageNumber_tv.setText(position+1+"/"+UserData.filesEntities.size());
                    if(UserData.filesEntities.get(position).isMp4()){//当前轮播item为视频
                        isControlShowing = false;//更改控制栏的状态
                        if(handler_showOrHideCtrl!=null)//取消之前添加的隐藏控制栏的延时操作
                            handler_showOrHideCtrl.removeCallbacks(runnable_showOrHideCtrl);
                        pause_ib.setVisibility(View.VISIBLE);//显示暂停按钮
                        Log.i(TAG,"当前轮播item为视频");
                        stopBannerPlay();//停止轮播
                        hideBannerShowVideoView();//隐藏banner
                        String video_url = getVideoUrl(UserData.filesEntities.get(position).getWocUrl());
                        //申请权限
                        MainActivityPermissionsDispatcher.startVideoPlayWithCheck(MainActivity.this,video_url);
//                        startVideoPlay(video_url);//开始播放视频

                    }else{//当前item为图片
                        pause_ib.setVisibility(View.GONE);//隐藏暂停按钮
                        stopVideoPlay();//停止播放视频
                        showBannerHideVideoView();//显示banner
                        startBannerPlay();//开始轮播
                    }
//                    updatePauseButtonStatus();//更新暂停按钮状态
                    Log.i("sssddd-pos",position+"-"+UserData.filesEntities.get(position).getWocUrl());
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if(pages.size()>1){
            pageNumber_tv.setText("1/"+pages.size());
            banner.setCanLoop(true);
        }else if(pages.size() == 1){
            pageNumber_tv.setText("1/"+pages.size());
            banner.setCanLoop(false);
        }else if(pages.size() == 0){
            pageNumber_tv.setText("0/0");
            showNoPageView();
        }
        banner.setScrollDuration(500);
        startBannerPlay();
        banner.getOnPageChangeListener().onPageSelected(0);//触发onPageSelected

        prev_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //重置切换页的冷却时间
                startBannerPlay();
                bannerToPrev();
            }
        });

        next_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //重置切换页的冷却时间
                startBannerPlay();
                bannerToNext();
            }
        });

        pause_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UserData.filesEntities ==null || UserData.filesEntities.size()-1<banner.getCurrentItem()){//防止下标越界
                    return;
                }
                if(UserData.filesEntities.get(banner.getCurrentItem()).isMp4()) {//当前轮播item为视频
                    videoPlayer.show(5000);//显示控制栏
                    Log.i(TAG,videoPlayer.isPlaying()+"--sss");
                    if(videoPlayer!=null && videoPlayer.isPlaying()){
                        pauseVideoPlay();//暂停视频播放
                        pause_ib.setImageResource(R.drawable.play_selector);
                    }else{
                        resumeVideoPlay();//恢复视频播放
                        pause_ib.setImageResource(R.drawable.pause_selector);
                    }
                }
            }

        });

        exitLogin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                intent.putExtra("needAutoLogin",false);//不要自动登录
                startActivity(intent);
                MainActivity.this.finish();
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
                    settingDialog = SettingDialog.show(MainActivity.this,true);
            }
        });

        noPage_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOrHideAndDelayHideCtrl();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideBottomUIMenu();//全屏

        //开启后台更新文件服务
        if(util.isNetWorkConnect()){
            Intent intent = new Intent(MainActivity.this, BackgroundUpdateFilesService.class);
            startService(intent);
        }

        //注册广播
        if(myBroadCastReceiver == null){
            myBroadCastReceiver = new MyBroadCastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UserData.NEW_FILE_ACTION);//更新文件action
        intentFilter.addAction(UserData.CONNECT_SERVER_SUCCESS_ACTION);//连接服务器超过action
        intentFilter.addAction(UserData.PAGE_CHANGE_DELAY_CHANGED);//轮换时间改变action
        intentFilter.addAction(UserData.GET_FILES_702);//获取文件返回702action
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);//网络状态改变action
        intentFilter.addAction(UserData.OFFLINE_TIME_OUT);
        registerReceiver(myBroadCastReceiver,intentFilter);

        //恢复视频播放
        resumeVideoPlay();
        Log.i(TAG,"onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();

        //停止后台更新文件服务
        Intent intent = new Intent(MainActivity.this, BackgroundUpdateFilesService.class);
        stopService(intent);

        //反注册广播
        unregisterReceiver(myBroadCastReceiver);

        //停止视频
        pauseVideoPlay();

        Log.i(TAG,"onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止后台更新文件服务
        Intent intent = new Intent(MainActivity.this, BackgroundUpdateFilesService.class);
        stopService(intent);

        //停止视频播放
        stopVideoPlay();

        //清除数据
//        UserData.clear();

        //停止轮播
        banner.stopTurning();

        Log.i(TAG,"onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG,"onSaveInstanceState");
        if(videoPlayer !=null){
            outState.putInt("videoPlayerSeekPosition",videoPlayer.getCurrentPosition());
            videoPlayerSeekPosition = videoPlayer.getCurrentPosition();
            Log.i(TAG,"onSaveInstanceState"+videoPlayer.getCurrentPosition());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(TAG,"onRestoreInstanceState");
        videoPlayerSeekPosition =  savedInstanceState.getInt("videoPlayerSeekPosition",0);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
//        showOrHideControl();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
     void showRequestPermissionTip(final PermissionRequest request){
        new AlertDialog.Builder(this)
                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton("不给", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage("视频和图片缓存需要写入权限")
                .show();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
     void showPermissionDeniedTip(){
        Toast.makeText(getBaseContext(),"拒绝写入权限视频和图片将无法缓存",Toast.LENGTH_SHORT).show();
    }
    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
     void showPermissionNeverAsk(){
        new AlertDialog.Builder(this)
                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage("您已经禁止了写入权限,是否现在去开启")
                .show();
    }

    private void iniPlayer(){
        //播放器设置
        if(videoPlayer == null)
            videoPlayer = new GiraffePlayer(MainActivity.this);
        videoPlayer.setScaleType(GiraffePlayer.SCALETYPE_16_9);
        videoPlayer.setFullScreenOnly(false);
        videoPlayer.onControlPanelVisibilityChange(new GiraffePlayer.OnControlPanelVisibilityChangeListener() {
            @Override
            public void change(boolean isShowing) {
                if(UserData.filesEntities!=null && UserData.filesEntities.size()>0 && UserData.filesEntities.get(banner.getCurrentItem()).isMp4()) {//当前轮播item为视频
                    isControlShowing = isShowing;
                    showOrHideControl(isShowing);//控制栏的显示和隐藏
                }
            }
        });
        videoPlayer.onComplete(new Runnable() {
            @Override
            public void run() {//播放完成，下一首
                if(UserData.filesEntities.size() == 1 || util.getPageChangeDelay()<=0){//如果只有一个文件 或者 不轮换 就重复播放当前视频
                    String video_url = getVideoUrl(UserData.filesEntities.get(banner.getCurrentItem()).getWocUrl());
                    //申请权限
                    MainActivityPermissionsDispatcher.startVideoPlayWithCheck(MainActivity.this,video_url);
//                    startVideoPlay(video_url);//开始播放视频
                }else{
                    showBannerHideVideoView();//显示banner
                    startBannerPlay();//开始轮播
                    bannerToNext();//下一个
                }
            }
        });
        videoPlayer.onError(new GiraffePlayer.OnErrorListener() {
            @Override
            public void onError(int what, int extra) {
                Toast.makeText(getBaseContext(), "不支持此类视频的播放，即将播放下一个。", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startBannerPlay();//开始轮播
                        showBannerHideVideoView();//显示banner
                        bannerToNext();//下一个
                    }
                }, 3000);
            }
        });

    }

    //停止轮播
    private void stopBannerPlay(){
        if(banner!=null){
            banner.startTurning(9999999);//暂停
        }
    }

    //开始轮播
    private void startBannerPlay(){
        if(banner!=null){
            long delay = util.getPageChangeDelay();
            Log.i(TAG,delay+"+delay");
            if(delay>0){
                banner.startTurning(delay);
            }else{
                stopBannerPlay();
            }
        }
    }

    //开始播放视频
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)//动态申请权限
    void startVideoPlay(String url){
//        Toast.makeText(getBaseContext(),url,Toast.LENGTH_SHORT).show();
        Log.i(TAG,url);
        if(pause_ib!=null){
            pause_ib.setImageResource(R.drawable.pause_selector);
        }
        if(videoPlayer!=null){
            if(videoPlayer.isPlaying()){
                videoPlayer.stop();
            }
            videoPlayer.play(url);
        }
    }

    //停止播放视频
    private void stopVideoPlay(){
        if(pause_ib!=null){
            pause_ib.setImageResource(R.drawable.play_selector);
        }
        if(videoPlayer!=null){
            videoPlayer.stop();
        }
    }

    //暂停播放视频
    private void pauseVideoPlay(){
        if(videoPlayer!=null){
            videoPlayer.pause();
        }
    }

    //恢复播放视频
    private void resumeVideoPlay(){
        Log.i(TAG,"resumeVideoPlay"+videoPlayerSeekPosition);
        if(videoPlayer!=null){
            videoPlayer.start();
            if(videoPlayerSeekPosition>0){
                videoPlayer.seekTo(videoPlayerSeekPosition-5*1000,true);
                videoPlayerSeekPosition = 0;
            }
        }
    }

    //下一个
    private void bannerToNext(){
        //重置控制栏的隐藏
        if(handler_showOrHideCtrl!=null)
            handler_showOrHideCtrl.removeCallbacks(runnable_showOrHideCtrl);
        handler_showOrHideCtrl.postDelayed(runnable_showOrHideCtrl,5000);

        if(banner!=null) {
            banner.setcurrentitem(banner.getCurrentItem() + 1);
        }
    }

    //上一个
    private void bannerToPrev(){
        //重置控制栏的隐藏
        if(handler_showOrHideCtrl!=null)
            handler_showOrHideCtrl.removeCallbacks(runnable_showOrHideCtrl);
        handler_showOrHideCtrl.postDelayed(runnable_showOrHideCtrl,5000);

        if(UserData.filesEntities == null)
            return;

        if(banner!=null){
            if(banner.getCurrentItem()==0){
                banner.setcurrentitem(UserData.filesEntities.size()-1);
            }else{
                banner.setcurrentitem(banner.getCurrentItem() - 1);
            }

        }
    }

//    private void updatePauseButtonStatus(){
//            if(videoPlayer.is){
//                pause_ib.setImageResource(R.drawable.pause_selector);
//            }else{
//                pause_ib.setImageResource(R.drawable.play_selector);
//            }
//    }

    private void hideBannerShowVideoView(){
        if(banner!=null){
            banner.setVisibility(View.GONE);
        }
        if(video_view !=null){
            video_view.setVisibility(View.VISIBLE);
        }
        if(noPage_tv!=null){
            noPage_tv.setVisibility(View.GONE);
        }
    }

    private void showBannerHideVideoView(){
        if(banner!=null){
            banner.setVisibility(View.VISIBLE);
        }
        if(video_view !=null){
            video_view.setVisibility(View.GONE);
        }
        if(noPage_tv!=null){
            noPage_tv.setVisibility(View.GONE);
        }
    }

    private void showNoPageView(){
        if(banner!=null){
            banner.setVisibility(View.GONE);
        }
        if(video_view !=null){
            video_view.setVisibility(View.GONE);
        }
        if(noPage_tv!=null){
            noPage_tv.setVisibility(View.VISIBLE);
        }
    }

    private void showOrHideControl(boolean isShowing){
        if(isShowing){
            if(ctrl_ll!=null){
                ctrl_ll.setVisibility(View.VISIBLE);
            }
            if(toolBar_ll!=null){
                toolBar_ll.setVisibility(View.VISIBLE);
            }
        }else{
            if(ctrl_ll!=null){
                ctrl_ll.setVisibility(View.GONE);
            }
            if(toolBar_ll!=null){
                toolBar_ll.setVisibility(View.GONE);
            }
        }

    }
    //获取经过处理的视频播放地址
    private String getVideoUrl(String url){
        HttpProxyCacheServer proxy = getProxy();
        String proxyUrl = proxy.getProxyUrl(url);
        Log.i("sssddd",proxyUrl);
        return proxyUrl;
    }
    private HttpProxyCacheServer getProxy() {
        return App.getProxy(getApplicationContext());
    }

    //从UserData.filesEntities获取图片的地址
    private List<String> getUrls(){
        List<String> images_url = new ArrayList<>();
        if(UserData.filesEntities!=null){
            for(FilesEntity filesEntity:UserData.filesEntities){
                if(!filesEntity.isMp4()){
                    images_url.add(filesEntity.getWocUrl());
                }else{
                    images_url.add(filesEntity.getWocVideoImg());
                }
            }
        }
        return images_url;
    }

    private void iniView(){
        toolBar_ll = (LinearLayout) findViewById(R.id.toolBar_ll);
        more_btn = (ImageButton) findViewById(R.id.more_btn);
        changScreenOrientation_btn = (ImageButton) findViewById(R.id.changScreenOrientation_btn);
        exitLogin_btn = (ImageButton) findViewById(R.id.exit_login_btn);
        pageNumber_tv = (TextView) findViewById(R.id.pageNumber_tv);

        noPage_tv = (TextView) findViewById(R.id.noPage_tv);
        video_view = findViewById(R.id.include);
        banner = (ConvenientBanner) findViewById(R.id.convenientBanner);

        ctrl_ll = (LinearLayout) findViewById(R.id.ctrl_ll);
        prev_ib = (ImageButton) findViewById(R.id.prev_ib);
        next_ib = (ImageButton) findViewById(R.id.next_ib);
        pause_ib = (ImageButton) findViewById(R.id.pause_ib);
    }

    //广播接受者
    class MyBroadCastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG,action);
            if(action.equals(UserData.NEW_FILE_ACTION)){//有新文件
                //不管是否有在播放视频都停止播放视频 并 显示 banner  因为当视频在加载的时候更新文件的时候检查不到状态
                stopVideoPlay();
                showBannerHideVideoView();
                updatePages();//更新viewpager
                Toast.makeText(getBaseContext(),"发现新的作业，正在更新...",Toast.LENGTH_SHORT).show();

            }else if(action.equals(UserData.CONNECT_SERVER_SUCCESS_ACTION)){//后台service连接服务器成功
                Toast.makeText(getBaseContext(),"连接服务器成功",Toast.LENGTH_SHORT).show();

            }else if(action.equals(UserData.PAGE_CHANGE_DELAY_CHANGED)){//更改轮换时间
                if(!videoPlayer.isPlaying()){//如果当前不是视频播放就立即更换轮换时间  如果当前正在播放视频那么在播放视频完成之后就会更新轮换时间
                    startBannerPlay();//重新设置轮换时间
                }

            }else if(action.equals(UserData.GET_FILES_702)){//获取文件返回702 表示该站点 在其他地方登录 本程序将退出到登录界面
                Toast.makeText(getBaseContext(),"该站点在其他地方登录,本程序将退出到登录界面!",Toast.LENGTH_SHORT).show();
                Intent intent2LoginActivity = new Intent(MainActivity.this,LoginActivity.class);
                intent2LoginActivity.putExtra("needAutoLogin",false);//带个数据过去告诉LoginActivity不用自动登录
                startActivity(intent2LoginActivity);
                MainActivity.this.finish();

            }else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){//网络状态改变

                if(util.isNetWorkConnect()){//如果是网络已连接 开启后台更新服务
                    offlineTime = 0;//重置离线时间
                    stopCheckOfflineTimeout();//取消检测离线时间是否超过
                    Intent intentOfStarService = new Intent(MainActivity.this, BackgroundUpdateFilesService.class);
                    startService(intentOfStarService);
                    Toast.makeText(getBaseContext(),"网络已连接，开启后台更新服务！",Toast.LENGTH_SHORT).show();

                }else{//如果是网络没有连接 关闭后台更新服务  记录离线时间 开启检测离线超时timer
                    if(offlineTime == 0){//等于0才更新离线时间 记录最早的离线时间
                        offlineTime = System.currentTimeMillis();//记录离线时间
                        //开启timer检测离线时间是否超过
                        startCheckOfflineTimeout();
                    }
                    Intent intentOfStopService = new Intent(MainActivity.this, BackgroundUpdateFilesService.class);
                    stopService(intentOfStopService);
                }

            }else if(action.equals(UserData.OFFLINE_TIME_OUT)){//离线超时 退出到登录界面
                Intent intent2LoginActivity = new Intent(MainActivity.this,LoginActivity.class);
                intent2LoginActivity.putExtra("needAutoLogin",false);//带个数据过去告诉LoginActivity不用自动登录
                startActivity(intent2LoginActivity);
                MainActivity.this.finish();
                Toast.makeText(getBaseContext(),"离线时间已经用完，请重新登录！",Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void startCheckOfflineTimeout(){
        if(checkOfflineTimeoutTimer == null){
            checkOfflineTimeoutTimer = new Timer();
        }
        if(checkOfflineTimeoutTimeTask == null){
            checkOfflineTimeoutTimeTask = new TimerTask() {
                @Override
                public void run() {
                    if(offlineTime == 0){//如果等于0 就说明没有离线
                        this.cancel();
                        return;
                    }
                    final long currentTime = System.currentTimeMillis();
                    Log.i(TAG+"---",currentTime - offlineTime+"=====");
                    if(currentTime - offlineTime>=UserData.offlineTimeout){
                        Intent intent = new Intent(UserData.OFFLINE_TIME_OUT);
                        sendBroadcast(intent);
                        stopCheckOfflineTimeout();//停止检测离线超时
                    }
                }
            };
        }
        checkOfflineTimeoutTimer.schedule(checkOfflineTimeoutTimeTask,0,UserData.checkOfflineTimeoutTime);
    }

    private void stopCheckOfflineTimeout(){
        if(checkOfflineTimeoutTimer!=null)
            checkOfflineTimeoutTimer.cancel();
        if(checkOfflineTimeoutTimeTask!=null)
            checkOfflineTimeoutTimeTask.cancel();
    }

    private void updatePages() {
        pages = getUrls();//重新获取url
        banner.setPages(new CBViewHolderCreator<ImageViewHolder>() {//重新设置banner pages
            @Override
            public ImageViewHolder createHolder() {
                return new ImageViewHolder();
            }
        },pages);
        startBannerPlay();//开始轮播

        if(pages.size()>0){
            banner.getOnPageChangeListener().onPageSelected(0);//触发onPageSelected
        }else if(pages.size() == 0){
            showNoPageView();
            Toast.makeText(getBaseContext(),"671",Toast.LENGTH_SHORT).show();
            pageNumber_tv.setText("0/0");//更新页码指示器
        }

        if(pages.size() == 1){
            banner.setCanLoop(false);
        }else if(pages.size()>1){
            banner.setCanLoop(true);
        }

    }
    private class ImageViewHolder implements Holder<String> {

        private PhotoView view;

        @Override
        public View createView(Context context) {
            view = new PhotoView(context);
            view.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float x, float y) {
                    showOrHideAndDelayHideCtrl();
                }
                @Override
                public void onOutsidePhotoTap() {
                    showOrHideAndDelayHideCtrl();
                }
            });
//            view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));
//            view.setScaleType(ImageView.ScaleType.FIT_XY);
            return view;
        }

        @Override
        public void UpdateUI(Context context, int position, String url) {
            Glide.with(getApplicationContext()).load(url).apply(options).into(view);
        }
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

    private void showOrHideAndDelayHideCtrl(){
        isControlShowing = !isControlShowing;//改变控制栏的状态
        showOrHideControl(isControlShowing);//根据传入参数显示或者隐藏控制栏

        if(handler_showOrHideCtrl!=null)//如果延时操作handler不为空 先删除先前添加的延时操作
            handler_showOrHideCtrl.removeCallbacks(runnable_showOrHideCtrl);

        if(isControlShowing){//如果控制栏当前为显示状态
            handler_showOrHideCtrl.postDelayed(runnable_showOrHideCtrl,5000);//添加一个5秒之后隐藏控制栏的操作
        }
    }
}
