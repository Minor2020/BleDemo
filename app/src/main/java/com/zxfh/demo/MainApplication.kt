package com.zxfh.demo

import androidx.multidex.MultiDexApplication
import com.zxfh.blereader.BLEReader

class MainApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        BLEReader.getInstance().setApplication(this)
        BLEReader.getInstance().setLogEnabled(true)
    }
}