package com.example.meerkart40

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class ListaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista)
        initRecyclerView()
        Log.d("ListaActivity", "Actividad Iniciada")

        // Procesar intent inicial
        handleNFCIntent(intent)
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
            Log.d("ListaActivity", "Foreground Dispatch Activado")
        }
        val adapter = ProductAdapter(ProductProvider.productList)
        adapter.notifyDataSetChanged()
        
    }

    override fun onPause() {
        super.onPause()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter?.disableForegroundDispatch(this)
        Log.d("ListaActivity", "Foreground Dispatch Desactivado")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("ListaActivity", "Nuevo Intent NFC recibido: ${intent.action}")
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
            Log.d("ListaActivity", "ID NFC Procesado: $nfcId")
            // Aquí puedes manejar los datos leídos
            var ref: String=""
            var nomProd: String = ""
            var precio: String = ""

            var newProduct: Productos


            runBlocking {
                try {
                    ref = ObtenerRef(nfcId.toString())
                    nomProd = ObtenerProducto(ref)
                    precio = ObtenerPrecio(ref)

                    Log.d("ProductProvider", "Producto newProduct")



                    val existingProduct = ProductProvider.productList.find { it.ref == ref.toInt() }
                    var i=0

                    if (existingProduct != null) {
                        // Producto existente: actualizar cantidad y precio
                        existingProduct.cantidad++
                        existingProduct.precio += precio.toDouble() // Actualizar precio si es necesario.
                    } else {
                        newProduct = Productos(ref.toInt(), nomProd, 1, precio.toDouble())
                        ProductProvider.addProduct(newProduct)
                    }
                    UpdateTotal()










                } catch (e:Exception){
                    Log.d("Detecto", "${e.message}")
                }
            }
            Log.d("ProductProvider", "HOLA" + ProductProvider.productList.toString())

        }

    }

    private fun processNFC(intent: Intent): String {
        var nfcId: String = ""
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val idTag = tag?.id
        Log.d("ListaActivity", "ID Tag leído: $idTag")
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            Log.d("Funciona", "primer_if")
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMsgs != null) {
                val msgs = arrayOfNulls<NdefMessage>(rawMsgs.size)
                for (i in rawMsgs.indices) {
                    msgs[i] = rawMsgs[i] as NdefMessage
                    Log.d("msgs", msgs[i].toString())
                }
                Log.d("idMsg", readNdefMessage(msgs))
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
        val subtotal = ProductProvider.productList.sumOf { it.precio * it.cantidad }
        val subtotalTextView: TextView = findViewById(R.id.subtotalBox)
        subtotalTextView.text = String.format("Total: \n%.2f €", subtotal)
    }

}

//    override fun onCreate(savedInstanceState: Bundle?) {
//
//
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_lista)
//        initRecyclerView()
// //      ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.lista)) { v, insets ->
// //          val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
// //          v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
// //         insets
// //     }
//
//
//        val ref = intent.getStringExtra("REF")
//        val nomProd = intent.getStringExtra("NOM_PROD")
//        val precio = intent.getDoubleExtra("PRECIO", 0.0)
//
//        // Usar las variables recibidas según sea necesario
//        Log.d("ListaActivity", "Referencia: $ref, Nombre Producto: $nomProd, Precio: $precio")
//
//
//   }
//
//    override fun onResume() {
//        super.onResume()
//        // Actualiza la lista cuando la actividad se reanuda
//        val adapter = ProductAdapter(ProductProvider.productList)
//        adapter.notifyDataSetChanged()
//    }
//
//    private fun initRecyclerView(){
//        val recyclerView = findViewById<RecyclerView>(R.id.recyclerProductos)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = ProductAdapter(ProductProvider.productList)
//
//    }

