package com.example.smstomail

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activité de gestion des filtres avancés SMS
 */
class AdvancedFiltersActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AdvancedFilters"
    }
    
    private lateinit var filterConfig: AdvancedFilterConfig
    private lateinit var smsProcessor: AdvancedSmsProcessor
    private lateinit var rulesAdapter: FilterRulesAdapter
    
    // Views
    private lateinit var switchAutoProcessing: SwitchMaterial
    private lateinit var switchGroupByLabel: SwitchMaterial
    private lateinit var editTextBatchSize: TextInputEditText
    private lateinit var switchDateFilter: SwitchMaterial
    private lateinit var buttonStartDate: Button
    private lateinit var buttonEndDate: Button
    private lateinit var recyclerViewRules: RecyclerView
    private lateinit var fabAddRule: FloatingActionButton
    private lateinit var buttonProcessSMS: Button
    private lateinit var buttonProcessFiltered: Button
    private lateinit var buttonViewStatistics: Button
    
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Date filters
    private var startDateTimestamp: Long? = null
    private var endDateTimestamp: Long? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_filters)
        
        initComponents()
        initViews()
        setupClickListeners()
        loadConfiguration()
        updateUI()
    }
    
    private fun initComponents() {
        filterConfig = AdvancedFilterConfig(this)
        smsProcessor = AdvancedSmsProcessor(this)
        
        // Initialiser avec les règles par défaut si nécessaire
        val simpleConfig = SimpleConfig(this)
        val emailConfig = simpleConfig.getEmailConfig()
        if (emailConfig.userEmail.isNotBlank()) {
            filterConfig.initializeDefaultRules(emailConfig.userEmail)
        }
    }
    
    private fun initViews() {
        switchAutoProcessing = findViewById(R.id.switchAutoProcessing)
        switchGroupByLabel = findViewById(R.id.switchGroupByLabel)
        editTextBatchSize = findViewById(R.id.editTextBatchSize)
        switchDateFilter = findViewById(R.id.switchDateFilter)
        buttonStartDate = findViewById(R.id.buttonStartDate)
        buttonEndDate = findViewById(R.id.buttonEndDate)
        recyclerViewRules = findViewById(R.id.recyclerViewRules)
        fabAddRule = findViewById(R.id.fabAddRule)
        buttonProcessSMS = findViewById(R.id.buttonProcessSMS)
        buttonProcessFiltered = findViewById(R.id.buttonProcessFiltered)
        buttonViewStatistics = findViewById(R.id.buttonViewStatistics)
        
        // Setup RecyclerView
        rulesAdapter = FilterRulesAdapter { rule, action ->
            when (action) {
                FilterRulesAdapter.Action.EDIT -> editRule(rule)
                FilterRulesAdapter.Action.DELETE -> deleteRule(rule)
                FilterRulesAdapter.Action.TOGGLE -> toggleRule(rule)
            }
        }
        
        recyclerViewRules.layoutManager = LinearLayoutManager(this)
        recyclerViewRules.adapter = rulesAdapter
    }
    
    private fun setupClickListeners() {
        fabAddRule.setOnClickListener { showAddRuleDialog() }
        buttonStartDate.setOnClickListener { showDatePicker(true) }
        buttonEndDate.setOnClickListener { showDatePicker(false) }
        
        buttonProcessSMS.setOnClickListener { processAllSMS() }
        buttonProcessFiltered.setOnClickListener { processFilteredSMS() }
        buttonViewStatistics.setOnClickListener { showStatistics() }
        
        // Auto-save when switches change
        switchAutoProcessing.setOnCheckedChangeListener { _, _ -> saveAutoProcessingOptions() }
        switchGroupByLabel.setOnCheckedChangeListener { _, _ -> saveAutoProcessingOptions() }
        switchDateFilter.setOnCheckedChangeListener { _, _ -> saveDateFilter() }
    }
    
    private fun loadConfiguration() {
        // Load auto-processing options
        val autoOptions = filterConfig.getAutoProcessingOptions()
        switchAutoProcessing.isChecked = autoOptions.enabled
        switchGroupByLabel.isChecked = autoOptions.groupByLabel
        editTextBatchSize.setText(autoOptions.maxBatchSize.toString())
        
        // Load date filter
        val dateFilter = filterConfig.getDateFilter()
        switchDateFilter.isChecked = dateFilter.enabled
        startDateTimestamp = dateFilter.startDate
        endDateTimestamp = dateFilter.endDate
        
        updateDateButtons()
        
        // Load rules
        rulesAdapter.updateRules(filterConfig.getAdvancedRules())
    }
    
    private fun updateUI() {
        // Update button states based on configuration
        val simpleConfig = SimpleConfig(this)
        val emailConfig = simpleConfig.getEmailConfig()
        val isConfigured = emailConfig.isValid()
        
        buttonProcessSMS.isEnabled = isConfigured
        buttonProcessFiltered.isEnabled = isConfigured
        
        Log.d(TAG, "UI mise à jour - Configuration valide: $isConfigured")
    }
    
    private fun showAddRuleDialog(rule: AdvancedFilterConfig.AdvancedFilterRule? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_rule, null)
        
        val editTextRuleName = dialogView.findViewById<TextInputEditText>(R.id.editTextRuleName)
        val editTextLabel = dialogView.findViewById<TextInputEditText>(R.id.editTextLabel)
        val editTextKeywords = dialogView.findViewById<TextInputEditText>(R.id.editTextKeywords)
        val editTextRecipientEmail = dialogView.findViewById<TextInputEditText>(R.id.editTextRecipientEmail)
        val chipGroupPriority = dialogView.findViewById<ChipGroup>(R.id.chipGroupPriority)
        val chipNormal = dialogView.findViewById<Chip>(R.id.chipNormal)
        val chipHigh = dialogView.findViewById<Chip>(R.id.chipHigh)
        val chipCritical = dialogView.findViewById<Chip>(R.id.chipCritical)
        val switchAutoForward = dialogView.findViewById<SwitchMaterial>(R.id.switchAutoForward)
        
        // Pre-fill if editing
        rule?.let {
            editTextRuleName.setText(it.name)
            editTextLabel.setText(it.label)
            editTextKeywords.setText(it.keywords.joinToString(", "))
            editTextRecipientEmail.setText(it.recipientEmail)
            switchAutoForward.isChecked = it.autoForward
            
            when (it.priority) {
                1 -> chipHigh.isChecked = true
                2 -> chipCritical.isChecked = true
                else -> chipNormal.isChecked = true
            }
        } ?: run {
            // Default values for new rule
            val simpleConfig = SimpleConfig(this)
            val emailConfig = simpleConfig.getEmailConfig()
            editTextRecipientEmail.setText(emailConfig.recipientEmail)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.buttonSave).setOnClickListener {
            saveRule(
                dialog,
                rule,
                editTextRuleName.text.toString(),
                editTextLabel.text.toString(),
                editTextKeywords.text.toString(),
                editTextRecipientEmail.text.toString(),
                getPriorityFromChips(chipGroupPriority),
                switchAutoForward.isChecked
            )
        }
        
        dialog.show()
    }
    
    private fun getPriorityFromChips(chipGroup: ChipGroup): Int {
        return when (chipGroup.checkedChipId) {
            R.id.chipHigh -> 1
            R.id.chipCritical -> 2
            else -> 0 // Normal
        }
    }
    
    private fun saveRule(
        dialog: Dialog,
        existingRule: AdvancedFilterConfig.AdvancedFilterRule?,
        name: String,
        label: String,
        keywordsText: String,
        recipientEmail: String,
        priorityId: Int,
        autoForward: Boolean
    ) {
        if (name.isBlank() || label.isBlank() || keywordsText.isBlank() || recipientEmail.isBlank()) {
            Toast.makeText(this, "Tous les champs sont requis", Toast.LENGTH_SHORT).show()
            return
        }
        
        val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val priority = priorityId // priorityId is now directly the priority value
        
        val newRule = AdvancedFilterConfig.AdvancedFilterRule(
            id = existingRule?.id ?: UUID.randomUUID().toString(),
            name = name,
            keywords = keywords,
            label = label,
            recipientEmail = recipientEmail,
            autoForward = autoForward,
            priority = priority,
            dateCreated = existingRule?.dateCreated ?: System.currentTimeMillis()
        )
        
        val currentRules = filterConfig.getAdvancedRules().toMutableList()
        
        if (existingRule != null) {
            // Update existing rule
            val index = currentRules.indexOfFirst { it.id == existingRule.id }
            if (index != -1) {
                currentRules[index] = newRule
            }
        } else {
            // Add new rule
            currentRules.add(newRule)
        }
        
        filterConfig.saveAdvancedRules(currentRules)
        rulesAdapter.updateRules(currentRules)
        
        dialog.dismiss()
        Toast.makeText(this, "Règle sauvegardée", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Règle sauvegardée: ${newRule.name} - Libellé: ${newRule.label}")
    }
    
    private fun editRule(rule: AdvancedFilterConfig.AdvancedFilterRule) {
        showAddRuleDialog(rule)
    }
    
    private fun deleteRule(rule: AdvancedFilterConfig.AdvancedFilterRule) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer la règle")
            .setMessage("Êtes-vous sûr de vouloir supprimer la règle '${rule.name}' ?")
            .setPositiveButton("Supprimer") { _, _ ->
                val currentRules = filterConfig.getAdvancedRules().toMutableList()
                currentRules.removeAll { it.id == rule.id }
                filterConfig.saveAdvancedRules(currentRules)
                rulesAdapter.updateRules(currentRules)
                Toast.makeText(this, "Règle supprimée", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun toggleRule(rule: AdvancedFilterConfig.AdvancedFilterRule) {
        val currentRules = filterConfig.getAdvancedRules().toMutableList()
        val index = currentRules.indexOfFirst { it.id == rule.id }
        if (index != -1) {
            currentRules[index] = rule.copy(enabled = !rule.enabled)
            filterConfig.saveAdvancedRules(currentRules)
            rulesAdapter.updateRules(currentRules)
        }
    }
    
    private fun saveAutoProcessingOptions() {
        val batchSize = editTextBatchSize.text.toString().toIntOrNull() ?: 10
        
        val options = AdvancedFilterConfig.AutoProcessingOptions(
            enabled = switchAutoProcessing.isChecked,
            groupByLabel = switchGroupByLabel.isChecked,
            maxBatchSize = batchSize
        )
        
        filterConfig.saveAutoProcessingOptions(options)
        Log.d(TAG, "Options de traitement automatique sauvegardées")
    }
    
    private fun saveDateFilter() {
        val filter = AdvancedFilterConfig.DateFilter(
            enabled = switchDateFilter.isChecked,
            startDate = startDateTimestamp,
            endDate = endDateTimestamp
        )
        
        filterConfig.saveDateFilter(filter)
        Log.d(TAG, "Filtre de date sauvegardé")
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                if (isStartDate) {
                    startDateTimestamp = calendar.timeInMillis
                } else {
                    endDateTimestamp = calendar.timeInMillis
                }
                updateDateButtons()
                saveDateFilter()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun updateDateButtons() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        buttonStartDate.text = startDateTimestamp?.let { sdf.format(Date(it)) } ?: "Sélectionner"
        buttonEndDate.text = endDateTimestamp?.let { sdf.format(Date(it)) } ?: "Sélectionner"
    }
    
    private fun processAllSMS() {
        activityScope.launch {
            try {
                buttonProcessSMS.isEnabled = false
                buttonProcessSMS.text = "Traitement en cours..."
                
                val result = smsProcessor.processSMSWithFilters(forceAll = true)
                
                showProcessingResult(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "Erreur traitement SMS", e)
                Toast.makeText(this@AdvancedFiltersActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                buttonProcessSMS.isEnabled = true
                buttonProcessSMS.text = "📧 Traiter tous les SMS"
            }
        }
    }
    
    private fun processFilteredSMS() {
        activityScope.launch {
            try {
                buttonProcessFiltered.isEnabled = false
                buttonProcessFiltered.text = "Traitement en cours..."
                
                val result = smsProcessor.processSMSWithFilters()
                
                showProcessingResult(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "Erreur traitement SMS filtrés", e)
                Toast.makeText(this@AdvancedFiltersActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                buttonProcessFiltered.isEnabled = true
                buttonProcessFiltered.text = "🔍 Traiter SMS filtrés"
            }
        }
    }
    
    private fun showProcessingResult(result: AdvancedSmsProcessor.ProcessingResult) {
        val message = buildString {
            append("📊 Résultats du traitement:\n\n")
            append("📱 SMS traités: ${result.totalProcessed}\n")
            append("📧 Emails envoyés: ${result.emailsSent}\n\n")
            
            if (result.groupedByLabel.isNotEmpty()) {
                append("🏷️ Répartition par libellé:\n")
                result.groupedByLabel.forEach { (label, count) ->
                    append("• $label: $count SMS\n")
                }
                append("\n")
            }
            
            if (result.errors.isNotEmpty()) {
                append("⚠️ Erreurs (${result.errors.size}):\n")
                result.errors.take(3).forEach { error ->
                    append("• $error\n")
                }
                if (result.errors.size > 3) {
                    append("• ... et ${result.errors.size - 3} autres\n")
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Traitement terminé")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showStatistics() {
        val stats = smsProcessor.getFilterStatistics()
        
        val message = buildString {
            append("📊 Statistiques des filtres:\n\n")
            append("📱 Total SMS: ${stats["totalSMS"]}\n")
            append("🏷️ Règles totales: ${stats["totalRules"]}\n")
            append("✅ Règles actives: ${stats["activeRules"]}\n")
            append("🚀 Auto-transfert: ${stats["autoForwardRules"]}\n")
            append("📂 Libellés créés: ${stats["groupedLabels"]}\n\n")
            
            @Suppress("UNCHECKED_CAST")
            val labelDistribution = stats["labelDistribution"] as? Map<String, Int>
            if (!labelDistribution.isNullOrEmpty()) {
                append("📈 Répartition par libellé:\n")
                labelDistribution.forEach { (label, count) ->
                    append("• $label: $count SMS\n")
                }
                append("\n")
            }
            
            val lastProcessed = stats["lastProcessed"] as? Long ?: 0
            if (lastProcessed > 0) {
                append("⏰ Dernier traitement: ${filterConfig.formatDate(lastProcessed)}")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Statistiques")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}

/**
 * Adapter pour la liste des règles de filtrage
 */
class FilterRulesAdapter(
    private val onActionClick: (AdvancedFilterConfig.AdvancedFilterRule, Action) -> Unit
) : RecyclerView.Adapter<FilterRulesAdapter.RuleViewHolder>() {
    
    enum class Action { EDIT, DELETE, TOGGLE }
    
    private var rules = listOf<AdvancedFilterConfig.AdvancedFilterRule>()
    
    fun updateRules(newRules: List<AdvancedFilterConfig.AdvancedFilterRule>) {
        rules = newRules
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_rule, parent, false)
        return RuleViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(rules[position])
    }
    
    override fun getItemCount() = rules.size
    
    inner class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator = itemView.findViewById<View>(R.id.colorIndicator)
        private val textRuleName = itemView.findViewById<TextView>(R.id.textRuleName)
        private val textLabel = itemView.findViewById<TextView>(R.id.textLabel)
        private val textKeywords = itemView.findViewById<TextView>(R.id.textKeywords)
        private val chipPriority = itemView.findViewById<Chip>(R.id.chipPriority)
        private val switchEnabled = itemView.findViewById<SwitchMaterial>(R.id.switchEnabled)
        private val chipAutoForward = itemView.findViewById<Chip>(R.id.chipAutoForward)
        private val buttonEdit = itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonEdit)
        private val buttonDelete = itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonDelete)
        
        fun bind(rule: AdvancedFilterConfig.AdvancedFilterRule) {
            textRuleName.text = rule.name
            textLabel.text = "🏷️ ${rule.label}"
            textKeywords.text = "Mots-clés: ${rule.keywords.joinToString(", ")}"
            
            val priorityText = when (rule.priority) {
                1 -> "⚠️ Haute"
                2 -> "🚨 Critique"
                else -> "📝 Normale"
            }
            chipPriority.text = priorityText
            
            switchEnabled.isChecked = rule.enabled
            
            chipAutoForward.visibility = if (rule.autoForward) View.VISIBLE else View.GONE
            
            try {
                colorIndicator.setBackgroundColor(Color.parseColor(rule.color))
            } catch (e: Exception) {
                colorIndicator.setBackgroundColor(Color.BLUE)
            }
            
            // Click listeners
            switchEnabled.setOnCheckedChangeListener { _, _ ->
                onActionClick(rule, Action.TOGGLE)
            }
            
            buttonEdit.setOnClickListener { onActionClick(rule, Action.EDIT) }
            buttonDelete.setOnClickListener { onActionClick(rule, Action.DELETE) }
        }
    }
}