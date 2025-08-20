package com.example.smstomail

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Activité de démarrage avec logo interne
 */
class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val SPLASH_DELAY = 2000L // 2 secondes
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Lancer l'activité principale après le délai
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, SimpleMainActivity::class.java))
            finish()
            
            // Animation de transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DELAY)
    }
    
    override fun onBackPressed() {
        // Désactiver le bouton retour sur l'écran de démarrage
        // Ne rien faire
    }
}