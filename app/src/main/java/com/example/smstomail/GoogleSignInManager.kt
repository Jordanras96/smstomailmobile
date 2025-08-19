package com.example.smstomail

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Gestionnaire d'authentification Google utilisant Google Identity Services
 * Remplace OAuth2GmailManager qui utilisait AppAuth
 */
class GoogleSignInManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleSignIn"
        private const val PREFS_NAME = "google_auth_state"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }
    
    private val googleSignInClient: GoogleSignInClient
    
    init {
        // Configuration Google Sign-In avec les scopes Gmail
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(OAuth2Config.WEB_CLIENT_ID) // Server-side auth pour Gmail API
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(OAuth2Config.SCOPE_GMAIL_SEND) // Gmail send scope
            )
            .build()
            
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        Log.d(TAG, "GoogleSignInManager initialisé avec Web Client ID: ${OAuth2Config.WEB_CLIENT_ID}")
    }
    
    /**
     * Obtient le client Google Sign-In pour lancer l'authentification
     */
    fun getSignInClient(): GoogleSignInClient = googleSignInClient
    
    /**
     * Vérifie si l'utilisateur est connecté et a les permissions Gmail
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val hasGmailScope = account?.grantedScopes?.contains(Scope(OAuth2Config.SCOPE_GMAIL_SEND)) == true
        val hasValidTokens = hasValidAccessToken()
        
        Log.d(TAG, "État de connexion - Account: ${account != null}, Gmail scope: $hasGmailScope, Valid tokens: $hasValidTokens")
        return account != null && hasGmailScope && hasValidTokens
    }
    
    /**
     * Traite le résultat de l'authentification et échange le code d'autorisation
     */
    fun handleSignInResult(serverAuthCode: String?, callback: (Boolean, String?) -> Unit) {
        if (serverAuthCode == null) {
            Log.e(TAG, "Server auth code est null")
            callback(false, "Code d'autorisation manquant")
            return
        }
        
        Log.d(TAG, "Code d'autorisation reçu, échange contre les tokens...")
        
        // Échanger le code contre les tokens en arrière-plan
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokens = exchangeAuthorizationCode(serverAuthCode)
                
                if (tokens != null) {
                    saveTokens(tokens)
                    Log.d(TAG, "Tokens sauvegardés avec succès")
                    
                    withContext(Dispatchers.Main) {
                        callback(true, "Authentification Google réussie")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(false, "Impossible d'obtenir les tokens d'accès")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'échange de tokens", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Erreur d'authentification: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Échange le code d'autorisation contre les tokens d'accès
     */
    private suspend fun exchangeAuthorizationCode(authCode: String): TokenResponse? = withContext(Dispatchers.IO) {
        try {
            val url = URL(OAuth2Config.TOKEN_URI)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            
            // Paramètres pour l'échange de code
            val params = mapOf(
                "client_id" to OAuth2Config.WEB_CLIENT_ID,
                "code" to authCode,
                "grant_type" to "authorization_code"
            )
            
            val postData = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", null)
                val expiresIn = json.optInt("expires_in", 3600)
                
                Log.d(TAG, "Tokens obtenus - Access token présent: ${accessToken.isNotEmpty()}, Refresh token: ${refreshToken != null}")
                
                TokenResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresInSeconds = expiresIn
                )
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Erreur échange de code: ${connection.responseCode} - $errorResponse")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'échange de code", e)
            null
        }
    }
    
    /**
     * Obtient un token d'accès valide (refresh si nécessaire)
     */
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        
        if (accessToken != null && System.currentTimeMillis() < expiryTime) {
            Log.d(TAG, "Token d'accès valide trouvé")
            return@withContext accessToken
        }
        
        if (refreshToken != null) {
            Log.d(TAG, "Token expiré, tentative de refresh...")
            val newTokens = refreshAccessToken(refreshToken)
            if (newTokens != null) {
                saveTokens(newTokens)
                return@withContext newTokens.accessToken
            }
        }
        
        Log.w(TAG, "Aucun token d'accès valide disponible")
        return@withContext null
    }
    
    /**
     * Refresh le token d'accès
     */
    private suspend fun refreshAccessToken(refreshToken: String): TokenResponse? = withContext(Dispatchers.IO) {
        try {
            val url = URL(OAuth2Config.TOKEN_URI)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            
            val params = mapOf(
                "client_id" to OAuth2Config.WEB_CLIENT_ID,
                "refresh_token" to refreshToken,
                "grant_type" to "refresh_token"
            )
            
            val postData = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val accessToken = json.getString("access_token")
                val newRefreshToken = json.optString("refresh_token", refreshToken) // Garde l'ancien si pas de nouveau
                val expiresIn = json.optInt("expires_in", 3600)
                
                Log.d(TAG, "Token refreshed avec succès")
                
                TokenResponse(
                    accessToken = accessToken,
                    refreshToken = newRefreshToken,
                    expiresInSeconds = expiresIn
                )
            } else {
                Log.e(TAG, "Erreur refresh token: ${connection.responseCode}")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors du refresh", e)
            null
        }
    }
    
    /**
     * Sauvegarde les tokens en local
     */
    private fun saveTokens(tokens: TokenResponse) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiryTime = System.currentTimeMillis() + (tokens.expiresInSeconds * 1000L)
        
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            putLong(KEY_TOKEN_EXPIRY, expiryTime)
        }
        
        Log.d(TAG, "Tokens sauvegardés, expiration: ${expiryTime - System.currentTimeMillis()}ms")
    }
    
    /**
     * Vérifie si on a un token d'accès valide
     */
    private fun hasValidAccessToken(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        
        return accessToken != null && System.currentTimeMillis() < expiryTime
    }
    
    /**
     * Déconnexion
     */
    fun signOut(callback: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            // Supprimer les tokens locaux
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { clear() }
            
            Log.d(TAG, "Déconnexion terminée")
            callback()
        }
    }
    
    /**
     * Classe de données pour les tokens
     */
    data class TokenResponse(
        val accessToken: String,
        val refreshToken: String?,
        val expiresInSeconds: Int
    )
}