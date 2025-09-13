package com.expensetracker.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model for Account entity
 * Represents a bank account or financial account
 */
data class Account(
    val id: Long = 0,
    val bankName: String,
    val accountType: AccountType,
    val accountNumber: String,
    val nickname: String,
    val currentBalance: BigDecimal,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime
)

enum class AccountType {
    SAVINGS, CHECKING, CREDIT_CARD, INVESTMENT, CASH
}