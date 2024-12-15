package com.example.meerkart40

import android.app.blob.BlobStoreManager
import android.os.AsyncTask
import android.util.Log
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class ticket(private val Email: String,
             private val asunto: String,
             private val compra: String
) : AsyncTask<Void, Void, Void>()  {

    private val username = "labxat.2024@gmail.com"  // Tu correo de Gmail
    private val password = "qkhv qoha biwo zppx"  // Tu contraseña de aplicación de Gmail

    override fun doInBackground(vararg params: Void?): Void? {
        try {
            // Configuración de propiedades para SMTP de Gmail
            val properties = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
            }

            // Autenticación con el servidor SMTP usando la contraseña de aplicación
            val session = Session.getInstance(properties, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

            // Crear el mensaje de correo
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(Email))
                setSubject(asunto)
                setText(compra)
            }

            // Enviar el correo
            Transport.send(message)
            Log.d("EmailSender", "Correo enviado exitosamente")
        } catch (e: Exception) {
            Log.e("EmailSender", "Error al enviar el correo: ${e.message}")
        }
        return null
    }
}