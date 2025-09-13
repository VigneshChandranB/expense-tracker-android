package com.expensetracker.domain.model

/**
 * Notification preferences for the application
 */
data class NotificationPreferences(
    val id: Long = 0,
    val billRemindersEnabled: Boolean = true,
    val billReminderDaysBefore: Int = 3,
    val spendingLimitAlertsEnabled: Boolean = true,
    val lowBalanceAlertsEnabled: Boolean = true,
    val lowBalanceThreshold: Double = 1000.0,
    val unusualSpendingAlertsEnabled: Boolean = true,
    val transactionNotificationsEnabled: Boolean = false,
    val weeklyReportsEnabled: Boolean = true,
    val monthlyReportsEnabled: Boolean = true,
    val notificationSound: String = "default",
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00"
)

/**
 * Account-specific notification preferences
 */
data class AccountNotificationPreferences(
    val id: Long = 0,
    val accountId: Long,
    val spendingLimitEnabled: Boolean = false,
    val spendingLimit: Double = 0.0,
    val lowBalanceEnabled: Boolean = true,
    val lowBalanceThreshold: Double = 1000.0,
    val transactionAlertsEnabled: Boolean = false,
    val billRemindersEnabled: Boolean = true
)