package com.example.smstomail

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.security.MessageDigest

/**
 * Utilitaire pour déboguer la configuration OAuth 2.0
 */
object OAuth2DebugHelper {
    
    private const val TAG = "OAuth2Debug"
    
    /**
     * Affiche les informations de configuration OAuth pour le débogage
     */
    fun logConfigurationInfo(context: Context) {
        Log.d(TAG, "=== Configuration OAuth 2.0 ===")
        Log.d(TAG, "Android Client ID: ${OAuth2Config.ANDROID_CLIENT_ID}")
        Log.d(TAG, "Web Client ID: ${OAuth2Config.WEB_CLIENT_ID}")
        Log.d(TAG, "Configuration valide: ${OAuth2Config.isConfigured()}")
        
        // Informations sur l'application
        Log.d(TAG, "Package name: ${context.packageName}")
        Log.d(TAG, "SHA-1 Debug: ${getDebugSHA1(context)}")
        
        // Vérification des permissions
        logPermissions(context)
    }
    
    /**
     * Obtient l'empreinte SHA-1 pour la clé de debug
     */
    private fun getDebugSHA1(context: Context): String {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            signatures?.let { sigs ->
                val md = MessageDigest.getInstance("SHA1")
                md.update(sigs[0].toByteArray())
                md.digest().joinToString(":") { "%02X".format(it) }
            } ?: "Non disponible"
            
        } catch (e: Exception) {
            Log.w(TAG, "Erreur lors du calcul SHA-1", e)
            "Erreur: ${e.message}"
        }
    }
    
    /**
     * Vérifie les permissions nécessaires
     */
    private fun logPermissions(context: Context) {
        val requiredPermissions = arrayOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        )
        
        Log.d(TAG, "=== Permissions ===")
        for (permission in requiredPermissions) {
            val granted = context.checkCallingOrSelfPermission(permission) == 
                PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "$permission: ${if (granted) "ACCORDÉE" else "REFUSÉE"}")
        }
    }
    
    /**
     * Génère des instructions de configuration spécifiques à cette installation
     */
    fun generateSetupInstructions(context: Context): String {
        val sha1 = getDebugSHA1(context)
        
        return """
        === Instructions de configuration OAuth ===
        
        1. Google Cloud Console > APIs & Services > Credentials
        2. Sélectionnez votre client OAuth Android
        3. Vérifiez la configuration :
           - Package name: ${context.packageName}
           - SHA-1 certificate fingerprint: $sha1
           - Authorized redirect URIs: N/A (Google Identity Services)
        
        4. OAuth consent screen :
           - Ajoutez votre email comme utilisateur de test
           - Gardez le statut "Testing" pour le développement
        
        5. APIs & Services > Library :
           - Vérifiez que Gmail API est activée
        """.trimIndent()
    }
}