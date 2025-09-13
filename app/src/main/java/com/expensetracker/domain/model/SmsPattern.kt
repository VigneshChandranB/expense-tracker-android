package com.expensetracker.domain.model

/**
 * Domain model for SMS Pattern entity
 * Represents patterns used to parse bank SMS messages for transaction extraction
 */
data class SmsPattern(
    val id: Long = 0,
    val bankName: String,
    val senderPattern: String,
    val amountPattern: String,
    val merchantPattern: String,
    val datePattern: String,
    val typePattern: String,
    val accountPattern: String? = null,
    val isActive: Boolean = true
)