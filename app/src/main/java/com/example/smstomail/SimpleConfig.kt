package com.example.smstomail

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SimpleConfig(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    
    data class EmailConfig(
        val userEmail: String = "",
        val isGmailAuthenticated: Boolean = false,
        val recipientEmail: String = ""
    ) {
        fun isValid(): Boolean {
            return userEmail.isNotBlank() && 
                   recipientEmail.isNotBlank() && 
                   isGmailAuthenticated
        }
    }
    
    fun saveEmailConfig(userEmail: String, recipientEmail: String) {
        prefs.edit {
            putString("user_email", userEmail)
            putString("recipient_email", recipientEmail)
        }
    }
    
    fun setGmailAuthenticated(authenticated: Boolean) {
        prefs.edit {
            putBoolean("gmail_authenticated", authenticated)
        }
    }
    
    fun getEmailConfig(): EmailConfig {
        return EmailConfig(
            userEmail = prefs.getString("user_email", "") ?: "",
            recipientEmail = prefs.getString("recipient_email", "") ?: "",
            isGmailAuthenticated = prefs.getBoolean("gmail_authenticated", false)
        )
    }
    
    // Règles simples de filtrage
    data class FilterRule(
        val name: String,
        val keyword: String,
        val recipientEmail: String,
        val enabled: Boolean = true
    )
    
    fun saveFilterRules(rules: List<FilterRule>) {
        val jsonArray = org.json.JSONArray()
        
        rules.forEach { rule ->
            val jsonRule = org.json.JSONObject().apply {
                put("name", rule.name)
                put("keyword", rule.keyword)
                put("recipientEmail", rule.recipientEmail)
                put("enabled", rule.enabled)
            }
            jsonArray.put(jsonRule)
        }
        
        prefs.edit {
            putString("filter_rules", jsonArray.toString())
        }
    }
    
    fun getFilterRules(): List<FilterRule> {
        val rulesJson = prefs.getString("filter_rules", "[]") ?: "[]"
        val jsonArray = org.json.JSONArray(rulesJson)
        val rules = mutableListOf<FilterRule>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonRule = jsonArray.getJSONObject(i)
            rules.add(
                FilterRule(
                    name = jsonRule.getString("name"),
                    keyword = jsonRule.getString("keyword"),
                    recipientEmail = jsonRule.getString("recipientEmail"),
                    enabled = jsonRule.getBoolean("enabled")
                )
            )
        }
        
        return rules
    }
    
    fun findMatchingRule(sender: String, content: String): FilterRule? {
        val rules = getFilterRules().filter { it.enabled }
        
        return rules.find { rule ->
            content.contains(rule.keyword, ignoreCase = true) ||
            sender.contains(rule.keyword, ignoreCase = true)
        }
    }
    
    fun getDefaultRules(): List<FilterRule> {
        return listOf(
            FilterRule(
                name = "Factures",
                keyword = "facture",
                recipientEmail = getEmailConfig().recipientEmail
            ),
            FilterRule(
                name = "Banque", 
                keyword = "banque",
                recipientEmail = getEmailConfig().recipientEmail
            ),
            FilterRule(
                name = "Codes",
                keyword = "code",
                recipientEmail = getEmailConfig().recipientEmail
            )
        )
    }
    
    fun initializeDefaultRules() {
        if (getFilterRules().isEmpty()) {
            saveFilterRules(getDefaultRules())
        }
    }
}