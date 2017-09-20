package util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import entity.FilesEntity;

/**
 * Created by guhh on 2017/9/5.
 */

public class UserData {
    public static String ip;
    public static int port;
    public static String enCodeStation;//加密后的工站
    public static String station;
    public static final int getFilesDelay = 6000;//获取文件的间隔时间  单位毫秒
    public static final int socketConnectTime = 1000;//socket连接超时  单位毫秒
    public static final int socketSoTime = 5000;//socket获取数据超时  单位毫秒
//    public static final long offlineTimeout = 48 * 60 * 60 * 1000; //离线时间超时 单位毫秒
//    public static final long checkOfflineTimeoutTime = 2 * 60 * 60 * 1000; //离线时间超时 单位毫秒
    public static final long offlineTimeout = 7 * 1000; //离线时间超时 单位毫秒
    public static final long checkOfflineTimeoutTime = 1000; //检查离线时间超时的时间 单位毫秒


    public static List<FilesEntity> filesEntities = new ArrayList<>();
    public static final String NEW_FILE_ACTION = "com.guhh.sopMaster";
    public static final String CONNECT_SERVER_SUCCESS_ACTION = "com.guhh.sopMaster.CSSA";
    public static final String PAGE_CHANGE_DELAY_CHANGED = "com.guhh.sopMaster.PCDC";
    public static final String GET_FILES_702 = "com.guhh.sopMaster.GF702";
    public static final String LOGIN_702 = "com.guhh.sopMaster.LOGIN702";
    public static final String OFFLINE_TIME_OUT = "com.guhh.sopMaster.OTO";

    public static void clear(){
        ip = "";
        port = -1;
        enCodeStation = "";
        station = "";
        filesEntities = new ArrayList<>();
    }
}
