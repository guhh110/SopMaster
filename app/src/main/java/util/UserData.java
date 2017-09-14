package util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import entity.FilesEntity;

/**
 * Created by sunpn on 2017/9/5.
 */

public class UserData {
    public static String ip;
    public static int port;
    public static String enCodeStation;//加密后的工站
    public static String station;
    public static int getFilesDelay = 3000;//获取文件的间隔时间  单位毫秒
    public static List<FilesEntity> filesEntities = new ArrayList<>();
    public static final String NEW_FILE_ACTION = "com.guhh.sopMaster";
    public static final String CONNECT_SERVER_SUCCESS_ACTION = "com.guhh.sopMaster.CSSA";
    public static final String PAGE_CHANGE_DELAY_CHANGED = "com.guhh.sopMaster.PCDC";


    public static void clear(){
        ip = "";
        port = -1;
        enCodeStation = "";
        station = "";
        filesEntities = new ArrayList<>();
    }
}
