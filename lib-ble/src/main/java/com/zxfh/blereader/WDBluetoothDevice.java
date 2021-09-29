package com.zxfh.blereader;

import android.bluetooth.BluetoothDevice;

public class WDBluetoothDevice {
    public BluetoothDevice device;
    public int rssi;
    public byte[] scanRecord;

    public WDBluetoothDevice(BluetoothDevice var1, int var2, byte[] var3) {
        this.device = var1;
        this.setRssi(var2);
        this.setScanRecord(var3);
    }

    public void setRssi(int var1) {
        this.rssi = var1;
    }

    public void setScanRecord(byte[] var1) {
        if (var1 != null && var1.length > 0) {
            byte[] var10000 = var1;
            byte[] var2;
            this.scanRecord = var2 = new byte[var1.length];
            int var3 = var1.length;
            System.arraycopy(var10000, 0, var2, 0, var3);
        }

    }
}
