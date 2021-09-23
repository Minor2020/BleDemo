package com.zxfh.demo

import android.app.Application
import com.ble.zxfh.sdk.blereader.BLEReader

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BLEReader.getInstance().setApplication(this)
    }
}