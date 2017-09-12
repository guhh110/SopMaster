package util;

import java.io.File;
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
    public static int getFilesDelay = 1000;//获取文件的间隔时间  单位毫秒
    public static List<FilesEntity> filesEntities;
    public static int changePageDelay = 3;
    public static String NEW_FILE_ACTION = "com.guhh.sopMaster";

    public static void clear(){
        ip = "";
        port = -1;
        enCodeStation = "";
        station = "";
        filesEntities = null;
    }
}
