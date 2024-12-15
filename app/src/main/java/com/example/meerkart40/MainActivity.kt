package com.example.meerkart40

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable


val supabase = createSupabaseClient(
    supabaseUrl = "https://zdgpnziviiipmsyvupoh.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkZ3Bueml2aWlpcG1zeXZ1cG9oIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzEzNDQzMzcsImV4cCI6MjA0NjkyMDMzN30.z_21oHpHaOBLLR_ndfUzw3lyLIAlD06Wfq1j_-XuXXU"
){
    install(Auth)
    install(Postgrest)
}

class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val botoRegister: Button = findViewById<Button>(R.id.button_sign_in)
        val forgotPassword:TextView=findViewById(R.id.ForgotPassword)
        val boto_login: Button = findViewById(R.id.button_login)

        gotoRegister(botoRegister)
        gotoForgotPass(forgotPassword)

        val usuario: EditText = findViewById(R.id.email)
        val contraseña: EditText = findViewById(R.id.contra)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        val titulo = intent.getStringExtra("ALERT_TITLE") ?: ""
        val mensaje = intent.getStringExtra("ALERT_MESSAGE") ?: ""

        if (titulo != ""){
            alerta(this, titulo, mensaje, 2000)
        }

        boto_login.setOnClickListener {
            var email = usuario.text.toString()
            var contra = contraseña.text.toString()
            runBlocking {
            try{
                inicioSesion(email, contra)
                gotoLista(email)

            } catch (e: Exception){
                alerta(this@MainActivity, "Error en inicio de sesión", "Usuario o contraseña incorrectos", 1000)
            }
        }
        }







    }
    fun gotoRegister(botoRegister:Button){
        botoRegister.setOnClickListener {
            val go = Intent(this, RegisterActivity::class.java)
            startActivity(go)
        }

    }
    fun gotoForgotPass(forgotPass:TextView){
        forgotPass.setOnClickListener {
            val goFP= Intent(this,ForgotActivity::class.java)
            startActivity(goFP)
        }

    }

    suspend fun inicioSesion(correo: String, contra: String){
        supabase.auth.signInWith(Email) {
            email = correo
            password = contra
        }
    }

    fun gotoLista(correo: String){
            val goLista = Intent(this, ListaActivity::class.java)
        goLista.putExtra("Usuario", correo)
            startActivity(goLista)

    }



    companion object{
        fun alerta(context:Activity, titulo: String, mensaje: String, duration: Long){
                val alert = AlertDialog.Builder(context)
                    .setTitle(titulo)
                    .setMessage(mensaje)
                    .create()

                alert.show()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (alert.isShowing){
                        alert.dismiss()
                    }
                }, duration)


        }
    }

    @Serializable
    data class cliente(
        val email: String,
        val nomCli: String,
        val apellido: String,
        val pag1:String,
        val pag2: String
    )

    @Serializable
    data class  unidad(
        val id : Int,
        val comprado: Boolean,
        val referencia: Int
    )

    @Serializable
    data class  compra(
        val idCompra: Int,
        val email: String,
        val precioCompra: Float
    )

    @Serializable
    data class cantidad(
        val idCompra: Int,
        val referencia: Int,
        val cantidad: Int
    )



}