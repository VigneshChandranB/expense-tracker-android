package com.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for notification preferences
 */
@Entity(tableName = "notification_preferences")
data class NotificationPreferencesEntity(
    @PrimaryKey
    val id: Long = 1L, // Single row for global notification preferences
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
    val quietHoursEnd: String = "08:00",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for account-specific notification preferences
 */
@Entity(
    tableName = "account_notification_preferences",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class AccountNotificationPreferencesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val spendingLimitEnabled: Boolean = false,
    val spendingLimit: Double = 0.0,
    val lowBalanceEnabled: Boolean = true,
    val lowBalanceThreshold: Double = 1000.0,
    val transactionAlertsEnabled: Boolean = false,
    val billRemindersEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)