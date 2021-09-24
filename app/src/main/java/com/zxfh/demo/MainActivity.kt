package com.zxfh.demo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
    /** BLE SDK 回调 */
    private val bleCallback = object : IBLEReader_Callback {
        override fun onLeScan(p0: MutableList<WDBluetoothDevice>?) {
        }

        override fun onConnectGatt(p0: Int, p1: Any?) {

        }

        override fun onServicesDiscovered(p0: Int, p1: Any?) {
        }

        override fun onCharacteristicChanged(p0: Int, p1: Any?) {
        }

        override fun onReadRemoteRssi(p0: Int) {
        }

        override fun onOTA(p0: Int, p1: Any?) {
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

        BLEReader.getInstance().callback = bleCallback
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
            add(CardModel(1003, "对卡下电") { download() })
            add(CardModel(1004, "断开链接") { disconnect() })
            add(CardModel(1005, "清空显示") { clearScreen() })

            add(CardModel(2001, "检测卡类型") { checkMode() })
            add(CardModel(2002, "错误次数") { errorCount() })
            add(CardModel(2003, "读卡数据") { readData() })
            add(CardModel(2004, "核对密码") { checkPassword() })
            add(CardModel(2005, "读卡密码") { readPassword() })

            add(CardModel(3001, "写卡数据") { writeData() })
            add(CardModel(3002, "更改密码") { modifyPassword() })
            add(CardModel(3003, "保护标志") { protectFlag() })
            add(CardModel(3004, "保护数据") { protectData() })
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
        infoAdapter?.add(msg)
        infoAdapter?.notifyDataSetChanged()
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

    private fun connect() {
        // 检查蓝牙是否启用
        if (BLEReader.getInstance().isBLEnabled) {
            scanDevices()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun reset() {

    }

    private fun protectData() {

    }

    private fun protectFlag() {

    }

    private fun modifyPassword() {

    }

    private fun writeData() {

    }

    private fun readPassword() {

    }

    private fun checkPassword() {

    }

    private fun readData() {

    }

    private fun checkMode() {

    }

    private fun errorCount() {

    }

    private fun download() {

    }

    private fun upload() {

    }

    private fun disconnect() {
        BLEReader.INSTANCE.disconnectGatt()
    }

    private fun clearScreen() {
        infoAdapter?.clear()
    }
}
