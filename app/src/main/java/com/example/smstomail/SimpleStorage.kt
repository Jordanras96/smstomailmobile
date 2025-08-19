package com.example.smstomail

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class SimpleStorage(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("sms_storage", Context.MODE_PRIVATE)
    
    data class SimpleSms(
        val id: String,
        val sender: String,
        val content: String,
        val timestamp: Long,
        val hash: String,
        val sent: Boolean = false
    )
    
    fun saveSms(sender: String, content: String): String? {
        val timestamp = System.currentTimeMillis()
        val hash = generateHash(sender, content, timestamp)
        
        // Vérifier doublon
        if (isDuplicate(hash)) {
            return null // SMS dupliqué
        }
        
        val id = "sms_$timestamp"
        val sms = SimpleSms(id, sender, content, timestamp, hash)
        
        val json = JSONObject().apply {
            put("id", sms.id)
            put("sender", sms.sender)
            put("content", sms.content)
            put("timestamp", sms.timestamp)
            put("hash", sms.hash)
            put("sent", sms.sent)
        }
        
        // Sauver dans SharedPreferences
        prefs.edit {
            putString(id, json.toString())
        }
        
        // Ajouter à la liste des SMS non envoyés
        addToPendingList(id)
        
        return id
    }
    
    fun getPendingSms(): List<SimpleSms> {
        val pendingIds = getPendingIds()
        val smsList = mutableListOf<SimpleSms>()
        
        pendingIds.forEach { id ->
            getSmsById(id)?.let { sms ->
                if (!sms.sent) {
                    smsList.add(sms)
                }
            }
        }
        
        return smsList
    }
    
    fun markAsSent(id: String): Boolean {
        return try {
            val sms = getSmsById(id) ?: return false
            val updatedSms = sms.copy(sent = true)
            
            val json = JSONObject().apply {
                put("id", updatedSms.id)
                put("sender", updatedSms.sender)
                put("content", updatedSms.content)
                put("timestamp", updatedSms.timestamp)
                put("hash", updatedSms.hash)
                put("sent", updatedSms.sent)
            }
            
            prefs.edit {
                putString(id, json.toString())
            }
            removeFromPendingList(id)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getSmsById(id: String): SimpleSms? {
        return try {
            val jsonStr = prefs.getString(id, null) ?: return null
            val json = JSONObject(jsonStr)
            
            SimpleSms(
                id = json.getString("id"),
                sender = json.getString("sender"),
                content = json.getString("content"),
                timestamp = json.getLong("timestamp"),
                hash = json.getString("hash"),
                sent = json.getBoolean("sent")
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateHash(sender: String, content: String, timestamp: Long): String {
        return try {
            val input = "$sender|$content|$timestamp"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "${sender.hashCode()}_${content.hashCode()}_$timestamp"
        }
    }
    
    private fun isDuplicate(hash: String): Boolean {
        val allKeys = prefs.all.keys
        allKeys.forEach { key ->
            if (key.startsWith("sms_")) {
                getSmsById(key)?.let { sms ->
                    if (sms.hash == hash) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    private fun getPendingIds(): List<String> {
        return try {
            val jsonStr = prefs.getString("pending_list", "[]")
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun addToPendingList(id: String) {
        try {
            val currentList = getPendingIds().toMutableList()
            if (!currentList.contains(id)) {
                currentList.add(id)
                val jsonArray = JSONArray(currentList)
                prefs.edit {
                    putString("pending_list", jsonArray.toString())
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun removeFromPendingList(id: String) {
        try {
            val currentList = getPendingIds().toMutableList()
            currentList.remove(id)
            val jsonArray = JSONArray(currentList)
            prefs.edit {
                putString("pending_list", jsonArray.toString())
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    fun getAllSms(): List<SimpleSms> {
        val allKeys = prefs.all.keys.filter { it.startsWith("sms_") }
        val smsList = mutableListOf<SimpleSms>()
        
        allKeys.forEach { key ->
            getSmsById(key)?.let { sms ->
                smsList.add(sms)
            }
        }
        
        return smsList.sortedByDescending { it.timestamp }
    }
    
    fun getStats(): String {
        val allKeys = prefs.all.keys.filter { it.startsWith("sms_") }
        val totalSms = allKeys.size
        val pendingSms = getPendingIds().size
        val sentSms = totalSms - pendingSms
        
        return "Total: $totalSms, Envoyés: $sentSms, En attente: $pendingSms"
    }
}