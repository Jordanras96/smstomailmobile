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
        private const val SPLASH_DELAY = 1500L // 1.5 secondes
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Lancer l'activité principale après le délai
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(this, SimpleMainActivity::class.java)
                startActivity(intent)
                finish()
                
                // Animation de transition
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (e: Exception) {
                e.printStackTrace()
                // En cas d'erreur, fermer l'app proprement
                finish()
            }
        }, SPLASH_DELAY)
    }
    
    override fun onBackPressed() {
        // Désactiver le bouton retour sur l'écran de démarrage
        // Ne rien faire
    }
}