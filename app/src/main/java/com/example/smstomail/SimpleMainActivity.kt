package com.example.smstomail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class SimpleMainActivity : AppCompatActivity() {
    
    private lateinit var statusTextView: TextView
    private lateinit var smsCountTextView: TextView
    private lateinit var recipientEditText: EditText
    private lateinit var buttonRequestPermissions: Button
    private lateinit var buttonGmailAuth: Button
    private lateinit var buttonSaveConfig: Button
    private lateinit var buttonTestSms: Button
    
    private lateinit var simpleConfig: SimpleConfig
    private lateinit var simpleStorage: SimpleStorage
    private lateinit var googleSignInManager: GoogleSignInManager
    private lateinit var googleGmailSender: GoogleGmailSender
    
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Permissions requises
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.GET_ACCOUNTS
    )
    
    // Request permissions launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updateStatus()
    }
    
    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("SMS2Gmail", "Google Sign-In Result Code: ${result.resultCode}")
        
        if (result.resultCode == RESULT_OK && result.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                
                android.util.Log.d("SMS2Gmail", "Google Sign-In réussi pour: ${account?.email}")
                
                // Vérifier l'authentification avec la nouvelle approche Android standalone
                googleSignInManager.handleSignInResult { success, message ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Gmail connecté avec succès!", Toast.LENGTH_SHORT).show()
                            updateStatus()
                        } else {
                            Toast.makeText(this, message ?: "Erreur authentification", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
            } catch (e: ApiException) {
                android.util.Log.e("SMS2Gmail", "Erreur Google Sign-In: ${e.statusCode}", e)
                Toast.makeText(this, "Erreur d'authentification: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Authentification annulée", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getDebugKeystoreSHA1() {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }
            
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            signatures?.forEach { signature ->
                val md = java.security.MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                val sha1 = md.digest().joinToString(":") { "%02X".format(it) }
                android.util.Log.d("SMS2Gmail", "SHA-1 Debug Keystore: $sha1")
                
                // Copier dans le presse-papiers pour faciliter la config
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("SHA-1", sha1)
                clipboard.setPrimaryClip(clip)
                
                Toast.makeText(this, "SHA-1 copié: $sha1", Toast.LENGTH_LONG).show()
                return // Prendre seulement le premier
            }
        } catch (e: Exception) {
            android.util.Log.e("SMS2Gmail", "Erreur lors calcul SHA-1", e)
            Toast.makeText(this, "Erreur calcul SHA-1. Utilisez: keytool -keystore ~/.android/debug.keystore -list -v", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initServices()
        setupClickListeners()
        
        // Debug Google Identity Services configuration
        // OAuth2DebugHelper.logConfigurationInfo(this) // Disabled for now
        
        updateStatus()
    }
    
    // OAuth2 callback handling removed - Google Identity Services handles this automatically
    
    
    private fun initViews() {
        statusTextView = findViewById(R.id.statusTextView)
        smsCountTextView = findViewById(R.id.textViewSmsCount)
        recipientEditText = findViewById(R.id.editTextRecipient)
        buttonRequestPermissions = findViewById(R.id.buttonRequestPermissions)
        buttonGmailAuth = findViewById(R.id.buttonGmailAuth)
        buttonSaveConfig = findViewById(R.id.buttonSaveConfig)
        buttonTestSms = findViewById(R.id.buttonTestSms)
    }
    
    private fun initServices() {
        simpleConfig = SimpleConfig(this)
        simpleStorage = SimpleStorage(this)
        googleSignInManager = GoogleSignInManager(this)
        googleGmailSender = GoogleGmailSender(this, googleSignInManager)
        
        // Initialiser les règles par défaut
        simpleConfig.initializeDefaultRules()
        
        // Charger la config existante
        val emailConfig = simpleConfig.getEmailConfig()
        recipientEditText.setText(emailConfig.recipientEmail)
    }
    
    private fun setupClickListeners() {
        buttonRequestPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }
        
        buttonGmailAuth.setOnClickListener {
            connectToGmail()
        }
        
        buttonSaveConfig.setOnClickListener {
            saveConfiguration()
        }
        
        buttonTestSms.setOnClickListener {
            testEmailSending()
        }
        
        statusTextView.setOnClickListener {
            updateStatus()
        }
    }
    
    private fun showGmailSetupInstructions() {
        val instructions = """
            ⚠️ Configuration Gmail API requise:
            
            1. Aller sur Google Cloud Console
            2. Créer/sélectionner un projet
            3. Activer Gmail API
            4. Créer des identifiants OAuth 2.0
            5. Configurer l'écran de consentement
            6. Ajouter le SHA-1 de votre APK
            
            Pour plus d'infos: developers.google.com/gmail/api
        """.trimIndent()
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Configuration Gmail API")
            .setMessage(instructions)
            .setPositiveButton("OK") { _, _ -> }
            .setNegativeButton("Obtenir SHA-1") { _, _ -> getDebugKeystoreSHA1() }
            .setNeutralButton("Réessayer") { _, _ -> connectToGmail() }
            .show()
    }
    
    private fun connectToGmail() {
        android.util.Log.d("SMS2Gmail", "Tentative de connexion Google Sign-In Gmail")
        
        // Vérifier la configuration
        if (!OAuth2Config.isConfigured()) {
            Toast.makeText(this, "Configuration Google manquante - vérifiez OAuth2Config", Toast.LENGTH_LONG).show()
            return
        }
        
        // Vérifier si déjà connecté
        if (googleSignInManager.isSignedIn()) {
            android.util.Log.d("SMS2Gmail", "Déjà authentifié avec Google")
            Toast.makeText(this, "Déjà connecté à Gmail", Toast.LENGTH_SHORT).show()
            updateStatus()
            return
        }
        
        try {
            // Lancer Google Sign-In
            val signInIntent = googleSignInManager.getSignInClient().signInIntent
            googleSignInLauncher.launch(signInIntent)
            
        } catch (e: Exception) {
            android.util.Log.e("SMS2Gmail", "Erreur lors du lancement Google Sign-In", e)
            Toast.makeText(this, "Erreur Google Sign-In: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Toast.makeText(this, "Toutes les permissions sont accordées", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveConfiguration() {
        val recipientEmail = recipientEditText.text.toString().trim()
        
        if (recipientEmail.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
            // Utiliser OAuth2 pour obtenir l'email utilisateur
            val userEmail = "oauth2.user@gmail.com" // Placeholder - sera obtenu via OAuth2
            
            simpleConfig.saveEmailConfig(userEmail, recipientEmail)
            Toast.makeText(this, "Configuration sauvegardée", Toast.LENGTH_SHORT).show()
            updateStatus()
        } else {
            Toast.makeText(this, "Email destinataire invalide", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testEmailSending() {
        val emailConfig = simpleConfig.getEmailConfig()
        
        if (!emailConfig.isValid()) {
            Toast.makeText(this, "Configuration email incomplète", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!googleSignInManager.isSignedIn()) {
            Toast.makeText(this, "Gmail non authentifié", Toast.LENGTH_SHORT).show()
            return
        }
        
        activityScope.launch {
            try {
                val success = googleGmailSender.sendEmail(
                    subject = "[TEST] SMS to Mail App Google Identity",
                    body = "Ceci est un test d'envoi depuis l'application SMS to Mail avec Google Identity Services.\n\nSi vous recevez ce message, l'application fonctionne correctement!",
                    recipientEmail = emailConfig.recipientEmail
                )
                
                if (success) {
                    Toast.makeText(this@SimpleMainActivity, "Email de test envoyé avec succès!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@SimpleMainActivity, "Erreur lors de l'envoi de l'email de test", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SimpleMainActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateStatus() {
        val permissionsGranted = areAllPermissionsGranted()
        val gmailAuthenticated = googleSignInManager.isSignedIn()
        val emailConfig = simpleConfig.getEmailConfig()
        val smsCount = simpleStorage.getAllSms().size
        
        val status = when {
            !permissionsGranted -> getString(R.string.status_permissions_missing)
            !gmailAuthenticated -> getString(R.string.status_gmail_not_connected)
            !emailConfig.isValid() -> getString(R.string.status_email_config_incomplete)
            else -> getString(R.string.status_app_ready)
        }
        
        statusTextView.text = getString(R.string.status_template, status)
        smsCountTextView.text = getString(R.string.sms_count_template, smsCount)
        
        // Mettre à jour les boutons selon l'état
        buttonGmailAuth.isEnabled = permissionsGranted
        buttonGmailAuth.text = if (gmailAuthenticated) {
            getString(R.string.gmail_connected_oauth2)
        } else {
            getString(R.string.connect_to_gmail)
        }
        
        buttonSaveConfig.isEnabled = gmailAuthenticated
        buttonTestSms.isEnabled = emailConfig.isValid() && gmailAuthenticated
        
        // Debug: Log de l'état
        android.util.Log.d("SMS2Gmail", "Permissions: $permissionsGranted, Gmail OAuth2: $gmailAuthenticated")
    }
    
    private fun areAllPermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}