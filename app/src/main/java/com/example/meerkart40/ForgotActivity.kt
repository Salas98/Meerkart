package com.example.meerkart40

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

class ForgotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var forgotEmail: EditText = findViewById(R.id.forgot_email)
        var email = forgotEmail.text.toString()
        val botoSendForgot: Button =findViewById(R.id.envia_correo_recu)
        botoSendForgot.setOnClickListener {
            runBlocking {
                try {
                    resetPasswordEmail(email)
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun resetPasswordEmail(correo: String){
        supabase.auth.resetPasswordForEmail(
            email = correo,
            redirectUrl = "https://jotaemers.github.io/password-reset.html"
        )
    }
    
}
