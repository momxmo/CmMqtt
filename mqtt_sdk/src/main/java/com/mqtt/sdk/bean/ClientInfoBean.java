package com.mqtt.sdk.bean;

public class ClientInfoBean {
    public String packgeName;
    public String appVersion;
    public String SysVersion;
    public String NetType;


    public String getPackgeName() {
        return packgeName;
    }

    public void setPackgeName(String packgeName) {
        this.packgeName = packgeName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getSysVersion() {
        return SysVersion;
    }

    public void setSysVersion(String sysVersion) {
        SysVersion = sysVersion;
    }


    public String getNetType() {
        return NetType;
    }

    public void setNetType(String netType) {
        NetType = netType;
    }
}
