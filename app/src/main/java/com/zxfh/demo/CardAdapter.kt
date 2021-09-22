package com.zxfh.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CardAdapter(context: Context, objects: ArrayList<CardModel>) :
    ArrayAdapter<CardModel>(context, 0, objects) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var item = convertView
        if (item == null) {
            item = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false)
        }
        val cardModel = getItem(position)
        item?.apply {
            findViewById<TextView>(R.id.tv_card).apply { text = cardModel?.name }
            setOnClickListener {  }
        }
        return item!!
    }
}