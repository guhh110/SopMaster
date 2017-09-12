package entity;

/**
 * Created by sunpn on 2017/9/5.
 */

public class FilesEntity {
    private int id;
    private String wocUrl;
    private int wocID;
    private String wocName;
    private int wocOrder;
    private String wocVideoImg;
    private int wocDocID;

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

    public String getWocVideoImg() {
        return wocVideoImg;
    }

    public void setWocVideoImg(String wocVideoImg) {
        this.wocVideoImg = wocVideoImg;
    }

    public int getWocDocID() {
        return wocDocID;
    }

    public void setWocDocID(int wocDocID) {
        this.wocDocID = wocDocID;
    }

    @Override
    public String toString() {
        return "FilesEntity{" +
                "id=" + id +
                ", wocUrl='" + wocUrl + '\'' +
                ", wocID=" + wocID +
                ", wocName='" + wocName + '\'' +
                ", wocOrder=" + wocOrder +
                ", wocVideoImg='" + wocVideoImg + '\'' +
                ", wocDocID=" + wocDocID +
                '}';
    }

    public boolean isMp4(){
        return wocUrl.endsWith("mp4")||wocUrl.endsWith("MP4");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilesEntity that = (FilesEntity) o;

        if (id != that.id) return false;
        if (wocID != that.wocID) return false;
        if (wocOrder != that.wocOrder) return false;
        if (wocDocID != that.wocDocID) return false;
        if (wocUrl != null ? !wocUrl.equals(that.wocUrl) : that.wocUrl != null) return false;
        if (wocName != null ? !wocName.equals(that.wocName) : that.wocName != null) return false;
        return wocVideoImg != null ? wocVideoImg.equals(that.wocVideoImg) : that.wocVideoImg == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (wocUrl != null ? wocUrl.hashCode() : 0);
        result = 31 * result + wocID;
        result = 31 * result + (wocName != null ? wocName.hashCode() : 0);
        result = 31 * result + wocOrder;
        result = 31 * result + (wocVideoImg != null ? wocVideoImg.hashCode() : 0);
        result = 31 * result + wocDocID;
        return result;
    }
}
