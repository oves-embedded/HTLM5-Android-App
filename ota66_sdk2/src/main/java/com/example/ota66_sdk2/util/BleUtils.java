package com.example.ota66_sdk2.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;


import com.example.ota66_sdk2.beans.BleConstant;
import com.example.ota66_sdk2.beans.FirmWareFile;
import com.example.ota66_sdk2.beans.Partition;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;


/**
 * BleUtils
 *
 * @author:zhoululu
 * @date:2018/7/7
 */

public class BleUtils {

    @SuppressLint("MissingPermission")
    public static boolean enableNotifications(BluetoothGatt bluetoothGatt){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_UUID));

        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_WRITE_UUID));

         boolean isEnableNotification = bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);
        if (isEnableNotification){
            BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BleConstant.DESCRIPTOR_UUID));
            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            return bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
        }else {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public static boolean enableIndicateNotifications(BluetoothGatt bluetoothGatt){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));

        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_INDICATE_UUID));

        boolean isEnableNotification = bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);
        if (isEnableNotification){
            BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BleConstant.DESCRIPTOR_UUID));
            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
        }else {
            return false;
        }


        return true;
    }

    @SuppressLint("MissingPermission")
    public static boolean checkIsOTA(BluetoothGatt bluetoothGatt){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));

        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_DATA_WRITE_UUID));
        if(bluetoothGattCharacteristic != null){
            return true;
        }else {
            return false;
        }
    }


    @SuppressLint("MissingPermission")
    public static boolean sendOTACommand(BluetoothGatt bluetoothGatt, String commd,boolean respons){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));
        if(bluetoothGattService == null){
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_WRITE_UUID));
        if(!respons){
            bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        }else{
            bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }
        bluetoothGattCharacteristic.setValue(HexString.parseHexString(commd));
        boolean isOK = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (isOK){
            Log.d("send ota commond", commd);
        }else {
            Log.e("send ota commond", "发送失败："+commd );
        }



        return true;

       // LogUtil.getLogUtilInstance().save("send ota commond: "+commd);
    }


    public static String getOTAMac(String deviceAddress){
        final String firstBytes = deviceAddress.substring(0, 15);
        // assuming that the device address is correct
        final String lastByte = deviceAddress.substring(15);
        final String lastByteIncremented = String.format("%02X", (Integer.valueOf(lastByte, 16) + 1) & 0xFF);

        return firstBytes + lastByteIncremented;
    }

    @SuppressLint("MissingPermission")
    public static boolean sendOTADate(BluetoothGatt bluetoothGatt,String cmd){
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_OTA_UUID));
        if(bluetoothGattService == null){
            Log.e(" OTA service", "service is null");
            return false;
        }

        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_OTA_DATA_WRITE_UUID));

        bluetoothGattCharacteristic.setValue(HexString.parseHexString(cmd.toLowerCase()));
        bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);

        Log.d("send ota data", cmd);

        return true;
    }

    public static String make_part_cmd(int index,long flash_addr,String run_addr,int size,int checksum){
        String fa = Util.translateStr(Util.strAdd0(Long.toHexString(flash_addr),8));
        String ra = Util.translateStr(Util.strAdd0(run_addr,8));
        String sz = Util.translateStr(Util.strAdd0(Integer.toHexString(size),8));
        String cs = Util.translateStr(Util.strAdd0(Integer.toHexString(checksum),4));
        String in = Util.strAdd0(Integer.toHexString(index),2);

        return "02"+ in +fa + ra + sz + cs;
    }

    public static String make_part_cmd(int index,long flash_addr,String run_addr,int size,String micCode){
        String fa = Util.translateStr(Util.strAdd0(Long.toHexString(flash_addr),8));
        String ra = Util.translateStr(Util.strAdd0(run_addr,8));
        String sz = Util.translateStr(Util.strAdd0(Integer.toHexString(size),8));
        String cs = Util.strAdd0(micCode,8);
        String in = Util.strAdd0(Integer.toHexString(index),2);

        return "02"+ in +fa + ra + sz + cs;
    }

    public static String make_resource_cmd(@NonNull FirmWareFile firmWareFile){

        String startAddress = firmWareFile.getList().get(0).getAddress();
        //&0x12000
        long flashLongAdd = Long.parseLong(startAddress,16) & 0xfffff000;
        long flashLongSize = Long.parseLong(startAddress,16) & 0xfff;
        for (Partition partition : firmWareFile.getList()){
            flashLongSize += partition.getPartitionLength();
        }
        flashLongSize = (flashLongSize + 0xfff) & 0xfffff000;

        String fa = Util.translateStr(Util.strAdd0(Long.toHexString(flashLongAdd),8));
        String sz = Util.translateStr(Util.strAdd0(Long.toHexString(flashLongSize),8));

        return "05" + fa + sz;
    }

    public static boolean sendPartition(BluetoothGatt gatt, FirmWareFile firmWareFile, int partitionIndex, long flash_addr){
        Partition partition = firmWareFile.getList().get(partitionIndex);
        int checsum = getPartitionCheckSum(partition);
        String cmd = make_part_cmd(partitionIndex, flash_addr, partition.getAddress(), partition.getPartitionLength(), checsum);
        if (firmWareFile.getPath().endsWith("hexe16")){
            List<List<String>> blocks = partition.getBlocks();
            List<String> lastBlock = blocks.get(blocks.size()-1);
            String lastData = lastBlock.get(lastBlock.size()-1);
            if (lastData.length() < 8) lastData = lastBlock.get(lastBlock.size()-2) + lastData;
            String micCode = lastData.substring(lastData.length()-8,lastData.length());
            cmd = make_part_cmd(partitionIndex, flash_addr, partition.getAddress(), partition.getPartitionLength(), micCode);
        }
        Log.e("TAG", "sendPartition: =================="+cmd );
        return sendOTACommand(gatt,cmd, true);
    }

    public static boolean sendResource(BluetoothGatt gatt, FirmWareFile firmWareFile){
        String cmd = make_resource_cmd(firmWareFile);

        return sendOTACommand(gatt,cmd, true);
    }

    public static int getPartitionCheckSum(Partition partition){
        return checkSum(0,HexString.parseHexString(partition.getData()));
    }

    private static int checkSum(int crc, byte[] data) {
        // 存储需要产生校验码的数据
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i];
        }
        int len = buf.length;

        for (int pos = 0; pos < len; pos++) {
            if (buf[pos] < 0) {
                // XOR byte into least sig. byte of
                crc ^= (int) buf[pos] + 256;
                // crc
            } else {
                // XOR byte into least sig. byte of crc
                crc ^= (int) buf[pos];
            }
            // Loop over each bit
            for (int i = 8; i != 0; i--) {
                // If the LSB is set
                if ((crc & 0x0001) != 0) {
                    // Shift right and XOR 0xA001
                    crc >>= 1;
                    crc ^= 0xA001;
                } else{
                    // Else LSB is not set
                    // Just shift right
                    crc >>= 1;
                }
            }
        }

        return crc;
    }

    public static String getRandomStr(){
        StringBuffer buffer = new StringBuffer();
        int length = 32;
        char[] allChar = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        SecureRandom random = new SecureRandom();
        for(int i=0; i< length; i++){
            buffer.append(allChar[random.nextInt(allChar.length)]);
        }
        return String.valueOf(buffer);
    }

}
