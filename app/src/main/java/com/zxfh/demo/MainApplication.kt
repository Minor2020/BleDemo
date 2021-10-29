package com.zxfh.demo

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.zxfh.blereader.BLEHandler

class MainApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        BLEHandler.getInstance().setApplication(this)
        BLEHandler.getInstance().setLogEnabled(true)
    }
}