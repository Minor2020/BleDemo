package com.zxfh.demo;

import static com.zxfh.demo.MainActivityKt.REQUEST_ENABLE_BT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.zxfh.blereader.BLEReader;
import com.zxfh.blereader.IBLEReader_Callback;
import com.zxfh.blereader.PosMemoryConstants;

import java.util.Iterator;
import java.util.Set;

import android.util.Log;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SdkApiSample {

    private MainActivity activity;
    /** "12 00 05 00 B0 00 00 00 0A" */
    private byte[] READ_ZONE_CMD = new byte[] {18, 0, 5, 0, (byte) 176, 0, 0, 0, 10};
    /** hex str "12 00 05 00 12 00 00 00 05" to bytes */
    private byte[] MOCK_SEND_DATA = new byte[] {18, 0, 5, 0, 18, 0, 0, 0, 5};
    /** onCharacteristicChanged 回传数据 */
    private byte[] dynamicBytes;
    private int cardType = 0;

    /** BLE SDK 回调 */
    IBLEReader_Callback bleCallback = new IBLEReader_Callback() {
        /**
         * @see 4.6.2 public void onConnectGatt(int status, Object data)
         */
        public void onConnectGatt(int p0, @Nullable Object p1) {
            String data = (String) p1;
            sprintInfo("onConnectGatt status " + p0 + " data " + data);
            if (p0 == 0) {
            }
        }

        /**
         * @see 4.6.3  onServicesDiscovered(int status, Object data);
         */
        public void onServicesDiscovered(int p0, @Nullable Object p1) {
            boolean data = (Boolean) p1;
            sprintInfo("onServicesDiscovered status " + p0 + " data " + data);
            if (data) {
                sprintInfo("蓝牙服务已开启");
            }
        }

        /**
         * @see 4.6.4 onCharacteristicChanged(int status, Object data)
         */
        public void onCharacteristicChanged(int p0, @Nullable Object p1) {
            byte[] data = (byte[]) p1;
            dynamicBytes = data;
            String hexStr = BytesUtils.Companion.getHexStr(dynamicBytes);
            sprintInfo("onCharacteristicChanged status " + p0 + " data " + hexStr);
            Log.d("MBLE", "data " + hexStr);
        }

        public void onReadRemoteRssi(int p0) {
            sprintInfo("onReadRemoteRssi " + p0);
        }

        public void onOTA(int p0, @Nullable Object p1) {
            sprintInfo("onOTA status " + p0);
        }

        public int onChangeBLEParameter() {
            return 0;
        }
    };


    public SdkApiSample(MainActivity activity) {
        BLEReader.getInstance().setListener(bleCallback);
        this.activity = activity;
    }

    private void printUuid() {
        sprintInfo(BLEReader.getInstance().getUuid());
    }

    private void sprintInfo(String msg) {
        activity.sprintInfo(msg);
    }

    /**
     * 释放蓝牙
     */
    public final void releaseGatt() {
        BLEReader.getInstance().disconnectGatt();
    }

    /**
     * 扫描设备
     */
    private final void scanDevices() {
        sprintInfo("蓝牙扫描开始...");
        BLEReader.getInstance().getBluetoothAdapter().startDiscovery();
    }

    /**
     * 蓝牙配对
     */
    public final int pairBle(@Nullable String macAddress) {
        BLEReader.getInstance().getBluetoothAdapter().cancelDiscovery();
        return BLEReader.getInstance().connectGatt(macAddress);

    }

    /**
     * 查询已配对的蓝牙设备
     */
    public final void pairedDevices() {
//        sprintInfo("已连接设备")
//        val pairedDevices: Set<BluetoothDevice>? = BLEReader.getInstance().bluetoothAdapter.bondedDevices
//        val deviceInfo = StringBuilder()
//        pairedDevices?.forEach { device ->
//                val deviceName = device.name
//            val deviceHardwareAddress = device.address // MAC address
//
//            sprintInfo("$deviceName : $deviceHardwareAddress")
//        }
    }


    /**
     * 修改密码
     */
    public final void modifyPassword() {
        int res = BLEReader.getInstance().MC_UpdatePIN_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SC,
                new byte[] {(byte) 240, (byte) 240});
        sprintInfo("修改密码返回 " + res);
    }

    /**
     * 连接设备
     */
    public final void connect() {
        // 检查蓝牙是否启用
        if (BLEReader.getInstance().isBLEnabled()) {
            scanDevices();
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * 模块复位
     */
    public final void reset() {
        int res = BLEReader.getInstance().ICC_Reset(new byte[16], new int[2]);
        sprintInfo("卡片复位返回 " + res);
    }

    /**
     * 写卡数据
     */
    public final void writeData() {
        int res = -1;
        if (cardType == BLEReader.CARD_TYPE_AT88SC102) {
            res = BLEReader.getInstance().MC_Write_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_MTZ, 0, new byte[] {18, 52}, 0, 2);
        } else if (cardType == BLEReader.CARD_TYPE_AT24C02) {
            res = BLEReader.getInstance().MC_Write_AT24C02(0, new byte[] {0, (byte)0xB0, 0, 0, (byte)0xC3},
                    0, 5);
        }
        sprintInfo("写数据返回值 " + res);
    }

    /**
     * 核对密码
     */
    public final void checkPassword() {
        int res = BLEReader.getInstance().MC_VerifyPIN_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SC,
                new byte[] {(byte) 0xF0, (byte) 0xF0}, new int[2]);
        sprintInfo("验证密码返回 " + res);
    }

    /**
     * 读卡数据
     */
    public final void readData() {
        int result = -1;
        if (cardType == BLEReader.CARD_TYPE_AT88SC102) {
            result = BLEReader.getInstance().MC_Read_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_MTZ, 0, 2,
                    new byte[100]);
        } else if (cardType == BLEReader.CARD_TYPE_AT24C02) {
            result = BLEReader.getInstance().MC_Read_AT24C02(0, 176, new byte[180]); // 21(0x15)
        }
        sprintInfo("读卡数据返回 " + result);
    }

    /**
     * 检测卡类型
     */
    public final void checkMode() {
        int res = BLEReader.getInstance().ICC_GetCardType(false);
        cardType = res;
        if (res == BLEReader.CARD_TYPE_AT88SC102) {
            sprintInfo("卡类型 CARD_TYPE_AT88SC102 值 " + res);
        } else if (res == BLEReader.CARD_TYPE_AT24C02) {
            sprintInfo("卡类型 CARD_TYPE_AT24C02 值 " + res);
        } else {
            sprintInfo("卡类型 " + res);
        }
    }

    /**
     * 错误次数判断
     */
    public final void errorCount() {
        int res = BLEReader.getInstance().MC_PAC_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SCAC);
        sprintInfo("错误次数返回 " + res);
    }

    /**
     * 对卡上电
     */
    public final void upload() {
        int res = BLEReader.getInstance().sendData(this.MOCK_SEND_DATA);
        sprintInfo("上电返回 " + res);
    }

    /**
     * 断开连接
     */
    public final void disconnect() {
        int result = BLEReader.getInstance().disconnectGatt();
        sprintInfo("断开连接 " + result);
    }

    /**
     * 测试SM4
     */
    public final void testSM4() {
        sprintInfo("BLE(" + BLEReader.getInstance().testSm4() + ')');
    }

}

