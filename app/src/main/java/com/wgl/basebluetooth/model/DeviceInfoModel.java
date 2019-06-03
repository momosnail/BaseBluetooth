package com.wgl.basebluetooth.model;

public class DeviceInfoModel {
    private String deviceName;
    private boolean isBond;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean isBond() {
        return isBond;
    }

    public void setBond(boolean bond) {
        isBond = bond;
    }
}
