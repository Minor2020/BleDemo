package com.zxfh.ble

import android.content.Context
import android.widget.Toast

object BLEReader {

    fun handshake(context: Context) {
        Toast.makeText(context, "From remote.", Toast.LENGTH_SHORT).show()
    }
}