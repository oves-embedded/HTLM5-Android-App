package com.oves.app.callback;

import com.oves.app.entity.CharacteristicDomain;
import com.oves.app.entity.ServicesPropertiesDomain;

public interface InitBleServiceDataCallBack {

    void onProgress(int total, int progress, CharacteristicDomain domain);

    void onComplete(ServicesPropertiesDomain domain);

    void onFailure(String error);


}
