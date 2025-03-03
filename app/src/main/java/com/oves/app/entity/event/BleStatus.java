package com.oves.app.entity.event;

import com.oves.app.enums.DeviceConnStatEnum;

public class BleStatus {

    private String macAddress;

    private DeviceConnStatEnum status;

    public BleStatus(String macAddress, DeviceConnStatEnum status) {
        this.macAddress = macAddress;
        this.status = status;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public DeviceConnStatEnum getStatus() {
        return status;
    }

    public void setStatus(DeviceConnStatEnum status) {
        this.status = status;
    }
}
