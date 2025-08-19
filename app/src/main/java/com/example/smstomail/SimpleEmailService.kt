package com.example.smstomail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class SimpleEmailService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val smsId = intent?.getStringExtra("sms_id")
        
        if (smsId != null) {
            serviceScope.launch {
                sendSmsEmail(smsId)
                stopSelf(startId)
            }
        }
        
        return START_NOT_STICKY
    }
    
    private suspend fun sendSmsEmail(smsId: String) {
        try {
            val storage = SimpleStorage(this)
            val config = SimpleConfig(this)
            val gmailSender = SimpleGmailSender(this)
            
            // Récupérer le SMS
            val smsList = storage.getPendingSms()
            val sms = smsList.find { it.id == smsId }
            
            if (sms == null) {
                return // SMS introuvable
            }
            
            // Vérifier l'authentification Gmail
            if (!gmailSender.isAuthenticated()) {
                showToast("Gmail non authentifié")
                return
            }
            
            // Vérifier la configuration email
            val emailConfig = config.getEmailConfig()
            if (emailConfig.recipientEmail.isBlank()) {
                showToast("Email destinataire non configuré")
                return
            }
            
            // Vérifier si une règle de filtrage s'applique
            val matchingRule = config.findMatchingRule(sms.sender, sms.content)
            val recipientEmail = matchingRule?.recipientEmail ?: emailConfig.recipientEmail
            
            // Préparer l'email
            val subject = "[SMS] ${sms.sender}"
            val body = buildEmailBody(sms)
            
            // Envoyer l'email via Gmail API
            if (gmailSender.sendEmail(subject, body, recipientEmail)) {
                // Marquer comme envoyé
                storage.markAsSent(smsId)
                
                // Notification de succès
                showToast("SMS envoyé par email ✅")
            } else {
                showToast("Erreur envoi email ❌")
            }
            
        } catch (e: Exception) {
            showToast("Erreur: ${e.message}")
        }
    }
    
    private fun buildEmailBody(sms: SimpleStorage.SimpleSms): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sms.timestamp))
        
        return """
SMS reçu automatiquement

Expéditeur: ${sms.sender}
Date: $dateStr
Contenu:

${sms.content}

---
Envoyé automatiquement par SMS to Mail App
        """.trimIndent()
    }
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
    
    private fun showToast(message: String) {
        // Utiliser Handler pour afficher Toast depuis thread background
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}