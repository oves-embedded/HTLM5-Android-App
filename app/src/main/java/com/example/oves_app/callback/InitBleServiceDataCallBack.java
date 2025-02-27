package com.example.oves_app.callback;

import com.example.oves_app.entity.CharacteristicDomain;
import com.example.oves_app.entity.ServicesPropertiesDomain;

public interface InitBleServiceDataCallBack {

    void onProgress(int total, int progress, CharacteristicDomain domain);

    void onComplete(ServicesPropertiesDomain domain);

    void onFailure(String error);


}
