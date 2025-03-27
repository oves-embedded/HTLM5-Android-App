package com.example.ota66_sdk2;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ota66_sdk2.beans.ErrorCode;
import com.example.ota66_sdk2.beans.FirmWareFile;
import com.example.ota66_sdk2.beans.OTAType;
import com.example.ota66_sdk2.ble.BleCallBack;
import com.example.ota66_sdk2.ble.BleScanner;
import com.example.ota66_sdk2.ble.OTACallBack;
import com.example.ota66_sdk2.ble.OTAUtilsCallBack;
import com.example.ota66_sdk2.util.BleUtils;
import com.example.ota66_sdk2.util.HexString;


/**
 * BleUtils
 *
 * @date:2018/7/13
 */

/**
 * 升级或者升级失败都要调用{@link #cancleOTA()}。且不支持重复使用，即如升级完后，需要升级其他的设备，
 * 请重新创建新的{@link OTAUtils}对象。
 * Created on 2020/12/24.
 *
 * @author WAMsAI (wamsai1096@qq.com)
 */
public class OTAUtils {
    public static int MTU_SIZE = 23;

    private Context mContext;
    private BleScanner mBleScanner;
    private BluetoothGatt mBluetoothGatt;
    private BleCallBack mBleCallBack;
    private boolean isConnected;
    private boolean isQuick;

    private String otaKey = "";
    private boolean otaKeyCmd0x74 = true;

    private OTACallBack callBack;

    /**
     * 创建OTAUtils实例
     * @param context
     * @param callBack
     */
    public OTAUtils(Context context, OTACallBack callBack) {
        this.mContext = context;
        this.callBack = callBack;

        init();
    }

    private void init(){
        OTAUtilsCallBack otaUtilsCallBack = new OTAUtilsCallBackImpl();
        mBleScanner = new BleScanner(mContext,otaUtilsCallBack);

        mBleCallBack = new BleCallBack();
        mBleCallBack.setOtaUtilsCallBack(otaUtilsCallBack);

        SharedPreferences sp = mContext.getSharedPreferences("data",MODE_PRIVATE);
        String keyValue = sp.getString("AESKey","123");
        mBleCallBack.password = keyValue;
    }

    public void setOtaKey(@Nullable String otaKey) {
        this.otaKey = otaKey;
    }

    public void setOtaKeyCmd0x74(boolean enable0x74) {
        this.otaKeyCmd0x74 = enable0x74;
    }
    @SuppressLint("MissingPermission")
    public void connectDevice(@NonNull String address){

        if(isConnected){
            Log.e("TAG", "connectDevice: 已经连上" );
            callBack.onConnected(isConnected);
            return;
        }

        BluetoothManager mBluetoothManager = (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothDevice device = mBluetoothManager.getAdapter().getRemoteDevice(address);

        mBluetoothGatt = device.connectGatt(mContext.getApplicationContext(),false,mBleCallBack);
    }

    public void starScan(){
        mBleScanner.scanDevice();
    }
    @SuppressLint("MissingPermission")
    public void disConnectDevice(){
        if(isConnected && mBluetoothGatt != null){
            mBluetoothGatt.disconnect();
        }
    }
    @SuppressLint("MissingPermission")
    public void close(){
        if(mBluetoothGatt != null){
            mBluetoothGatt.close();
        }
    }

    public void stopScan(){
        mBleScanner.stopScanDevice();
    }

    public void startOTA(){
        if (isConnected) {
            sendOtaKey();
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isConnected){
                    if(BleUtils.checkIsOTA(mBluetoothGatt)){
                        callBack.onOTA(true);
                    }else{

                        String command = "0102";
                        boolean isResponse = false;
                        if(isQuick){
                            command = "010201";
                            isResponse = true;
                        }
                        final boolean success = sendCommand(mBluetoothGatt, command, isResponse);

                        if(success){
                            callBack.onOTA(false);
                        }
                    }
                }else{
                    callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
                }
            }
        }, 800);
    }

    public void startResource(){
        if (isConnected) {
            sendOtaKey();
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    if (BleUtils.checkIsOTA(mBluetoothGatt)) {
                        callBack.onResource(true);
                    } else {
                        String command = "0103";
                        boolean isResponse = false;
                        if (isQuick) {
                            command = "010301";
                            isResponse = true;
                        }
                        boolean success = sendCommand(mBluetoothGatt, command, isResponse);
                        if (success) {
                            callBack.onResource(false);
                        }
                    }
                } else {
                    callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
                }
            }
        }, 800);
    }

    public void reBoot(){
        if(isConnected){
            if(BleUtils.checkIsOTA(mBluetoothGatt)){
                String command = "04";
                boolean isResponse = false;
                if(isQuick){
                    command = "0401";
                    isResponse = true;
                }
                boolean success = sendCommand(mBluetoothGatt,command,isResponse);
                if(success){
                    callBack.onReboot();
                }
            }else{
                callBack.onError(ErrorCode.DEVICE_NOT_IN_OTA);
            }
        }else{
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
        }
    }
    @SuppressLint("MissingPermission")
    public void updateFirmware(@NonNull String filePath, boolean isQuick, OTAType otaType){
        this.isQuick = isQuick;

        //检查设备是否已连接
        if(isConnected){
            //检查设备是否已经在OTA状态
            if(BleUtils.checkIsOTA(mBluetoothGatt)){

                FirmWareFile firmWareFile = new FirmWareFile(filePath,isQuick);
                if(firmWareFile.getCode() != 200){
                    callBack.onError(ErrorCode.FILE_ERROR);
                    return;
                }

                mBleCallBack.setFirmWareFile(firmWareFile,otaType);

                String command = "01"+ HexString.int2ByteString(firmWareFile.getList().size());
//                if(isQuick){
//                    command = command + "ff";
//                }else{
                    command = command + "00";
//                }
                sendCommand(mBluetoothGatt,command,true);

            }else{
                callBack.onError(ErrorCode.DEVICE_NOT_IN_OTA);
            }
        }else{
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
        }
    }

    public void updateFirmware(@NonNull String filePath,boolean isQuick){
        updateFirmware(filePath,isQuick,OTAType.OTA);
    }

    public void updateResource(@NonNull String filePath,boolean isQuick){
        this.isQuick = isQuick;

        //检查设备是否已连接
        if(isConnected){
            //检查设备是否已经在Resource状态
            if(BleUtils.checkIsOTA(mBluetoothGatt)){

                FirmWareFile firmWareFile = new FirmWareFile(filePath,isQuick);
                if(firmWareFile.getCode() != 200){
                    callBack.onError(ErrorCode.FILE_ERROR);
                    return;
                }

                mBleCallBack.setFirmWareFile(firmWareFile,OTAType.RESOURCE);

                String command = "01"+ HexString.int2ByteString(firmWareFile.getList().size());
//                if(isQuick){
//                    command = command + "ff";
//                }else{
                    command = command + "00";
//                }

                sendCommand(mBluetoothGatt,command,true);

            }else{
                callBack.onError(ErrorCode.DEVICE_NOT_IN_OTA);
            }
        }else{
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
        }
    }

    /**
     * 取消OTA升级后，会和设备自动断开连接
     */
    public void cancleOTA(){
        if(checkOTA()){
            mBleCallBack.setCancle();
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
            }
        } else {
            // XXX: 是否需要以下的处理吗？
            //      如果设备不支持OTA那么会一直保持连接，导致无法搜索到设备，所有下面进行了断开连接处理
            mBleCallBack.setCancle();
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
            }
        }
    }

    public boolean checkOTA(){
        if(isConnected){
            return BleUtils.checkIsOTA(mBluetoothGatt);
        }else {
            return false;
        }
    }

    /**
     * 设置OTA数据发送失败重试次数
     * @param times
     */
    public void setRetryTimes(int times){
        mBleCallBack.setRetryTimes(times);
    }

    /**
     * Android 8.0（包含）以上执行。
     *
     * Set the preferred connection PHY for this app.
     *
     * @see BluetoothGatt#setPreferredPhy
     */
    public boolean setPHY(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if (BluetoothAdapter.getDefaultAdapter().isLe2MPhySupported()) {
                if(mBluetoothGatt != null){
                    mBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M,BluetoothDevice.PHY_LE_2M,BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                }
            } else {
                return false;
            }
        }else{
            return false;
        }

        return true;
    }

    private void sendOtaKey() {
        String otaKey = this.otaKey;
        if (otaKey != null && !otaKey.isEmpty()) {
            byte [] bytes = otaKey.getBytes();
            String head = "05";
            if (this.otaKeyCmd0x74) {
                head = "74";
            }
            String command = head + bytes2HexString(bytes);
            Log.d("OTAKEY", "sendOtaKey: " + command);
            sendCommand(mBluetoothGatt, command, false);
        }
    }

    private boolean sendCommand(BluetoothGatt bluetoothGatt, String commd,boolean respons){
        boolean success = BleUtils.sendOTACommand(bluetoothGatt,commd,respons);
        if(!success){
            callBack.onError(ErrorCode.OTA_SERVICE_NOT_FOUND);
        }

        return success;
    }

    public void startSecurity() {
        mBleCallBack.startSecurity(mBluetoothGatt);
    }

    private class OTAUtilsCallBackImpl implements OTAUtilsCallBack{

        @Override
        public void onDeviceSearch(BluetoothDevice device, int rssi, byte[] scanRecord) {
            callBack.onDeviceSearch(device,rssi,scanRecord);
        }

        @Override
        public void onConnectChange(boolean connect) {
            isConnected = connect;

            callBack.onConnected(connect);
        }

        @Override
        public void onProcess(float process) {
            callBack.onProcess(process);
        }

        @Override
        public void onError(int code) {
            Log.e("TAG", "onError: ========="+code );
            callBack.onError(code);
        }

        @Override
        public void onOTAFinish() {

            callBack.onOTAFinish();
        }

        @Override
        public void onResourceFinish() {
            callBack.onResourceFinish();
        }

        @Override
        public void onReBootSuccess() {
            callBack.onRebootSuccess();
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            callBack.onPhyUpdate();
        }

        @Override
        public void onStartSecurityData() {
            callBack.onStartSecurityData();
        }
    }

    public static String bytes2HexString(final byte[] bytes) {
        return bytes2HexString(bytes, true);
    }

    public static String bytes2HexString(final byte[] bytes, boolean isUpperCase) {
        if (bytes == null) return "";
        char[] hexDigits = isUpperCase ? HEX_DIGITS_UPPER : HEX_DIGITS_LOWER;
        int len = bytes.length;
        if (len <= 0) return "";
        char[] ret = new char[len << 1];
        for (int i = 0, j = 0; i < len; i++) {
            ret[j++] = hexDigits[bytes[i] >> 4 & 0x0f];
            ret[j++] = hexDigits[bytes[i] & 0x0f];
        }
        return new String(ret);
    }

    private static final char[] HEX_DIGITS_UPPER =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] HEX_DIGITS_LOWER =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

}
