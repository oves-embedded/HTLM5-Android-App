package com.example.oves_app.callback;

import com.example.oves_app.entity.CharacteristicDomain;
import com.example.oves_app.entity.ServicesPropertiesDomain;

import java.util.Map;

public interface InitBleDataCallBack {

    void onProgress(int total, int progress, CharacteristicDomain domain);

    void onComplete(Map<String, ServicesPropertiesDomain>data);

    void onFailure(String error);



}
