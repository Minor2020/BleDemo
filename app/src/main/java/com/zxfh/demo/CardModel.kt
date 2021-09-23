package com.zxfh.demo

import android.view.View

class CardModel(val id: Int, val name: String, val action: (view: View) -> Unit)