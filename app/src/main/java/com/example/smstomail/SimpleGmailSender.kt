package com.example.smstomail

import android.content.Context
import android.util.Log

@Deprecated("Use OAuth2GmailSender instead")
// Classe obsolète - Utiliser OAuth2GmailSender maintenant

class SimpleGmailSender(private val context: Context) {
    
    companion object {
        private const val TAG = "SimpleGmailSender"
    }
    
    init {
        Log.w(TAG, "SimpleGmailSender is deprecated. Use OAuth2GmailSender instead.")
    }
    
    fun isAuthenticated(): Boolean {
        Log.w(TAG, "Method deprecated - returning false")
        return false
    }
    
    @Deprecated("Use OAuth2GmailSender.sendEmail() instead")
    suspend fun sendEmail(subject: String, body: String, recipientEmail: String): Boolean {
        Log.e(TAG, "This method is deprecated. Use OAuth2GmailSender instead.")
        return false
    }
}