package com.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.expensetracker.data.local.converter.BigDecimalConverter
import com.expensetracker.data.local.converter.LocalDateTimeConverter
import com.expensetracker.data.local.converter.MapConverter
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Room entity for storing notification data
 */
@Entity(tableName = "notifications")
@TypeConverters(LocalDateTimeConverter::class, BigDecimalConverter::class, MapConverter::class)
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val priority: String,
    val title: String,
    val message: String,
    val accountId: Long? = null,
    val transactionId: Long? = null,
    val amount: BigDecimal? = null,
    val categoryId: Long? = null,
    val scheduledTime: LocalDateTime,
    val createdAt: LocalDateTime,
    val isDelivered: Boolean = false,
    val deliveredAt: LocalDateTime? = null,
    val actionData: Map<String, String> = emptyMap()
)

/**
 * Room entity for storing notification preferences
 */
@Entity(tableName = "notification_preferences")
@TypeConverters(BigDecimalConverter::class, MapConverter::class)
data class NotificationPreferencesEntity(
    @PrimaryKey
    val id: Long = 1, // Single row for user preferences
    val billRemindersEnabled: Boolean = true,
    val billReminderDaysBefore: Int = 3,
    val spendingLimitAlertsEnabled: Boolean = true,
    val lowBalanceWarningsEnabled: Boolean = true,
    val lowBalanceThreshold: BigDecimal = BigDecimal("1000.00"),
    val unusualSpendingAlertsEnabled: Boolean = true,
    val budgetExceededAlertsEnabled: Boolean = true,
    val largeTransactionAlertsEnabled: Boolean = true,
    val largeTransactionThreshold: BigDecimal = BigDecimal("5000.00"),
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 8,
    val accountSpecificSettings: Map<String, String> = emptyMap() // JSON serialized account settings
)

/**
 * Room entity for account-specific notification settings
 */
@Entity(tableName = "account_notification_settings")
@TypeConverters(BigDecimalConverter::class)
data class AccountNotificationSettingsEntity(
    @PrimaryKey
    val accountId: Long,
    val spendingLimitEnabled: Boolean = true,
    val spendingLimit: BigDecimal? = null,
    val lowBalanceEnabled: Boolean = true,
    val lowBalanceThreshold: BigDecimal? = null,
    val unusualSpendingEnabled: Boolean = true
)