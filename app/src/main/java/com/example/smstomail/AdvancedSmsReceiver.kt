package com.example.smstomail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.*

/**
 * Récepteur SMS avancé avec support du filtrage automatique
 */
class AdvancedSmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AdvancedSmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS reçu - Action: ${intent.action}")
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "Aucun message SMS trouvé dans l'intent")
            return
        }
        
        // Traiter chaque SMS reçu
        messages.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress ?: "Inconnu"
            val content = smsMessage.messageBody ?: ""
            val timestamp = smsMessage.timestampMillis
            
            Log.d(TAG, "SMS reçu de $sender: ${content.take(50)}...")
            
            // Sauvegarder le SMS
            saveSMS(context, sender, content, timestamp)
            
            // Traitement automatique en arrière-plan
            processAutomaticForwarding(context, sender, content, timestamp)
        }
    }
    
    /**
     * Sauvegarde le SMS dans le stockage local
     */
    private fun saveSMS(context: Context, sender: String, content: String, timestamp: Long) {
        try {
            val storage = SimpleStorage(context)
            val sms = SimpleSms(
                id = "${timestamp}_${sender.hashCode()}",
                sender = sender,
                content = content,
                timestamp = timestamp
            )
            
            storage.saveSms(sms)
            Log.d(TAG, "SMS sauvegardé: ${sms.id}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde SMS", e)
        }
    }
    
    /**
     * Traite le transfert automatique du SMS selon les règles configurées
     */
    private fun processAutomaticForwarding(context: Context, sender: String, content: String, timestamp: Long) {
        // Lancer le traitement en arrière-plan
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        scope.launch {
            try {
                val smsProcessor = AdvancedSmsProcessor(context)
                val success = smsProcessor.processNewSMS(sender, content, timestamp)
                
                if (success) {
                    Log.d(TAG, "SMS auto-transféré avec succès")
                } else {
                    Log.d(TAG, "SMS non transféré (pas de règle correspondante ou auto-transfert désactivé)")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Erreur traitement automatique SMS", e)
            } finally {
                scope.cancel()
            }
        }
    }
}