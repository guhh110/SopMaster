package com.guhh.sopmaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.view.View;
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

import entity.FilesEntity;
import service.BackgroundUpdateFilesService;
import tcking.github.com.giraffeplayer.GiraffePlayer;
import tcking.github.com.giraffeplayer.GiraffePlayerActivity;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import util.UserData;
import util.Util;
import dialog.SettingDialog;

public class MainActivity extends AppCompatActivity {
    private Util util = new Util(getBaseContext());

    private static String TAG = "MainActivity";
    private boolean isBannerPlay = true;

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
//    private boolean isOnConfigurationChanged = false;//记录是不是旋转屏幕
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideBottomUIMenu();//全屏
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
                        startVideoPlay(video_url);//开始播放视频

                    }else{//当前item为图片
                        pause_ib.setVisibility(View.GONE);//隐藏暂停按钮
                        stopVideoPlay();//停止播放视频
                        showBannerHideVideoView();//显示banner
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
            banner.setCanLoop(true);
        }else if(pages.size() == 1){
            banner.setCanLoop(false);
        }else if(pages.size() == 0){
            showNoPageView();
        }
        banner.setScrollDuration(500);
        if(isBannerPlay)
            startBannerPlay();

        prev_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isBannerPlay){//重置切换页的冷却时间
                    startBannerPlay();
                }
                bannerToPrev();
            }
        });

        next_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isBannerPlay){//重置切换页的冷却时间
                    startBannerPlay();
                }
                bannerToNext();
            }
        });

        pause_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UserData.filesEntities ==null || UserData.filesEntities.size()-1<banner.getCurrentItem()){//防止下标越界
                    return;
                }
//
//                if(UserData.filesEntities.get(banner.getCurrentItem()).isMp4()) {//当前轮播item为视频
//                    Log.i(TAG,videoPlayer.isPlaying()+"--sss");
//                    if(videoPlayer.isPlaying()){
//                        pauseVideoPlay();
//                    }else{
//                        resumeVideoPlay();
//                    }
//                }else {//当前轮播item为图片
//                   if(isBannerPlay){
//                       isBannerPlay = false;
//                       stopBannerPlay();
//                   }else{
//                       isBannerPlay = true;
//                       startBannerPlay();
//                   }
//                }
//                updatePauseButtonStatus(false);//更新暂停按钮状态
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
                if(settingDialog == null){
                    settingDialog = SettingDialog.show(MainActivity.this,true);
                }else {
                    settingDialog.show();
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //开启后台更新文件服务
        Intent intent = new Intent(MainActivity.this, BackgroundUpdateFilesService.class);
        startService(intent);

        //注册广播
        if(myBroadCastReceiver == null){
            myBroadCastReceiver = new MyBroadCastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UserData.NEW_FILE_ACTION);
        registerReceiver(myBroadCastReceiver,intentFilter);

        //恢复视频播放
        resumeVideoPlay();

        Log.i(TAG,"onResume");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        recreate();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //停止后台更新文件服务
        Intent intent = new Intent(MainActivity.this, BackgroundUpdateFilesService.class);
        stopService(intent);

        //反注册广播
        unregisterReceiver(myBroadCastReceiver);

        //暂停视频
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
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG,"onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void onBackPressed() {
//        showOrHideControl();
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
                if(UserData.filesEntities!=null && UserData.filesEntities.get(banner.getCurrentItem()).isMp4()) {//当前轮播item为视频
                    isControlShowing = isShowing;
                    showOrHideControl(isShowing);//控制栏的显示和隐藏
                }
            }
        });
        videoPlayer.onComplete(new Runnable() {
            @Override
            public void run() {//播放完成，下一首
                if(UserData.filesEntities.size() == 1){//如果只有一个文件
                    String video_url = getVideoUrl(UserData.filesEntities.get(0).getWocUrl());
                    startVideoPlay(video_url);//开始播放视频
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
            banner.startTurning(UserData.changePageDelay * 1000);
        }
    }

    //停止播放视频
    private void stopVideoPlay(){
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
        if(videoPlayer!=null){
            videoPlayer.start();
        }
    }

    //开始播放视频
    private void startVideoPlay(String url){
        if(videoPlayer!=null){
            if(videoPlayer.isPlaying()){
                videoPlayer.stop();
            }
            videoPlayer.play(url);
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

    //接收BackgroundUpdateFilesService发过来的更新文件的广播
    class MyBroadCastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,intent.getAction());
            if(intent.getAction().equals(UserData.NEW_FILE_ACTION)){//有新文件
                //不管是否有在播放视频都停止播放视频 并 显示 banner  因为当视频在加载的时候更新文件的时候检查不到状态
                stopVideoPlay();
                showBannerHideVideoView();
                updatePages();//更新viewpager
            }
        }
    }

    private void updatePages() {
        pages = getUrls();//重新获取url
        banner.setPages(new CBViewHolderCreator<ImageViewHolder>() {//重新设置banner pages
            @Override
            public ImageViewHolder createHolder() {
                return new ImageViewHolder();
            }
        },pages);
        isBannerPlay = true;//重置变量
        startBannerPlay();//开始轮播

        if(pages.size()>0){
            banner.getOnPageChangeListener().onPageSelected(0);//触发onPageSelected
        }else if(pages.size() == 0){
            showNoPageView();
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
            Log.i(TAG,"createView");
            view = new PhotoView(context);
            view.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float x, float y) {
                    isControlShowing = !isControlShowing;
                    showOrHideControl(isControlShowing);
                    if(handler_showOrHideCtrl!=null)
                        handler_showOrHideCtrl.removeCallbacks(runnable_showOrHideCtrl);
                    handler_showOrHideCtrl.postDelayed(runnable_showOrHideCtrl,5000);
                }
                @Override
                public void onOutsidePhotoTap() {
                    isControlShowing = !isControlShowing;
                    showOrHideControl(isControlShowing);
                    if(handler_showOrHideCtrl!=null)
                        handler_showOrHideCtrl.removeCallbacks(runnable_showOrHideCtrl);
                    handler_showOrHideCtrl.postDelayed(runnable_showOrHideCtrl,5000);
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
        }
    }
}
