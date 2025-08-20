package com.example.smstomail

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Processeur avancé de SMS avec support des filtres, libellés et regroupement
 */
class AdvancedSmsProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "AdvancedSmsProcessor"
    }
    
    private val filterConfig = AdvancedFilterConfig(context)
    private val googleSignInManager = GoogleSignInManager(context)
    private val gmailSender = GoogleGmailSender(context, googleSignInManager)
    private val storage = SimpleStorage(context)
    
    /**
     * Résultat du traitement des SMS
     */
    data class ProcessingResult(
        val totalProcessed: Int,
        val emailsSent: Int,
        val errors: List<String>,
        val groupedByLabel: Map<String, Int>
    )
    
    /**
     * Traite les SMS selon les filtres et les regroupe par libellé
     */
    suspend fun processSMSWithFilters(
        dateFilter: AdvancedFilterConfig.DateFilter? = null,
        forceAll: Boolean = false
    ): ProcessingResult = withContext(Dispatchers.IO) {
        
        val allSMS = storage.getAllSms()
        val filter = dateFilter ?: filterConfig.getDateFilter()
        val autoOptions = filterConfig.getAutoProcessingOptions()
        
        Log.d(TAG, "Début traitement - Total SMS: ${allSMS.size}")
        
        // Filtrer par date si activé
        val filteredSMS = if (filter.enabled && !forceAll) {
            allSMS.filter { sms ->
                filter.isInRange(sms.timestamp) && 
                (forceAll || sms.timestamp > filter.lastProcessedDate)
            }
        } else {
            allSMS
        }
        
        Log.d(TAG, "SMS après filtre de date: ${filteredSMS.size}")
        
        // Grouper par libellé
        val groupedSMS = filterConfig.groupSMSByLabel(filteredSMS)
        val emailsSent = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        Log.d(TAG, "Groupes créés: ${groupedSMS.keys}")
        
        // Traiter chaque groupe
        for ((label, smsGroup) in groupedSMS) {
            try {
                if (smsGroup.isEmpty()) continue
                
                val matchingRule = filterConfig.findMatchingRule(
                    smsGroup.first().sender, 
                    smsGroup.first().content
                )
                
                // Vérifier si on doit traiter ce groupe
                if (matchingRule != null && (!autoOptions.enabled || matchingRule.autoForward || forceAll)) {
                    
                    if (autoOptions.groupByLabel && smsGroup.size > 1) {
                        // Envoyer en groupe
                        val success = sendGroupedEmail(matchingRule, smsGroup)
                        if (success) {
                            emailsSent.add("$label (${smsGroup.size} SMS)")
                            Log.d(TAG, "Email groupé envoyé pour $label: ${smsGroup.size} SMS")
                        } else {
                            errors.add("Erreur envoi groupé pour $label")
                        }
                    } else {
                        // Envoyer individuellement
                        var groupSuccess = 0
                        for (sms in smsGroup.take(autoOptions.maxBatchSize)) {
                            val success = sendIndividualEmail(matchingRule, sms)
                            if (success) {
                                groupSuccess++
                            } else {
                                errors.add("Erreur envoi SMS de $label")
                            }
                        }
                        if (groupSuccess > 0) {
                            emailsSent.add("$label ($groupSuccess SMS individuels)")
                            Log.d(TAG, "Emails individuels envoyés pour $label: $groupSuccess")
                        }
                    }
                } else {
                    Log.d(TAG, "Groupe $label ignoré (auto-forward: ${matchingRule?.autoForward}, forceAll: $forceAll)")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Erreur traitement groupe $label", e)
                errors.add("Erreur traitement $label: ${e.message}")
            }
        }
        
        // Mettre à jour la date de dernier traitement
        if (!forceAll) {
            filterConfig.updateLastProcessedDate()
        }
        
        val result = ProcessingResult(
            totalProcessed = filteredSMS.size,
            emailsSent = emailsSent.size,
            errors = errors,
            groupedByLabel = groupedSMS.mapValues { it.value.size }
        )
        
        Log.d(TAG, "Traitement terminé: $result")
        result
    }
    
    /**
     * Envoie un email groupé avec plusieurs SMS
     */
    private suspend fun sendGroupedEmail(
        rule: AdvancedFilterConfig.AdvancedFilterRule,
        smsGroup: List<SimpleSms>
    ): Boolean = withContext(Dispatchers.IO) {
        
        try {
            val subject = rule.generateEmailSubject(smsGroup.size)
            val body = buildGroupedEmailBody(rule, smsGroup)
            
            return@withContext gmailSender.sendEmail(
                subject = subject,
                body = body,
                recipientEmail = rule.recipientEmail
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur envoi email groupé", e)
            return@withContext false
        }
    }
    
    /**
     * Envoie un email pour un SMS individuel
     */
    private suspend fun sendIndividualEmail(
        rule: AdvancedFilterConfig.AdvancedFilterRule,
        sms: SimpleSms
    ): Boolean = withContext(Dispatchers.IO) {
        
        try {
            val subject = rule.generateEmailSubject(1)
            val body = buildIndividualEmailBody(rule, sms)
            
            return@withContext gmailSender.sendEmail(
                subject = subject,
                body = body,
                recipientEmail = rule.recipientEmail
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur envoi email individuel", e)
            return@withContext false
        }
    }
    
    /**
     * Construit le corps d'email pour un groupe de SMS
     */
    private fun buildGroupedEmailBody(
        rule: AdvancedFilterConfig.AdvancedFilterRule,
        smsGroup: List<SimpleSms>
    ): String {
        val sortedSMS = smsGroup.sortedByDescending { it.timestamp }
        
        return buildString {
            append("📱 Groupe de SMS - Libellé: ${rule.label}\n")
            append("🏷️ Règle: ${rule.name}\n")
            append("📊 Nombre de messages: ${smsGroup.size}\n")
            append("🔑 Mots-clés: ${rule.keywords.joinToString(", ")}\n\n")
            
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
            
            sortedSMS.forEachIndexed { index, sms ->
                append("📧 Message ${index + 1}/${smsGroup.size}\n")
                append("👤 Expéditeur: ${sms.sender}\n")
                append("🕐 Date: ${filterConfig.formatDate(sms.timestamp)}\n")
                append("💬 Contenu:\n${sms.content}\n\n")
                
                if (index < sortedSMS.size - 1) {
                    append("────────────────────────────────────────\n\n")
                }
            }
            
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("🤖 Envoyé automatiquement par SMS to Mail\n")
            append("📱 Application: ${context.packageName}\n")
            append("⏰ Traité le: ${filterConfig.formatDate(System.currentTimeMillis())}")
        }
    }
    
    /**
     * Construit le corps d'email pour un SMS individuel
     */
    private fun buildIndividualEmailBody(
        rule: AdvancedFilterConfig.AdvancedFilterRule,
        sms: SimpleSms
    ): String {
        return buildString {
            append("📱 SMS - Libellé: ${rule.label}\n")
            append("🏷️ Règle: ${rule.name}\n")
            append("🔑 Mots-clés correspondants: ${rule.keywords.joinToString(", ")}\n\n")
            
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
            
            append("👤 Expéditeur: ${sms.sender}\n")
            append("🕐 Date: ${filterConfig.formatDate(sms.timestamp)}\n")
            append("💬 Contenu:\n${sms.content}\n\n")
            
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("🤖 Envoyé automatiquement par SMS to Mail\n")
            append("📱 Application: ${context.packageName}\n")
            append("⏰ Traité le: ${filterConfig.formatDate(System.currentTimeMillis())}")
        }
    }
    
    /**
     * Traite automatiquement les nouveaux SMS selon les règles
     */
    suspend fun processNewSMS(sender: String, content: String, timestamp: Long): Boolean = withContext(Dispatchers.IO) {
        
        val autoOptions = filterConfig.getAutoProcessingOptions()
        if (!autoOptions.enabled) {
            Log.d(TAG, "Traitement automatique désactivé")
            return@withContext false
        }
        
        val matchingRules = filterConfig.findAllMatchingRules(sender, content)
        val autoForwardRules = matchingRules.filter { it.autoForward }
        
        if (autoForwardRules.isEmpty()) {
            Log.d(TAG, "Aucune règle auto-forward trouvée pour ce SMS")
            return@withContext false
        }
        
        // Utiliser la règle de plus haute priorité
        val rule = autoForwardRules.maxByOrNull { it.priority }!!
        
        val sms = SimpleSms(
            id = System.currentTimeMillis().toString(),
            sender = sender,
            content = content,
            timestamp = timestamp
        )
        
        try {
            val success = sendIndividualEmail(rule, sms)
            if (success) {
                Log.d(TAG, "SMS auto-transféré avec succès - Libellé: ${rule.label}")
            }
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Erreur auto-transfert SMS", e)
            return@withContext false
        }
    }
    
    /**
     * Obtient les statistiques des filtres
     */
    fun getFilterStatistics(): Map<String, Any> {
        val allSMS = storage.getAllSms()
        val groupedSMS = filterConfig.groupSMSByLabel(allSMS)
        val rules = filterConfig.getAdvancedRules()
        
        return mapOf(
            "totalSMS" to allSMS.size,
            "totalRules" to rules.size,
            "activeRules" to rules.count { it.enabled },
            "autoForwardRules" to rules.count { it.autoForward },
            "groupedLabels" to groupedSMS.keys.size,
            "labelDistribution" to groupedSMS.mapValues { it.value.size },
            "lastProcessed" to filterConfig.getDateFilter().lastProcessedDate
        )
    }
}