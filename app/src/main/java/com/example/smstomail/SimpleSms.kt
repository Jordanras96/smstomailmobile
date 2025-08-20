package com.example.smstomail

/**
 * Classe de données simple pour représenter un SMS
 */
data class SimpleSms(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val hash: String = "",
    val sent: Boolean = false
) {
    constructor(id: String, sender: String, content: String, timestamp: Long) :
        this(id, sender, content, timestamp, "", false)
}