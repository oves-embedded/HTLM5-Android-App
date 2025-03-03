package com.oves.app.callback;

import com.oves.app.entity.CharacteristicDomain;
import com.oves.app.entity.ServicesPropertiesDomain;

import java.util.Map;

public interface InitBleDataCallBack {

    void onProgress(int total, int progress, CharacteristicDomain domain);

    void onComplete(Map<String, ServicesPropertiesDomain>data);

    void onFailure(String error);



}
