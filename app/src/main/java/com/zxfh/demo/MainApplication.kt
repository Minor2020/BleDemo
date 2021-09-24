package com.zxfh.demo

import android.app.Application
import com.ble.zxfh.sdk.blereader.BLEReader
import com.ble.zxfh.sdk.blereader.IBLEReader_Callback
import com.ble.zxfh.sdk.blereader.WDBluetoothDevice

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BLEReader.getInstance().setApplication(this)
    }
}