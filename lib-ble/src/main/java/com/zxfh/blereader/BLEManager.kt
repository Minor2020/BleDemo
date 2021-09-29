package com.zxfh.blereader

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent

final class BLEManager() {

    /** 上下文 */
    private var context: Context? = null
    /** 蓝牙适配器 */
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    /** 请求打开蓝牙 */
    private val REQUEST_ENABLE_BT = 1

    private constructor(context: Context) : this() {
        this.context = context
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: BLEManager? = null

        fun getInstance(context: Context): BLEManager {
            val res = instance
            if (res != null) {
                return res
            }
            return synchronized(this) {
                val res = instance
                if (res != null) {
                    res
                } else {
                    val created = BLEManager(context)
                    instance = created
                    created
                }
            }
        }
    }

    fun isSupportBluetooth(): Boolean {
        return bluetoothAdapter != null
    }

    fun openBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            (context as Activity).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }
}