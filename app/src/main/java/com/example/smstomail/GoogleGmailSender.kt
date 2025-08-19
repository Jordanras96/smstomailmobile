package com.example.smstomail

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import android.util.Base64

/**
 * Gestionnaire d'envoi d'emails via Gmail API avec Google Identity Services
 * Remplace OAuth2GmailSender qui utilisait AppAuth
 */
class GoogleGmailSender(
    private val context: Context,
    private val googleSignInManager: GoogleSignInManager
) {
    
    companion object {
        private const val TAG = "GoogleGmailSender"
        private const val APPLICATION_NAME = "SMS to Mail Android App"
    }
    
    /**
     * Envoie un email via Gmail API
     */
    suspend fun sendEmail(
        subject: String,
        body: String,
        recipientEmail: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Début envoi email vers: $recipientEmail")
            
            // Obtenir un token d'accès valide
            val accessToken = googleSignInManager.getValidAccessToken()
            if (accessToken == null) {
                Log.e(TAG, "Aucun token d'accès valide disponible")
                return@withContext false
            }
            
            // Créer le service Gmail
            val credential = GoogleCredential().setAccessToken(accessToken)
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            
            val service = Gmail.Builder(transport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
            
            // Créer le message email
            val emailMessage = createEmailMessage(
                recipientEmail = recipientEmail,
                subject = subject,
                bodyText = body
            )
            
            if (emailMessage == null) {
                Log.e(TAG, "Impossible de créer le message email")
                return@withContext false
            }
            
            // Envoyer via Gmail API
            val result = service.users().messages().send("me", emailMessage).execute()
            
            if (result != null && result.id != null) {
                Log.d(TAG, "Email envoyé avec succès. ID: ${result.id}")
                return@withContext true
            } else {
                Log.e(TAG, "Échec de l'envoi - réponse invalide")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'envoi email", e)
            return@withContext false
        }
    }
    
    /**
     * Crée un message Gmail API à partir des paramètres
     */
    private suspend fun createEmailMessage(
        recipientEmail: String,
        subject: String,
        bodyText: String
    ): Message? = withContext(Dispatchers.IO) {
        try {
            // Obtenir l'email de l'utilisateur connecté
            val userAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            val senderEmail = userAccount?.email ?: "me@gmail.com"
            
            Log.d(TAG, "Création message de $senderEmail vers $recipientEmail")
            
            // Créer le message MIME
            val props = Properties()
            val session = Session.getDefaultInstance(props, null)
            
            val email = MimeMessage(session)
            email.setFrom(InternetAddress(senderEmail))
            email.addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(recipientEmail))
            email.subject = subject
            email.setText(bodyText, "utf-8", "plain")
            
            // Convertir en format Gmail API
            val buffer = ByteArrayOutputStream()
            email.writeTo(buffer)
            val bytes = buffer.toByteArray()
            val encodedEmail = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
            
            val message = Message()
            message.raw = encodedEmail
            
            Log.d(TAG, "Message MIME créé (${bytes.size} bytes)")
            return@withContext message
            
        } catch (e: MessagingException) {
            Log.e(TAG, "Erreur création message MIME", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur générale création message", e)
            return@withContext null
        }
    }
    
    /**
     * Vérifie si l'authentification Gmail est active
     */
    fun isAuthenticated(): Boolean {
        return googleSignInManager.isSignedIn()
    }
    
    /**
     * Teste la connexion Gmail en récupérant les informations du profil
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val accessToken = googleSignInManager.getValidAccessToken()
            if (accessToken == null) {
                Log.w(TAG, "Test connexion: aucun token disponible")
                return@withContext false
            }
            
            // Créer le service Gmail pour test
            val credential = GoogleCredential().setAccessToken(accessToken)
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            
            val service = Gmail.Builder(transport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
            
            // Tester en récupérant le profil utilisateur
            val profile = service.users().getProfile("me").execute()
            
            if (profile != null && profile.emailAddress != null) {
                Log.d(TAG, "Test connexion réussi pour: ${profile.emailAddress}")
                return@withContext true
            } else {
                Log.w(TAG, "Test connexion: réponse invalide")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur test connexion Gmail", e)
            return@withContext false
        }
    }
}