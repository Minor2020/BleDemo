package com.zxfh.demo

import android.app.Application
import com.zxfh.blereader.BLEHandler

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BLEHandler.getInstance().setApplication(this)
        BLEHandler.getInstance().setLogEnabled(true)
    }
}