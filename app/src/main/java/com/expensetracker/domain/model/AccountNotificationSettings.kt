package com.expensetracker.domain.model

import java.math.BigDecimal

/**
 * Account-specific notification settings
 */
data class AccountNotificationSettings(
    val accountId: Long,
    val spendingLimitEnabled: Boolean = false,
    val spendingLimit: BigDecimal? = null,
    val lowBalanceEnabled: Boolean = false,
    val lowBalanceThreshold: BigDecimal? = null,
    val unusualSpendingEnabled: Boolean = false,
    val largeTransactionEnabled: Boolean = false,
    val largeTransactionThreshold: BigDecimal? = null
)