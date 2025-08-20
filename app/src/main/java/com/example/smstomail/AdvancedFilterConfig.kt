package com.example.smstomail

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.*

/**
 * Configuration avancée pour les filtres SMS avec keywords, libellés et regroupement
 */
class AdvancedFilterConfig(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("advanced_filters", Context.MODE_PRIVATE)
    
    /**
     * Règle de filtre avancée avec support des libellés et options
     */
    data class AdvancedFilterRule(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val keywords: List<String>,
        val label: String, // Sera utilisé comme libellé Gmail
        val recipientEmail: String,
        val enabled: Boolean = true,
        val autoForward: Boolean = false, // Transfert automatique des nouveaux messages
        val priority: Int = 0, // 0=Normal, 1=Haute, 2=Critique
        val color: String = "#2196F3", // Couleur pour l'UI
        val dateCreated: Long = System.currentTimeMillis()
    ) {
        /**
         * Vérifie si le message correspond à cette règle
         */
        fun matches(sender: String, content: String): Boolean {
            return enabled && keywords.any { keyword ->
                content.contains(keyword, ignoreCase = true) ||
                sender.contains(keyword, ignoreCase = true)
            }
        }
        
        /**
         * Génère l'objet email avec libellé
         */
        fun generateEmailSubject(smsCount: Int = 1): String {
            val prefix = when (priority) {
                1 -> "[URGENT] "
                2 -> "[CRITIQUE] "
                else -> ""
            }
            return "${prefix}[$label] ${if (smsCount > 1) "$smsCount SMS" else "SMS"} - ${name}"
        }
    }
    
    /**
     * Options de traitement automatique
     */
    data class AutoProcessingOptions(
        val enabled: Boolean = false,
        val batchInterval: Long = 300000, // 5 minutes en millisecondes
        val maxBatchSize: Int = 10,
        val onlyWhenConnected: Boolean = true,
        val groupByLabel: Boolean = true
    )
    
    /**
     * Filtre de date pour le traitement des SMS
     */
    data class DateFilter(
        val enabled: Boolean = false,
        val startDate: Long? = null,
        val endDate: Long? = null,
        val lastProcessedDate: Long = 0
    ) {
        fun isInRange(timestamp: Long): Boolean {
            if (!enabled) return true
            
            val start = startDate ?: 0
            val end = endDate ?: Long.MAX_VALUE
            
            return timestamp >= start && timestamp <= end
        }
    }
    
    /**
     * Sauvegarde les règles de filtre avancées
     */
    fun saveAdvancedRules(rules: List<AdvancedFilterRule>) {
        val jsonArray = org.json.JSONArray()
        
        rules.forEach { rule ->
            val keywordsArray = org.json.JSONArray()
            rule.keywords.forEach { keyword -> keywordsArray.put(keyword) }
            
            val jsonRule = org.json.JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("keywords", keywordsArray)
                put("label", rule.label)
                put("recipientEmail", rule.recipientEmail)
                put("enabled", rule.enabled)
                put("autoForward", rule.autoForward)
                put("priority", rule.priority)
                put("color", rule.color)
                put("dateCreated", rule.dateCreated)
            }
            jsonArray.put(jsonRule)
        }
        
        prefs.edit {
            putString("advanced_rules", jsonArray.toString())
        }
    }
    
    /**
     * Récupère les règles de filtre avancées
     */
    fun getAdvancedRules(): List<AdvancedFilterRule> {
        val rulesJson = prefs.getString("advanced_rules", "[]") ?: "[]"
        val jsonArray = org.json.JSONArray(rulesJson)
        val rules = mutableListOf<AdvancedFilterRule>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonRule = jsonArray.getJSONObject(i)
            
            val keywordsArray = jsonRule.getJSONArray("keywords")
            val keywords = mutableListOf<String>()
            for (j in 0 until keywordsArray.length()) {
                keywords.add(keywordsArray.getString(j))
            }
            
            rules.add(
                AdvancedFilterRule(
                    id = jsonRule.getString("id"),
                    name = jsonRule.getString("name"),
                    keywords = keywords,
                    label = jsonRule.getString("label"),
                    recipientEmail = jsonRule.getString("recipientEmail"),
                    enabled = jsonRule.getBoolean("enabled"),
                    autoForward = jsonRule.optBoolean("autoForward", false),
                    priority = jsonRule.optInt("priority", 0),
                    color = jsonRule.optString("color", "#2196F3"),
                    dateCreated = jsonRule.optLong("dateCreated", System.currentTimeMillis())
                )
            )
        }
        
        return rules.sortedBy { it.priority }.reversed() // Priorité haute en premier
    }
    
    /**
     * Trouve la première règle correspondante
     */
    fun findMatchingRule(sender: String, content: String): AdvancedFilterRule? {
        return getAdvancedRules().find { it.matches(sender, content) }
    }
    
    /**
     * Trouve toutes les règles correspondantes
     */
    fun findAllMatchingRules(sender: String, content: String): List<AdvancedFilterRule> {
        return getAdvancedRules().filter { it.matches(sender, content) }
    }
    
    /**
     * Groupe les SMS par libellé
     */
    fun groupSMSByLabel(smsList: List<SimpleSms>): Map<String, List<SimpleSms>> {
        val groupedSMS = mutableMapOf<String, MutableList<SimpleSms>>()
        
        smsList.forEach { sms ->
            val matchingRule = findMatchingRule(sms.sender, sms.content)
            val label = matchingRule?.label ?: "Non classé"
            
            if (!groupedSMS.containsKey(label)) {
                groupedSMS[label] = mutableListOf()
            }
            groupedSMS[label]?.add(sms)
        }
        
        return groupedSMS
    }
    
    /**
     * Sauvegarde les options de traitement automatique
     */
    fun saveAutoProcessingOptions(options: AutoProcessingOptions) {
        prefs.edit {
            putBoolean("auto_enabled", options.enabled)
            putLong("auto_batch_interval", options.batchInterval)
            putInt("auto_max_batch_size", options.maxBatchSize)
            putBoolean("auto_only_when_connected", options.onlyWhenConnected)
            putBoolean("auto_group_by_label", options.groupByLabel)
        }
    }
    
    /**
     * Récupère les options de traitement automatique
     */
    fun getAutoProcessingOptions(): AutoProcessingOptions {
        return AutoProcessingOptions(
            enabled = prefs.getBoolean("auto_enabled", false),
            batchInterval = prefs.getLong("auto_batch_interval", 300000),
            maxBatchSize = prefs.getInt("auto_max_batch_size", 10),
            onlyWhenConnected = prefs.getBoolean("auto_only_when_connected", true),
            groupByLabel = prefs.getBoolean("auto_group_by_label", true)
        )
    }
    
    /**
     * Sauvegarde le filtre de date
     */
    fun saveDateFilter(filter: DateFilter) {
        prefs.edit {
            putBoolean("date_filter_enabled", filter.enabled)
            if (filter.startDate != null) putLong("date_filter_start", filter.startDate)
            if (filter.endDate != null) putLong("date_filter_end", filter.endDate)
            putLong("date_last_processed", filter.lastProcessedDate)
        }
    }
    
    /**
     * Récupère le filtre de date
     */
    fun getDateFilter(): DateFilter {
        val startDate = prefs.getLong("date_filter_start", -1)
        val endDate = prefs.getLong("date_filter_end", -1)
        
        return DateFilter(
            enabled = prefs.getBoolean("date_filter_enabled", false),
            startDate = if (startDate != -1L) startDate else null,
            endDate = if (endDate != -1L) endDate else null,
            lastProcessedDate = prefs.getLong("date_last_processed", 0)
        )
    }
    
    /**
     * Règles par défaut avec libellés
     */
    fun getDefaultAdvancedRules(userEmail: String): List<AdvancedFilterRule> {
        return listOf(
            AdvancedFilterRule(
                name = "Codes de vérification",
                keywords = listOf("code", "verification", "authentification", "otp", "pin"),
                label = "Sécurité",
                recipientEmail = userEmail,
                autoForward = true,
                priority = 2,
                color = "#F44336"
            ),
            AdvancedFilterRule(
                name = "Banque et Finance",
                keywords = listOf("banque", "carte", "virement", "solde", "transaction", "paypal"),
                label = "Finance",
                recipientEmail = userEmail,
                autoForward = true,
                priority = 1,
                color = "#4CAF50"
            ),
            AdvancedFilterRule(
                name = "Livraisons",
                keywords = listOf("livraison", "colis", "expédié", "dhl", "fedex", "colissimo"),
                label = "Livraisons",
                recipientEmail = userEmail,
                autoForward = false,
                priority = 0,
                color = "#FF9800"
            ),
            AdvancedFilterRule(
                name = "Factures",
                keywords = listOf("facture", "échéance", "paiement", "mensualité"),
                label = "Factures",
                recipientEmail = userEmail,
                autoForward = false,
                priority = 0,
                color = "#9C27B0"
            ),
            AdvancedFilterRule(
                name = "Promotion",
                keywords = listOf("promo", "offre", "réduction", "soldes", "gratuit"),
                label = "Marketing",
                recipientEmail = userEmail,
                autoForward = false,
                priority = 0,
                color = "#607D8B"
            )
        )
    }
    
    /**
     * Initialise les règles par défaut si aucune n'existe
     */
    fun initializeDefaultRules(userEmail: String) {
        if (getAdvancedRules().isEmpty()) {
            saveAdvancedRules(getDefaultAdvancedRules(userEmail))
        }
    }
    
    /**
     * Met à jour la date du dernier traitement
     */
    fun updateLastProcessedDate() {
        val currentFilter = getDateFilter()
        saveDateFilter(currentFilter.copy(lastProcessedDate = System.currentTimeMillis()))
    }
    
    /**
     * Formate une date pour l'affichage
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}