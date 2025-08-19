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
    
    // Client ID Web pour server-side auth (DOIT ÊTRE CRÉÉ dans Google Cloud Console)
    // Instructions: Créez un nouveau "Web application" client dans les Credentials
    const val WEB_CLIENT_ID = "447857613313-CRÉER_CLIENT_WEB.apps.googleusercontent.com"
    
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
        return WEB_CLIENT_ID != "447857613313-CRÉER_CLIENT_WEB.apps.googleusercontent.com"
    }
    
    /**
     * Instructions pour créer le client Web manquant
     */
    const val WEB_CLIENT_SETUP_INSTRUCTIONS = """
        ⚠️  CLIENT WEB REQUIS - ÉTAPES À SUIVRE:
        
        1. Allez sur: https://console.cloud.google.com/apis/credentials?project=mythical-sky-448106-v4
        2. Cliquez "Create Credentials" > "OAuth 2.0 Client ID"  
        3. Sélectionnez "Web application"
        4. Nom: "SMS to Mail Web Client"
        5. Authorized JavaScript origins:
           - https://smstomail.vercel.app
           - https://jordanras96.github.io
        6. Authorized redirect URIs:
           - https://smstomail.vercel.app/
           - https://jordanras96.github.io/smstomail/
        7. Copiez le Client ID généré 
        8. Remplacez WEB_CLIENT_ID dans OAuth2Config.kt
        
        ✅ CONFIGURATION ACTUELLE:
        - ✅ Projet: mythical-sky-448106-v4 
        - ✅ Client Android: Configuré
        - ✅ API Gmail: Activée
        - ❌ Client Web: À créer
    """
}