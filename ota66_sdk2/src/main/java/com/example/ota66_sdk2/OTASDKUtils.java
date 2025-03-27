package com.example.ota66_sdk2;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ota66_sdk2.beans.ErrorCode;
import com.example.ota66_sdk2.beans.OTAType;
import com.example.ota66_sdk2.ble.OTACallBack;
import com.example.ota66_sdk2.firware.UpdateFirewareCallBack;
import com.example.ota66_sdk2.util.BleUtils;


/**
 * OTASDKUtils
 *
 * @date:2018/7/13
 */

public class OTASDKUtils {

    private int STATUS = 0;
    private int START_OTA = 1;
    private int OTA_CONNECTING = 2;
    private int OTA_ING = 3;
    private int REBOOT = 4;
    private int START_RES = 5;
    private int RES_ING = 6;
    private int APP2OTA = 7;

    private OTACallBack otaCallBack;
    private OTAUtils otaUtils;
    private UpdateFirewareCallBack firewareCallBack;
    private String address;
    private String filePath;
    private OTAType otaType;
    //是否使用高速ota
    private boolean isQuick;

    /**
     * 创建OTASDKUtils方法
     * @param context
     * @param firewareCallBack
     */
    public OTASDKUtils(Context context, UpdateFirewareCallBack firewareCallBack) {
        this.firewareCallBack = firewareCallBack;

        otaCallBack = new OTACallBackImpl();
        otaUtils = new OTAUtils(context,otaCallBack);
    }

    public void setOtaKey(@Nullable String otaKey) {
        otaUtils.setOtaKey(otaKey);
    }

    public void setOtaKeyCmd0x74(boolean enable0x74) {
        otaUtils.setOtaKeyCmd0x74(enable0x74);
    }

    public void updateFirware(@NonNull String address, @NonNull String filePath){
        this.address = address;
        this.filePath = filePath;
        this.otaType = OTAType.OTA;

        initStatus();

        otaUtils.connectDevice(address);
    }

    public void updateSecurityFirware(@NonNull String address, @NonNull String filePath){
        this.address = address;
        this.filePath = filePath;
        this.otaType = OTAType.Security;

        initStatus();

        otaUtils.connectDevice(address);
    }

    public void updateResource(@NonNull String address, @NonNull String filePath){
        this.address = address;
        this.filePath = filePath;
        this.otaType = OTAType.RESOURCE;

        initStatus();

        otaUtils.connectDevice(address);
    }

    public void setRetryTimes(int times){
        if(otaUtils != null){
            otaUtils.setRetryTimes(times);
        }
    }

    public void cancleOTA(){
        otaUtils.cancleOTA();

        initStatus();
    }

    private void initStatus(){
        STATUS = 0;
    }

    private void error(int code){
        initStatus();
        firewareCallBack.onError(code);

        otaUtils.close();
    }

    private void success(){
        initStatus();
        firewareCallBack.onUpdateComplete();

        otaUtils.close();
    }

    private void startOta(){
        if(otaType == OTAType.OTA){
            otaUtils.updateFirmware(filePath,isQuick);
            STATUS = OTA_ING;
        }else if(otaType == OTAType.RESOURCE){
            otaUtils.updateResource(filePath,isQuick);
            STATUS = RES_ING;
        }else if(otaType == OTAType.Security){
            otaUtils.startSecurity();
            STATUS = RES_ING;
        }
    }

    private class OTACallBackImpl implements OTACallBack{
        @Override
        public void onConnected(boolean isConnected) {
            if(isConnected){
                isQuick = OTAUtils.MTU_SIZE > 23;
                Log.e("TAG", "onConnected: STATUS:"+STATUS+",otaType:"+otaType );
                if(STATUS == 0){
                    //建立连接之后走这里
                    if(otaType == OTAType.OTA){
                        STATUS = START_OTA;
                    }else if(otaType == OTAType.RESOURCE){
                        STATUS = START_RES;
                    }else if (otaType == OTAType.Security){
                        STATUS = RES_ING;
                    }
                }
                //设置MTU和enable之后会再一次回调走这里
                else if (otaType == OTAType.OTA && STATUS == START_OTA){
                    otaUtils.startOTA();
                }else if (otaType == OTAType.Security && STATUS == RES_ING){
                    otaUtils.startSecurity();
                }else if (otaType == OTAType.RESOURCE && STATUS == START_RES){
                    otaUtils.startResource();
                }
                //切换OTA模式断开重连走这里
                else if(STATUS == OTA_CONNECTING){
                    STATUS = APP2OTA;
                }else if(STATUS == APP2OTA){
                    Log.e("TAG", "onConnected: isQuick:"+isQuick );
//                    if(isQuick && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//                        Log.e("TAG", "onConnected: 1111111111111" );
//                        //设置高速率模式，设置成功将会触发onPhyUpdate
//                        otaUtils.setPHY();
//                    }else{
                        Log.e("TAG", "onConnected: 222222222222222" );
                        startOta();
//                    }
                }
                //错误的情况
                else{
                    Log.d("STATUS","error:"+STATUS);
                }
            }else{
                if(STATUS == START_OTA || STATUS == START_RES || STATUS == RES_ING ){
                    Log.e("TAG", "onConnected: 断开连接，重新扫描" );
                    //从APP模式切换到OTA模式的断开情况
                    otaUtils.starScan();
                }else if(STATUS == OTA_CONNECTING){
                    error(ErrorCode.OTA_CONNTEC_ERROR);
                }else if(STATUS == REBOOT){
                    success();
                }else if(STATUS == OTA_ING ){
                    error(ErrorCode.OTA_CONNTEC_ERROR);
                }else{
                    error(ErrorCode.CONNECT_ERROR);
                }
            }
        }

        @Override
        public void onOTA(boolean isConnected) {
            if(isConnected){
                if(isQuick && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if(!otaUtils.setPHY()) {
                        startOta();
                    }
                }else{
                    startOta();
                }
            }
        }

        @Override
        public void onDeviceSearch(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if((STATUS == START_OTA || STATUS == START_RES||STATUS == RES_ING) &&
                    device.getAddress().equals(BleUtils.getOTAMac(address))){
                otaUtils.stopScan();
                otaUtils.connectDevice(device.getAddress());

                STATUS = OTA_CONNECTING;
            }
        }

        @Override
        public void onProcess(float process) {
            firewareCallBack.onProcess(process);
        }

        @Override
        public void onError(int code) {
            Log.d("onError","error:"+code);
            error(code);
        }

        @Override
        public void onOTAFinish() {
            STATUS = REBOOT;
            otaUtils.reBoot();
        }

        @Override
        public void onResource(boolean isConnected) {
            if(isConnected){
                if(isQuick && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if(!otaUtils.setPHY()) {
                        startOta();
                    }
                }else{
                    startOta();
                }
            }
        }

        @Override
        public void onResourceFinish() {
            STATUS = REBOOT;
            otaUtils.reBoot();
        }

        @Override
        public void onReboot() {

        }

        @Override
        public void onRebootSuccess() {
            otaUtils.disConnectDevice();
        }

        @Override
        public void onPhyUpdate() {
            startOta();
        }

        @Override
        public void onStartSecurityData() {
            STATUS = OTA_ING;
            otaUtils.updateFirmware(filePath,isQuick,OTAType.Security);
        }
    }

}
