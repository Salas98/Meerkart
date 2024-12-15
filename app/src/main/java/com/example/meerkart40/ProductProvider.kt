package com.example.meerkart40

import android.util.Log
import androidx.recyclerview.widget.RecyclerView

object ProductProvider {
    // Lista de productos
    val productList: MutableList<Productos> = mutableListOf()

    // Metodo para agregar un producto
    fun addProduct(product: Productos) {
        productList.add(product)
    }

    fun vaciar(){
        productList.clear()
    }

}