package com.oves.app.application;

import android.app.Application;

import com.oves.app.thread.ThreadPool;
import com.hjq.toast.Toaster;
import com.hjq.toast.style.WhiteToastStyle;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.oves.app.util.FileServer;

import java.util.Locale;

public class MyApplication extends Application {

    private static MyApplication INSTANCE = new MyApplication();

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化吐司工具类
        Toaster.init(this, new WhiteToastStyle());
        Logger.addLogAdapter(new AndroidLogAdapter());

        Locale.setDefault(Locale.getDefault());

        FileServer.startFileServer(8090, this);
    }
    public static MyApplication getInstance() {
        return INSTANCE;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ThreadPool.shutDown();
        FileServer.stopFileServer();
    }


}
