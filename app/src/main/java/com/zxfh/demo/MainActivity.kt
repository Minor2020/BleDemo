package com.zxfh.demo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.GridView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ble.zxfh.sdk.blereader.BLEReader

class MainActivity : AppCompatActivity() {
    /** 请求启动蓝牙返回码 */
    private val REQUEST_ENABLE_BT = 1
    /** 按钮面板容器 */
    private var cardsView: GridView? = null
    /** 请求权限回调 */
    private lateinit var requestPermissionLaunch: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cardsView = findViewById(R.id.gv_container)
        val cardList = initData()
        val cardAdapter = CardAdapter(this, cardList)
        cardsView?.adapter = cardAdapter

        initPermissionLauncher()

        // Android 6.0 及以上，需要动态获取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放蓝牙通道
        BLEReader.getInstance().disconnectGatt()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 接收蓝牙启用后的回调
        if (requestCode == REQUEST_ENABLE_BT) {
            Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 初始化数据源
     * @return ArrayList<CardModel>
     */
    private fun initData(): ArrayList<CardModel> {
        return ArrayList<CardModel>().apply {

            add(CardModel(1001, "连接设备") { connect() })
            add(CardModel(1002, "对卡上电") { upload() })
            add(CardModel(1003, "对卡下电") { download() })
            add(CardModel(1004, "断开链接") { disconnect() })
            add(CardModel(1005, "清空显示") { clearScreen() })

            add(CardModel(2001, "检测卡类型") { checkType() })
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> { performAction() }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> { showInContextUI() }
            else -> {requestPermissionLaunch.launch(Manifest.permission.ACCESS_COARSE_LOCATION)}
        }
    }

    /**
     * 权限获取后执行的动作
     */
    private fun performAction() {
        Toast.makeText(this, "权限已获取", Toast.LENGTH_SHORT).show()
    }

    /**
     * 向用户解释权限的用途
     */
    private fun showInContextUI() {
        Toast.makeText(this, "未授予相关权限，程序不可用", Toast.LENGTH_SHORT).show()
    }

    private fun connect() {
        // 检查蓝牙是否启用
        if (BLEReader.getInstance().isBLEnabled) {
            // TODO：连接读卡器
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun reservedPlace() {

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

    private fun checkType() {

    }

    private fun errorCount() {

    }

    private fun download() {

    }

    private fun upload() {

    }

    private fun disconnect() {

    }

    private fun clearScreen() {

    }
}