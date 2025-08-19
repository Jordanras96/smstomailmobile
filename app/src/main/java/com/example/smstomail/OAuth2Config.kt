package com.example.smstomail

/**
 * Configuration Google Identity Services pour l'application SMS to Mail
 * 
 * IMPORTANT: Utilise Google Identity Services avec OAuth 2.0
 * Configuration basée sur le projet: mythical-sky-448106-v4
 * 
 * Scopes requis:
 * - openid: Authentification de base
 * - email: Accès à l'adresse email du compte
 * - profile: Informations de profil utilisateur  
 * - gmail.send: Envoi d'emails via Gmail API
 */
object OAuth2Config {
    
    // Informations du projet Google Cloud
    const val PROJECT_ID = "mythical-sky-448106-v4"
    
    // Client ID Android OAuth 2.0 (existant et configuré)
    const val ANDROID_CLIENT_ID = "447857613313-itn45fqo3jqeh51r2o3lumnt5ihdvqrk.apps.googleusercontent.com"
    
    // Client ID Web pour server-side auth (✅ CONFIGURÉ)
    const val WEB_CLIENT_ID = "447857613313-0vjdrgksgn4391bb6r23jeg7cok4ac1q.apps.googleusercontent.com"
    
    // Endpoints Google OAuth2 (basés sur le projet configuré)
    const val AUTH_URI = "https://accounts.google.com/o/oauth2/auth"
    const val TOKEN_URI = "https://oauth2.googleapis.com/token"
    const val AUTH_PROVIDER_CERT_URL = "https://www.googleapis.com/oauth2/v1/certs"
    
    // Scopes OAuth 2.0 requis pour l'application (configurés dans Google Cloud Console)
    const val SCOPE_OPENID = "openid"
    const val SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email" 
    const val SCOPE_PROFILE = "https://www.googleapis.com/auth/userinfo.profile"
    const val SCOPE_GMAIL_SEND = "https://www.googleapis.com/auth/gmail.send"
    
    // Liste de tous les scopes requis (correspondent aux scopes cochés dans Google Cloud Console)
    val REQUIRED_SCOPES = listOf(
        SCOPE_OPENID,
        SCOPE_EMAIL, 
        SCOPE_PROFILE,
        SCOPE_GMAIL_SEND
    )
    
    // Chaîne de scopes pour Google Sign-In
    val SCOPES_STRING = REQUIRED_SCOPES.joinToString(" ")
    
    /**
     * Vérifie si la configuration OAuth 2.0 est complète
     */
    fun isConfigured(): Boolean {
        return ANDROID_CLIENT_ID.endsWith(".apps.googleusercontent.com") &&
               ANDROID_CLIENT_ID.isNotBlank() &&
               WEB_CLIENT_ID != "447857613313-CRÉER_CLIENT_WEB.apps.googleusercontent.com" &&
               WEB_CLIENT_ID.endsWith(".apps.googleusercontent.com") &&
               PROJECT_ID.isNotBlank()
    }
    
    /**
     * Affiche les informations de configuration pour debug
     */
    fun getConfigInfo(): String {
        return """
            CONFIGURATION SMS TO MAIL:
            - Projet: $PROJECT_ID
            - Client Android: ${ANDROID_CLIENT_ID.take(20)}...
            - Client Web: ${if (isWebClientConfigured()) "✅ Configuré" else "❌ À créer"}
            - Scopes: ${REQUIRED_SCOPES.size} configurés
        """.trimIndent()
    }
    
    /**
     * Vérifie si le client Web est configuré
     */
    fun isWebClientConfigured(): Boolean {
        return WEB_CLIENT_ID.contains("-0vjdrgksgn4391bb6r23jeg7cok4ac1q.")
    }
    
    /**
     * État de la configuration complète
     */
    const val CONFIGURATION_STATUS = """
        ✅ CONFIGURATION OAUTH 2.0 COMPLÈTE:
        
        ✅ Projet Google Cloud: mythical-sky-448106-v4
        ✅ API Gmail: Activée  
        ✅ Client Android: 447857613313-itn45fqo3jqeh51r2o3lumnt5ihdvqrk
        ✅ Client Web: 447857613313-0vjdrgksgn4391bb6r23jeg7cok4ac1q
        ✅ Scopes configurés: openid, email, profile, gmail.send
        ✅ Sites autorisés:
           - https://smstomail.vercel.app
           - https://jordanras96.github.io/smstomail/
        
        📱 L'application est prête pour l'authentification OAuth 2.0 !
        
        🔧 RECOMMANDATION: Désactivez le "schéma d'URI personnalisé" 
        dans Google Cloud Console car Google Identity Services ne l'utilise pas.
    """
}