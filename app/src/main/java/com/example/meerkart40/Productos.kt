package com.example.meerkart40

import androidx.recyclerview.widget.RecyclerView

data class Productos (
    val ref: Int,
    val nombre:String,
    var cantidad:Int,
    var precio: Double
)
