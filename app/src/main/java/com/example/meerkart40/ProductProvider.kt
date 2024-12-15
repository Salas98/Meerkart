package com.example.meerkart40


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