package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model for Transaction entity
 * This represents the core business entity for financial transactions
 */
data class Transaction(
    val id: Long = 0,
    val amount: BigDecimal,
    val type: TransactionType,
    val category: Category,
    val merchant: String,
    val description: String?,
    val date: LocalDateTime,
    val source: TransactionSource,
    val accountId: Long,
    val transferAccountId: Long? = null,
    val transferTransactionId: Long? = null,
    val isRecurring: Boolean = false
)

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER_OUT, TRANSFER_IN
}

enum class TransactionSource {
    SMS_AUTO, MANUAL, IMPORTED
}