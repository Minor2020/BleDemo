package com.zxfh.demo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.ListView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ble.zxfh.sdk.blereader.BLEReader
import com.ble.zxfh.sdk.blereader.IBLEReader_Callback
import com.ble.zxfh.sdk.blereader.WDBluetoothDevice


/**
 *
 * demo 主界面
 * @author zxfh
 */
class MainActivity : AppCompatActivity() {
    /** "12 00 05 00 B0 00 00 00 0A" */
    private val READ_ZONE_CMD = byteArrayOf(0x12, 0x00, 0x05, 0x00, 0xB0.toByte(), 0x00, 0x00, 0x00, 0x0A)
    /** hex str "12 00 05 00 12 00 00 00 05" to bytes */
    private val MOCK_SEND_DATA = byteArrayOf(0x12, 0x00, 0x05, 0x00, 0x12, 0x00, 0x00, 0x00, 0x05)
    /** 请求启动蓝牙返回码 */
    private val REQUEST_ENABLE_BT = 1
    /** 按钮面板容器 */
    private var cardsView: GridView? = null
    /** 信息显示view */
    private var infoListView: ListView? = null
    /** 信息listAdapter */
    private var infoAdapter: ArrayAdapter<String>? = null
    /** 只为了 mac 去重显示 */
    private val macMap: HashMap<String?, String?> = HashMap()
    /** 请求权限回调 */
    private lateinit var requestPermissionLaunch: ActivityResultLauncher<String>
    /** onCharacteristicChanged 回传数据 */
    private var dynamicBytes: ByteArray? = null
    /** BLE SDK 回调 */
    private val bleCallback = object : IBLEReader_Callback {
        override fun onLeScan(p0: MutableList<WDBluetoothDevice>?) {
            sprintInfo("onnLeScan")
        }

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
                sprintInfo("发送 mock 数据")
                BLEReader.getInstance().sendData(MOCK_SEND_DATA)
            }
        }

        /**
         * @see 4.6.4 onCharacteristicChanged(int status, Object data)
         */
        override fun onCharacteristicChanged(p0: Int, p1: Any?) {
            val data = p1 as ByteArray
            dynamicBytes = data
            var hexStr = StringBuilder()
            data.forEach {
                hexStr.append(String.format("%02X ", it))
            }
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

    /**
     * 蓝牙发现接收器
     */
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC

                    if (shouldShowDeviceInfo(deviceName, deviceHardwareAddress)) {
                        sprintInfo("$deviceName : $deviceHardwareAddress")
                    }
                }
            }
        }
    }

    /**
     * 是否应该显示蓝牙设备信息
     * <p>
     *     剔除设备无名称的设备，比如：null; 剔除重复数据. TODO: 应该根据蓝牙设备数据特征筛选
     * </p>
     * @param 设备名称
     * @param MAC
     * @return true 显示
     */
    private fun shouldShowDeviceInfo(name: String?, mac: String?): Boolean {
        if (!name.isNullOrBlank() && !macMap.containsKey(mac)) {
            macMap[mac] = name
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cardsView = findViewById(R.id.gv_container)
        infoListView = findViewById(R.id.lv_info)
        infoAdapter = ArrayAdapter(this, R.layout.array_list_view_layout, R.id.tv_array_item)
        infoListView?.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
        infoListView?.adapter = infoAdapter
        infoListView?.setOnItemClickListener { parent, view, position, id ->
            val info = infoAdapter?.getItem(position)
            showConfirmationDialog(info)
        }

        val cardList = configData()
        val cardAdapter = CardAdapter(this, cardList)
        cardsView?.adapter = cardAdapter

        BLEReader.getInstance().set_callback(bleCallback)
        initPermissionLauncher()
        // Android 6.0 及以上，需要动态获取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
        }

        // 发现蓝牙设备会通知此广播
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放蓝牙通道
        BLEReader.getInstance().disconnectGatt()
        // 释放蓝牙设备发现广播
        unregisterReceiver(receiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 接收蓝牙启用后的回调
        if (requestCode == REQUEST_ENABLE_BT) {
            sprintInfo("蓝牙已开启")
        }
    }

    /**
     * 连接确认
     */
    private fun showConfirmationDialog(info: String?) {
        val splitIndex = info?.indexOf(':') ?: -1
        if (splitIndex == -1) {
            return
        }
        val macAddress = info?.substring(splitIndex + 1)?.trim()

        val dialog = AlertDialog.Builder(this).apply {
            val title = SpannableString("是否要连接 $info")
            title.setSpan(ForegroundColorSpan(Color.parseColor("#A52A2A")), 0, title.length, 0)
            title.setSpan(AbsoluteSizeSpan(16, true), 0, title.length, 0)
            setTitle(title)

            setPositiveButton("连接") { _, _ ->
                // 连接发起即可停止扫描
                BLEReader.getInstance().bluetoothAdapter.cancelDiscovery()
                val status = BLEReader.getInstance().connectGatt(macAddress)
                if (status == 0) {
                    sprintInfo("蓝牙已连接")
                } else {
                    sprintInfo("蓝牙连接失败")
                }
            }
            setNegativeButton("取消") { _, _ -> com.zxfh.ble.BLEReader.handshake(context)}
        }.create()
        dialog.show()
        dialog.window?.setBackgroundDrawable(generateDialogBackground())
    }

    private fun generateDialogBackground() : Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            cornerRadius = 30f
        }
    }

    /**
     * 初始化数据源
     * @return ArrayList<CardModel>
     */
    private fun configData(): ArrayList<CardModel> {
        return ArrayList<CardModel>().apply {

            add(CardModel(1001, "连接设备") { connect() })
            add(CardModel(1002, "对卡上电") { upload() })
//            add(CardModel(1003, "对卡下电") { download() })
            add(CardModel(1004, "断开链接") { disconnect() })
            add(CardModel(1005, "清空显示") { clearScreen() })

            add(CardModel(2001, "检测卡类型") { checkMode() })
            add(CardModel(2002, "错误次数") { errorCount() })
            add(CardModel(2003, "读卡数据") { readData() })
            add(CardModel(2004, "核对密码") { checkPassword() })
//            add(CardModel(2005, "读卡密码") { readPassword() })

            add(CardModel(3001, "写卡数据") { writeData() })
            add(CardModel(3002, "更改密码") { modifyPassword() })
//            add(CardModel(3003, "保护标志") { protectFlag() })
//            add(CardModel(3004, "保护数据") { protectData() })
            add(CardModel(3005, "模块复位") { reset() })

            add(CardModel(4001, "预留按钮") { reservedPlace() })
        }
    }

    /**
     * 初始化权限获取回调
     */
    private fun initPermissionLauncher() {
        requestPermissionLaunch = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                performAction()
            } else {
                showInContextUI()
            }
        }
    }

    /**
     * 检查权限
     */
    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> { performAction() }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> { showInContextUI() }
            else -> {requestPermissionLaunch.launch(Manifest.permission.ACCESS_FINE_LOCATION)}
        }
    }

    /**
     * 权限获取后执行的动作
     */
    private fun performAction() {
        sprintInfo("FINE_LOCATION 权限已获取，可扫描附近蓝牙")
    }

    /**
     * 向用户解释权限的用途
     */
    private fun showInContextUI() {
        sprintInfo("FINE_LOCATION 权限未授予，不可扫描附近蓝牙")
    }

    /**
     * 查询已配对的蓝牙设备
     */
    private fun pairedDevices() {
        sprintInfo("已连接设备")
        val pairedDevices: Set<BluetoothDevice>? = BLEReader.INSTANCE.bluetoothAdapter.bondedDevices
        val deviceInfo = StringBuilder()
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address

            sprintInfo("$deviceName : $deviceHardwareAddress")
        }
    }

    /**
     * 输出信息
     */
    private fun sprintInfo(msg: String) {
        runOnUiThread {
            infoAdapter?.add(msg)
            infoAdapter?.notifyDataSetChanged()
        }
    }

    /**
     * 扫描设备
     */
    private fun scanDevices() {
        sprintInfo("蓝牙扫描开始...")
        BLEReader.INSTANCE.bluetoothAdapter.startDiscovery()
    }

    /**
     * 预留按钮位置
     */
    private fun reservedPlace() {

    }

    private fun protectData() {

    }

    private fun protectFlag() {

    }

    private fun readPassword() {

    }

    private fun download() {

    }

    /**
     * 修改密码
     */
    private fun modifyPassword() {
        val res = BLEReader.INSTANCE.MC_UpdatePIN_AT88SC102(0, "F0F0".toByteArray())
        sprintInfo("修改密码返回值 $res")
    }

    /**
     * 连接设备
     */
    private fun connect() {
        // 检查蓝牙是否启用
        if (BLEReader.getInstance().isBLEnabled) {
            scanDevices()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    /**
     * 模块复位
     */
    private fun reset() {
        val res = BLEReader.INSTANCE.ICC_Reset(ByteArray(16), IntArray(2))
        sprintInfo("卡片复位返回值 $res")
    }

    /**
     * 写卡数据
     */
    private fun writeData() {
        val res = BLEReader.INSTANCE.MC_Write_AT88SC102(3, 1408, "1234".toByteArray(), 0, 2)
        sprintInfo("写数据返回值 $res")
    }

    /**
     * 核对密码
     */
    private fun checkPassword() {
        val res = BLEReader.INSTANCE.MC_VerifyPIN_AT88SC102(0, "4840".toByteArray(), IntArray(1));
        sprintInfo("验证密码返回值 $res")
    }

    /**
     * 读卡数据
     */
    private fun readData() {
        val result = BLEReader.INSTANCE.MC_Read_AT88SC102(7, 0, 2, ByteArray(100))
        sprintInfo("读卡数据返回值 $result")

    }

    /**
     * 检测卡类型
     */
    private fun checkMode() {
        val res = BLEReader.INSTANCE.ICC_GetCardType(false)
        sprintInfo("卡类型 $res")
    }

    /**
     * 错误次数判断
     */
    private fun errorCount() {
        val res = BLEReader.INSTANCE.MC_PAC_AT88SC102(0)
        sprintInfo("错误次数返回值 $res")
    }

    /**
     * 对卡上电
     */
    private fun upload() {
//        val result = BLEReader.INSTANCE.ICC_Reset(ByteArray(100), IntArray(100))
        val res = BLEReader.INSTANCE.sendData(MOCK_SEND_DATA)
        sprintInfo("上电返回值 $res")
    }

    /**
     * 断开连接
     */
    private fun disconnect() {
        val result = BLEReader.INSTANCE.disconnectGatt()
        sprintInfo("断开连接 $result")
    }

    /**
     * 清屏
     */
    private fun clearScreen() {
        infoAdapter?.clear()
    }
}
