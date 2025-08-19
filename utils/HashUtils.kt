package com.example.smstomail.utils

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

object HashUtils {
    
    fun generateSmsHash(sender: String, content: String, timestamp: Long): String {
        return try {
            val input = "$sender|$content|$timestamp"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback simple en cas d'erreur
            "${sender.hashCode()}_${content.hashCode()}_$timestamp"
        }
    }
    
    fun generateRuleHash(name: String, pattern: String): String {
        return try {
            val input = "$name|$pattern"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }.take(16)
        } catch (e: Exception) {
            "${name.hashCode()}_${pattern.hashCode()}".replace("-", "")
        }
    }
    
    fun validateHash(hash: String): Boolean {
        return hash.isNotBlank() && hash.length >= 8
    }
}