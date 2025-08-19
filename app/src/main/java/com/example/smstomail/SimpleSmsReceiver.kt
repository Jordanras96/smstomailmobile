package com.example.smstomail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.widget.Toast

class SimpleSmsReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        try {
            val smsMessages = extractSmsMessages(intent)
            
            smsMessages.forEach { smsMessage ->
                val sender = smsMessage.displayOriginatingAddress ?: smsMessage.originatingAddress ?: "Unknown"
                val content = smsMessage.messageBody ?: ""
                
                if (content.isNotBlank()) {
                    processSms(context, sender, content)
                }
            }
        } catch (e: Exception) {
            // Log l'erreur silencieusement
        }
    }
    
    private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            val pdus = intent.extras?.get("pdus") as? Array<*>
            val format = intent.getStringExtra("format")
            
            pdus?.forEach { pdu ->
                if (pdu is ByteArray) {
                    val smsMessage = if (format != null) {
                        SmsMessage.createFromPdu(pdu, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu)
                    }
                    
                    smsMessage?.let { messages.add(it) }
                }
            }
        } catch (e: Exception) {
            // Retourne liste vide en cas d'erreur
        }
        
        return messages
    }
    
    private fun processSms(context: Context, sender: String, content: String) {
        try {
            val storage = SimpleStorage(context)
            val smsId = storage.saveSms(sender, content)
            
            if (smsId != null) {
                // SMS sauvé avec succès
                Toast.makeText(context, "SMS reçu de $sender", Toast.LENGTH_SHORT).show()
                
                // Déclencher l'envoi email (simple approche avec Intent)
                triggerEmailSend(context, smsId)
            } else {
                // SMS dupliqué, ignoré
                Toast.makeText(context, "SMS dupliqué ignoré", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur traitement SMS", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun triggerEmailSend(context: Context, smsId: String) {
        try {
            // Approche simple : démarrer un service pour l'envoi email
            val emailIntent = Intent(context, SimpleEmailService::class.java).apply {
                putExtra("sms_id", smsId)
            }
            context.startService(emailIntent)
        } catch (e: Exception) {
            // Erreur silencieuse
        }
    }
}