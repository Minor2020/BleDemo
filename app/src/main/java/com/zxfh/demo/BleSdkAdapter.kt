package com.zxfh.demo

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Intent
import com.zxfh.blereader.BLEReader
import com.zxfh.blereader.IBLEReader_Callback
import com.zxfh.blereader.PosMemoryConstants

/**
 * 该类用做适配 BLE SDK Api 调用
 */
@Deprecated("Warning", ReplaceWith("SdkApiSample"))
class BleSdkAdapter(private val activity: MainActivity) {
    /** "12 00 05 00 B0 00 00 00 0A" */
    private val READ_ZONE_CMD = byteArrayOf(0x12, 0x00, 0x05, 0x00, 0xB0.toByte(), 0x00, 0x00, 0x00, 0x0A)
    /** hex str "12 00 05 00 12 00 00 00 05" to bytes */
    private val MOCK_SEND_DATA = byteArrayOf(0x12, 0x00, 0x05, 0x00, 0x12, 0x00, 0x00, 0x00, 0x05)
    /** onCharacteristicChanged 回传数据 */
    private var dynamicBytes: ByteArray? = null

    /** BLE SDK 回调 */
    private val bleCallback = object : IBLEReader_Callback {

        /**
         * @see 4.6.2 public void onConnectGatt(int status, Object data)
         */
        override fun onConnectGatt(p0: Int, p1: Any?) {
            val data = p1 as String
            sprintInfo("onConnectGatt status $p0 data $data")
            // 连接成功，NOTE！！ 与 Android 规范有出入 status == 0 && newStatus == 2 才表成功
            // SDK 内部联立判断有 bug，故在此只是借用 GATT_SUCCESS 字面值，也有可能服务没有链接成功
            if (p0 == BluetoothGatt.GATT_SUCCESS ) {
            }
        }

        /**
         * @see 4.6.3  onServicesDiscovered(int status, Object data);
         */
        override fun onServicesDiscovered(p0: Int, p1: Any?) {
            val data = p1 as Boolean
            sprintInfo("onServicesDiscovered status $p0 data $data")
            if (data) {
                sprintInfo("蓝牙服务已开启")
            }
        }

        /**
         * @see 4.6.4 onCharacteristicChanged(int status, Object data)
         */
        override fun onCharacteristicChanged(p0: Int, p1: Any?) {
            val data = p1 as ByteArray
            dynamicBytes = data
            val hexStr = BytesUtils.getHexStr(dynamicBytes)
            sprintInfo("onCharacteristicChanged status $p0 data $hexStr")
        }

        override fun onReadRemoteRssi(p0: Int) {
            sprintInfo("onReadRemoteRssi $p0")
        }

        override fun onOTA(p0: Int, p1: Any?) {
            sprintInfo("onOTA status $p0")
        }

        override fun onChangeBLEParameter(): Int {
            return 0
        }
    }

    init {
        BLEReader.getInstance().setListener(bleCallback)
    }


    private fun printUuid() {
        sprintInfo(BLEReader.getInstance().uuid)
    }

    private fun sprintInfo(msg: String) {
        activity.sprintInfo(msg)
    }

    /**
     * 释放蓝牙
     */
    fun releaseGatt() {
        BLEReader.getInstance().disconnectGatt()
    }

    /**
     * 扫描设备
     */
    private fun scanDevices() {
        sprintInfo("蓝牙扫描开始...")
        BLEReader.getInstance().bluetoothAdapter.startDiscovery()
    }

    /**
     * 蓝牙配对
     */
    fun pairBle(macAddress: String?): Int {
        BLEReader.getInstance().bluetoothAdapter.cancelDiscovery()
        return BLEReader.getInstance().connectGatt(macAddress)
    }

    /**
     * 查询已配对的蓝牙设备
     */
    fun pairedDevices() {
        sprintInfo("已连接设备")
        val pairedDevices: Set<BluetoothDevice>? = BLEReader.getInstance().bluetoothAdapter.bondedDevices
        val deviceInfo = StringBuilder()
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address

            sprintInfo("$deviceName : $deviceHardwareAddress")
        }
    }

    /**
     * 修改密码
     */
    fun modifyPassword() {
        val res = BLEReader.getInstance().MC_UpdatePIN_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SC,
            byteArrayOf(0xF0.toByte(), 0xF0.toByte()))
        sprintInfo("修改密码返回 $res")
    }

    /**
     * 连接设备
     */
    fun connect() {
        // 检查蓝牙是否启用
        if (BLEReader.getInstance().isBLEnabled) {
            scanDevices()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    /**
     * 模块复位
     */
    fun reset() {
        val res = BLEReader.getInstance().ICC_Reset(ByteArray(16), IntArray(2))
        // 返回卡片类型
        sprintInfo("卡片复位返回 $res")
    }

    /**
     * 写卡数据
     */
    fun writeData() {
        val res = BLEReader.getInstance().MC_Write_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_MTZ, 0,
            byteArrayOf(0x12, 0x34), 0, 2)
        sprintInfo("写数据返回值 $res")
    }

    /**
     * 核对密码
     */
    fun checkPassword() {
        val res = BLEReader.getInstance().MC_VerifyPIN_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SC,
            byteArrayOf(0x48, 0x40), IntArray(2));
        sprintInfo("验证密码返回 $res")
    }

    /**
     * 读卡数据
     */
    fun readData() {
        val result = BLEReader.getInstance().MC_Read_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_MTZ, 0, 2,
            ByteArray(100))
        sprintInfo("读卡数据返回 $result")

    }

    /**
     * 检测卡类型
     */
    fun checkMode() {
        val res = BLEReader.getInstance().ICC_GetCardType(false)
        if (res == BLEReader.CARD_TYPE_AT88SC102) {
            sprintInfo("卡类型 CARD_TYPE_AT88SC102 值 $res")
        } else {
            sprintInfo("卡类型 $res")
        }
    }

    /**
     * 错误次数判断
     */
    fun errorCount() {
        val res = BLEReader.getInstance().MC_PAC_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SCAC)
        sprintInfo("错误次数返回 $res")
    }

    /**
     * 对卡上电
     */
    fun upload() {
        val res = BLEReader.getInstance().sendData(MOCK_SEND_DATA)
        sprintInfo("上电返回 $res")
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        val result = BLEReader.getInstance().disconnectGatt()
        sprintInfo("断开连接 $result")
    }

    fun testSM4() {
        sprintInfo("BLE(${BLEReader.getInstance().testSm4()})")
    }
}