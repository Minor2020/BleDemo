package com.zxfh.demo

import android.Manifest
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 *
 * 该类用作 BLEDemo 界面展示；权限申请；蓝牙扫码配对
 * @author zxfh
 */
class MainActivity : AppCompatActivity() {
    /** 按钮面板容器 */
    private var cardsView: GridView? = null
    /** 信息显示view */
    private var infoListView: ListView? = null
    /** 信息listAdapter */
    private var infoAdapter: ArrayAdapter<String>? = null
    /** BLE sdk 接入适配器 */
    private var bleSdkAdapter: BleSdkAdapter? = null
    /** 只为了 mac 去重显示 */
    private val macMap: HashMap<String?, String?> = HashMap()
    /** 请求权限回调 */
    private lateinit var requestPermissionLaunch: ActivityResultLauncher<String>

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

        bleSdkAdapter = BleSdkAdapter(this)

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
        bleSdkAdapter?.releaseGatt()
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
                val status = bleSdkAdapter?.pairBle(macAddress)
                if (status == 0) {
                    sprintInfo("蓝牙已连接")
                } else {
                    sprintInfo("蓝牙连接失败")
                }
            }
            setNegativeButton("取消") { _, _ -> }
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

            add(CardModel(1001, "连接设备") { bleSdkAdapter?.connect() })
            add(CardModel(1002, "对卡上电") { bleSdkAdapter?.upload() })
            add(CardModel(1004, "断开链接") { bleSdkAdapter?.disconnect() })
            add(CardModel(1005, "清空显示") { clearScreen() })
            add(CardModel(2001, "检测卡类型") { bleSdkAdapter?.checkMode() })
            add(CardModel(2002, "错误次数") { bleSdkAdapter?.errorCount() })
            add(CardModel(2003, "读卡数据") { bleSdkAdapter?.readData() })
            add(CardModel(2004, "核对密码") { bleSdkAdapter?.checkPassword() })
            add(CardModel(3001, "写卡数据") { bleSdkAdapter?.writeData() })
            add(CardModel(3002, "更改密码") { bleSdkAdapter?.modifyPassword() })
            add(CardModel(3005, "模块复位") { bleSdkAdapter?.reset() })
            add(CardModel(4001, "SM4测试") { bleSdkAdapter?.testSM4() })

//            add(CardModel(1003, "对卡下电") { download() })
//            add(CardModel(2005, "读卡密码") { readPassword() })
//            add(CardModel(3003, "保护标志") { protectFlag() })
//            add(CardModel(3004, "保护数据") { protectData() })
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
     * 输出信息
     */
    fun sprintInfo(msg: String) {
        runOnUiThread {
            infoAdapter?.add(msg)
            infoAdapter?.notifyDataSetChanged()
        }
    }

    /**
     * 清屏
     */
    private fun clearScreen() {
        infoAdapter?.clear()
    }
}

/** 请求启动蓝牙返回码 */
const val REQUEST_ENABLE_BT = 1
