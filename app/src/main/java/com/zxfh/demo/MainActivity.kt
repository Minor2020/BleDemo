package com.zxfh.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.GridView

class MainActivity : AppCompatActivity() {
    var cardsView: GridView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cardsView = findViewById(R.id.gv_container)
        val cardList = initData()
        val cardAdapter = CardAdapter(this, cardList)
        cardsView?.adapter = cardAdapter
    }

    /**
     * 初始化数据源
     * @return ArrayList<CardModel>
     */
    private fun initData() : ArrayList<CardModel> {
        return ArrayList<CardModel>().apply {

            add(CardModel(1001, "连接设备"))
            add(CardModel(1002, "对卡上电"))
            add(CardModel(1003, "对卡下电"))
            add(CardModel(1004, "断开链接"))
            add(CardModel(1005, "清空显示"))

            add(CardModel(2001, "检测卡类型"))
            add(CardModel(2002, "错误次数"))
            add(CardModel(2003, "读卡数据"))
            add(CardModel(2004, "核对密码"))
            add(CardModel(2005, "读卡密码"))

            add(CardModel(3001, "写卡数据"))
            add(CardModel(3002, "更改密码"))
            add(CardModel(3003, "保护标志"))
            add(CardModel(3004, "保护数据"))
            add(CardModel(3005, "模块复位"))

            add(CardModel(4001, "预留按钮"))
        }
    }
}