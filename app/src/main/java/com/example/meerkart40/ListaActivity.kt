package com.example.meerkart40

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import io.github.jan.supabase.postgrest.query.Order


class ListaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista)
        initRecyclerView()
        val email = intent.getStringExtra("Usuario") ?: ""
        runBlocking {
            try {
                usuario(email)
            } catch (e:Exception){}
        }
        handleNFCIntent(intent)
        ticket(email)


    }

    override fun onResume() {
        super.onResume()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, this::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            nfcAdapter.enableForegroundDispatch(
                this,
                pendingIntent,
                null,
                null
            )
        }
        val adapter = ProductAdapter(ProductProvider.productList)
        adapter.notifyDataSetChanged()

    }

    override fun onPause() {
        super.onPause()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initRecyclerView()
        UpdateTotal()
        handleNFCIntent(intent)

    }

    private suspend fun ObtenerRef(nfcId: String): String {
        val ref =supabase.from("UNIDAD"). select(columns = Columns.list("referencia")){
            filter {
                eq("idUnidad", nfcId)
            }

        }
        return ref.data.dropLast(2).takeLast(1)
    }

    private suspend fun ObtenerProducto(ref: String): String{
        val nomProd = supabase.from("PRODUCTO").select (columns = Columns.list("nomProd")){
            filter {
                eq ("referencia", ref)
            }
        }
        return nomProd.data.dropLast(3).drop(13)
    }

    private suspend fun ObtenerPrecio(ref: String): String {
        val precio = supabase.from("PRODUCTO").select (columns = Columns.list("precio")){
            filter {
                eq ("referencia", ref)
            }
        }
        return precio.data.dropLast(2).substringAfter(":")
    }

    private fun handleNFCIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {

            val nfcId = processNFC(intent)

            // Aquí puedes manejar los datos leídos
            var ref: String=""
            var nomProd: String = ""
            var precio: String = ""

            var newProduct: Productos


            runBlocking {
                try {
                    val comprado = supabase.from("UNIDAD").select(columns = Columns.list("comprado")) {
                        filter {
                            eq("idUnidad", nfcId)
                        }
                    }
                    if (!comprado.data.dropLast(2).drop(13).toBoolean()) {
                        ref = ObtenerRef(nfcId.toString())
                        nomProd = ObtenerProducto(ref)
                        precio = ObtenerPrecio(ref)

                        val existingProduct =
                            ProductProvider.productList.find { it.ref == ref.toInt() }
                        var i = 0

                        if (existingProduct != null) {
                            // Producto existente: actualizar cantidad y precio
                            existingProduct.cantidad++
                            existingProduct.precio += precio.toDouble() // Actualizar precio si es necesario.
                        } else {
                            newProduct = Productos(ref.toInt(), nomProd, 1, precio.toDouble())
                            ProductProvider.addProduct(newProduct)
                        }
                        supabase.from("UNIDAD").update({set("comprado", "TRUE")}){filter { eq("idUnidad", nfcId) }}
                    }

                } catch (e:Exception){}
            }
        }
        UpdateTotal()

    }

    private fun processNFC(intent: Intent): String {
        var nfcId: String = ""
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val idTag = tag?.id
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null) {
                val msgs = arrayOfNulls<NdefMessage>(rawMsgs.size)
                for (i in rawMsgs.indices) {
                    msgs[i] = rawMsgs[i] as NdefMessage
                }
                nfcId = readNdefMessage(msgs)
            }
        }
        return nfcId
    }

    private fun readNdefMessage(msgs: Array<NdefMessage?>): String {

        var nfcId: String = ""
        for (msg in msgs) {
            msg?.let {
                for (record in it.records) {
                    val payload = record.payload
                    val text = String(payload, Charsets.UTF_8)
                    nfcId = text.takeLast(5)
                }
            }
        }
        return nfcId
    }

    private fun initRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerProductos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductAdapter(ProductProvider.productList)
    }

    private fun UpdateTotal(){
        val subtotal = ProductProvider.productList.sumOf { it.precio }
        val subtotalTextView: TextView = findViewById(R.id.subtotalBox)
        subtotalTextView.text = String.format("Total: \n%.2f €", subtotal)
    }

    private suspend fun usuario(email: String){
        val usuario : TextView = findViewById(R.id.username)

        val nombre = supabase.from("CLIENTE").select (columns = Columns.list("nomCli")){
            filter {
                eq ("email", email)
            }
        }

        val apellido = supabase.from("CLIENTE").select (columns = Columns.list("apellido")){
            filter {
                eq ("email", email)
            }
        }
        usuario.text = String.format("${nombre.data.dropLast(3).substringAfter("\"nomCli\":\"").substringBefore('"')}  ${apellido.data.dropLast(3).substringAfter("\"apellido\":\"").substringBefore('"')}")
    }

    private fun ticket(email: String){
        val sendEmailButton = findViewById<Button>(R.id.botonPago)
        sendEmailButton.setOnClickListener {
            if (ProductProvider.productList.size > 0) {
                val maxLength = ProductProvider.productList.maxOf { it.nombre.length }
                val compra = StringBuilder()


                compra.append(
                    "%-${maxLength}s | %8s | %7s|\n".format(
                        "Nombre",
                        "Cantidad",
                        "Precio"
                    )
                )
                compra.append("-".repeat(maxLength) + "-|----------|--------|\n")
                for (producto in ProductProvider.productList) {
                    val (_, nombre, cantidad, precio) = producto
                    compra.append(
                        "%-${maxLength}s | %8d | %7.2f\n".format(
                            nombre,
                            cantidad,
                            precio
                        )
                    )
                }

                compra.append("----------------------------------------------------------")
                compra.append("----------------------------------------------------------")
                compra.append("Total de la compra: "+ "${ProductProvider.productList.sumOf { it.precio }} €" )
                ticket(email, "ticket-digital", compra.toString()).execute()
                runBlocking {
                    try {
                        compra(email)
                    } catch (e: Exception){ Log.d("compra", e.message.toString())}
                }
                ProductProvider.vaciar()
                initRecyclerView()

            }else {
                MainActivity.alerta(this, "" ,"La cesta está vacía", 1000)}

        }

    }

    private suspend fun compra (email: String){
        val i=0
        val idCompra = idCompra(i)

        val ticket = MainActivity.compra(idCompra = idCompra, email = email, precioCompra = ProductProvider.productList.sumOf { it.precio }.toFloat())
        supabase.from("COMPRA").insert(ticket)



        for (producto in ProductProvider.productList){
            val cantidad = MainActivity.cantidad(idCompra = idCompra, referencia = producto.ref, cantidad = producto.cantidad)
            Log.d("compra3", cantidad.toString())
            supabase.from("CANTIDAD"). insert(cantidad)
        }



    }

    private suspend fun idCompra(i: Int): Int {
        var i=0
        while (!existe(i)){
            i += 1
        }
        return i
    }

    private suspend fun existe(i: Int): Boolean{
        val compra = supabase.from("COMPRA").select(columns = Columns.list("idCompra")){
            filter {
                eq("idCompra", i)
            }
        }
        return compra.data == "[]"
    }




}

