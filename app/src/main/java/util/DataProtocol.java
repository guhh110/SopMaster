package util;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by orman on 2017/7/6.
 */

public class DataProtocol {
    /**
     * 通用请求指令打包
     *
     * @param cmd  指令码
     * @param parm 参数（请根据通讯协议标准进行相应字段的Base64编码）
     * @return
     */
    public static String makeCmd(RequestCmd.Command cmd, String... parm) {
        String[] cc = new String[parm.length + 3];
        //android中数组元素则为null，join后null不能trim掉
        for (int i = 0; i < cc.length; i++)
            cc[i] = "";
        cc[cc.length - 2] = "X2%TfO=c";
        System.arraycopy(parm, 0, cc, 1, parm.length);
        String signdata = " " + TextUtils.join(" ", cc).trim();
        ;
        cc[0] = cmd.toString();
        cc[cc.length - 1] = "<END>";
        //Log.i("NET", "待签名数据: "+signdata);
        cc[cc.length - 2] = getMD5_16(signdata).toUpperCase();  //16位MD5
        //Log.i("NET", "签名后数据: "+cc[cc.length - 2]);
        Log.i("NET", "Make: " + TextUtils.join(" ", cc));
        return TextUtils.join(" ", cc);
    }

    /**
     * 16位MD5加密
     *
     * @param val
     * @return
     */
    public static String getMD5_16(String val) {
        try {
            MessageDigest bmd5 = MessageDigest.getInstance("MD5");
            bmd5.update(val.getBytes());
            int i;
            StringBuffer buf = new StringBuffer();
            byte[] b = bmd5.digest();// 加密
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getBase64String(String str) {
        return Base64.encodeToString(str.getBytes(), Base64.NO_WRAP);
    }

    public static String fromBase64String(String str) {
        return new String(Base64.decode(str, Base64.NO_WRAP));
    }

    /**
     * 登录结果
     */
    public static class LoginResult {
        private String StationId;
        private String msg;

        public String getStationId() {
            return StationId;
        }

        public void setStationId(String stationId) {
            StationId = stationId;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public LoginResult() {
            //JSON转换的要求，必须要有一个构造函数
        }
    }

    /**
     * 获取的作业数据
     */
    public static class RequestWorkResult {
        private int id;
        private String wocUrl;
        private int wocID;
        private String wocName;
        private int wocOrder;
        private String wocVideoImg;

        public String getWocVideoImg() {
            return wocVideoImg;
        }

        public void setWocVideoImg(String wocVideoImg) {
            this.wocVideoImg = wocVideoImg;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getWocUrl() {
            return wocUrl;
        }

        public void setWocUrl(String wocUrl) {
            this.wocUrl = wocUrl;
        }

        public int getWocID() {
            return wocID;
        }

        public void setWocID(int wocID) {
            this.wocID = wocID;
        }

        public String getWocName() {
            return wocName;
        }

        public void setWocName(String wocName) {
            this.wocName = wocName;
        }

        public int getWocOrder() {
            return wocOrder;
        }

        public void setWocOrder(int wocOrder) {
            this.wocOrder = wocOrder;
        }

        public RequestWorkResult() {
        }
    }

    public static class ApkFileFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            if (file.isFile())
                return file.getName().toLowerCase().endsWith(".apk");
            else
                return false;
        }
    }
}