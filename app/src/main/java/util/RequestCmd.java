package util;

import java.util.Arrays;

/**
 * Created by orman on 2017/7/6.
 */

public class RequestCmd {
    public static String[] CMDS = "LOGIN,STATIONLIST,REQUESTWORK,REQUESTHELP,LACKGOODS,PROCOUNT,STOPLINE,BADSCOUNT,TROUBLESHOOTING".split(",");
    public static String[] RESULTS = "LOGINRESULT,REQUESTWORKRESULT,STATIONLISTRESULT,REQUESTHELPRESULT,LACKGOODSRESULT,PROCOUNTRESULT,STOPLINERESULT,BADSCOUNTRESULT,TROUBLESHOOTINGRESULT".split(",");
    public static boolean ExistCmd(String c)
    {
        return Arrays.asList(CMDS).contains(c) ;
    }
    public static boolean ExistResult(String c)
    {
        return Arrays.asList(CMDS).contains(c);
    }

    public enum Command
    {
        LOGIN, STATIONLIST, REQUESTWORK, REQUESTHELP, LACKGOODS, PROCOUNT, STOPLINE, BADSCOUNT, TROUBLESHOOTING, LOGINRESULT,
        STATIONLISTRESULT, REQUESTWORKRESULT, REQUESTHELPRESULT, LACKGOODSRESULT, PROCOUNTRESULT, STOPLINERESULT, BADSCOUNTRESULT, TROUBLESHOOTINGRESULT
    }
}
