package com.zxfh.demo;

import com.zxfh.blereader.BLEReader;

import androidx.multidex.MultiDexApplication;

public class MainApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册BLEReader
        BLEReader.getInstance().setApplication(this);
        // 开启BLEReader log. NOTE:实际发版需要移出
        BLEReader.getInstance().setLogEnabled(true);
    }
}
