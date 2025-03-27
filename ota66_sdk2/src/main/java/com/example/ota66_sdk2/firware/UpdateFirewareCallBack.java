package com.example.ota66_sdk2.firware;

/**
 * UpdateFirewareCallBack
 *
 * @author:zhoululu
 * @date:2018/7/14
 */

public interface UpdateFirewareCallBack {

    //发生错误
    public void onError(int code);
    //OTA进度%
    public void onProcess(float process);
    //OTA完成
    public void onUpdateComplete();

}
