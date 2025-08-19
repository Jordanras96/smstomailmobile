package com.example.smstomail

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestionnaire d'authentification Google utilisant Google Identity Services
 * Architecture Android standalone (sans serveur backend)
 */
class GoogleSignInManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleSignIn"
    }
    
    private val googleSignInClient: GoogleSignInClient
    
    init {
        // Configuration Google Sign-In pour Android standalone
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(OAuth2Config.SCOPE_GMAIL_SEND)
            )
            .build()
            
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        Log.d(TAG, "GoogleSignInManager initialisé pour Android standalone")
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
        
        Log.d(TAG, "État de connexion - Account: ${account != null}, Gmail scope: $hasGmailScope")
        return account != null && hasGmailScope
    }
    
    /**
     * Gère le résultat de la connexion Google (approche Android standalone)
     */
    fun handleSignInResult(callback: (Boolean, String?) -> Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        
        if (account == null) {
            Log.e(TAG, "Aucun compte Google connecté")
            callback(false, "Connexion Google échouée")
            return
        }
        
        Log.d(TAG, "Compte Google connecté: ${account.email}")
        
        // Vérifier les scopes accordés
        val hasGmailScope = account.grantedScopes?.contains(Scope(OAuth2Config.SCOPE_GMAIL_SEND)) == true
        
        if (!hasGmailScope) {
            Log.e(TAG, "Scope Gmail.send non accordé")
            callback(false, "Permissions Gmail requises non accordées")
            return
        }
        
        // Mettre à jour la configuration automatiquement
        val simpleConfig = SimpleConfig(context)
        val currentConfig = simpleConfig.getEmailConfig()
        
        // Sauvegarder l'email de l'utilisateur et marquer comme authentifié
        simpleConfig.saveEmailConfig(
            userEmail = account.email ?: "",
            recipientEmail = currentConfig.recipientEmail.ifBlank { account.email ?: "" }
        )
        simpleConfig.setGmailAuthenticated(true)
        
        Log.d(TAG, "Configuration email mise à jour automatiquement")
        Log.d(TAG, "Authentification Google réussie avec scope Gmail")
        callback(true, "Authentification Google réussie")
    }
    
    /**
     * Obtient un token d'accès valide directement depuis Google Sign-In
     */
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.e(TAG, "Aucun compte Google connecté")
                return@withContext null
            }
            
            // Utiliser GoogleAuthUtil pour obtenir le token d'accès
            val scopes = "oauth2:${OAuth2Config.SCOPE_GMAIL_SEND}"
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                context, 
                account.account!!, 
                scopes
            )
            
            Log.d(TAG, "Token d'accès obtenu via GoogleAuthUtil")
            return@withContext token
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'obtention du token", e)
            return@withContext null
        }
    }
    
    /**
     * Invalide le token d'accès actuel (force le refresh au prochain appel)
     */
    suspend fun invalidateToken(token: String) = withContext(Dispatchers.IO) {
        try {
            com.google.android.gms.auth.GoogleAuthUtil.invalidateToken(context, token)
            Log.d(TAG, "Token invalidé avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'invalidation du token", e)
        }
    }
    
    /**
     * Déconnecte l'utilisateur
     */
    fun signOut(callback: (Boolean) -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Nettoyer la configuration d'authentification
                val simpleConfig = SimpleConfig(context)
                simpleConfig.setGmailAuthenticated(false)
                
                Log.d(TAG, "Déconnexion Google réussie, configuration nettoyée")
                callback(true)
            } else {
                Log.e(TAG, "Erreur déconnexion Google", task.exception)
                callback(false)
            }
        }
    }
}