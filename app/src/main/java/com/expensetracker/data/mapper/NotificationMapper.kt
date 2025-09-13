package com.expensetracker.data.mapper

import com.expensetracker.data.local.entity.AccountNotificationSettingsEntity
import com.expensetracker.data.local.entity.NotificationEntity
import com.expensetracker.data.local.entity.NotificationPreferencesEntity
import com.expensetracker.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Mapper for converting between notification domain models and data entities
 */
object NotificationMapper {
    
    private val gson = Gson()
    
    fun toEntity(notification: NotificationData): NotificationEntity {
        return NotificationEntity(
            id = notification.id,
            type = notification.type.name,
            priority = notification.priority.name,
            title = notification.title,
            message = notification.message,
            accountId = notification.accountId,
            transactionId = notification.transactionId,
            amount = notification.amount,
            categoryId = notification.category?.id,
            scheduledTime = notification.scheduledTime,
            createdAt = notification.createdAt,
            isDelivered = notification.isDelivered,
            deliveredAt = notification.deliveredAt,
            actionData = notification.actionData
        )
    }
    
    fun toDomain(entity: NotificationEntity, category: Category? = null): NotificationData {
        return NotificationData(
            id = entity.id,
            type = NotificationType.valueOf(entity.type),
            priority = NotificationPriority.valueOf(entity.priority),
            title = entity.title,
            message = entity.message,
            accountId = entity.accountId,
            transactionId = entity.transactionId,
            amount = entity.amount,
            category = category,
            scheduledTime = entity.scheduledTime,
            createdAt = entity.createdAt,
            isDelivered = entity.isDelivered,
            deliveredAt = entity.deliveredAt,
            actionData = entity.actionData
        )
    }
    
    fun toEntity(preferences: NotificationPreferences): NotificationPreferencesEntity {
        val accountSettingsJson = gson.toJson(preferences.accountSpecificSettings.mapKeys { it.key.toString() })
        
        return NotificationPreferencesEntity(
            id = preferences.id,
            billRemindersEnabled = preferences.billRemindersEnabled,
            billReminderDaysBefore = preferences.billReminderDaysBefore,
            spendingLimitAlertsEnabled = preferences.spendingLimitAlertsEnabled,
            lowBalanceWarningsEnabled = preferences.lowBalanceWarningsEnabled,
            lowBalanceThreshold = preferences.lowBalanceThreshold,
            unusualSpendingAlertsEnabled = preferences.unusualSpendingAlertsEnabled,
            budgetExceededAlertsEnabled = preferences.budgetExceededAlertsEnabled,
            largeTransactionAlertsEnabled = preferences.largeTransactionAlertsEnabled,
            largeTransactionThreshold = preferences.largeTransactionThreshold,
            quietHoursEnabled = preferences.quietHoursEnabled,
            quietHoursStart = preferences.quietHoursStart,
            quietHoursEnd = preferences.quietHoursEnd,
            accountSpecificSettings = mapOf("settings" to accountSettingsJson)
        )
    }
    
    fun toDomain(
        entity: NotificationPreferencesEntity,
        accountSettings: List<AccountNotificationSettingsEntity> = emptyList()
    ): NotificationPreferences {
        val accountSpecificSettings = accountSettings.associate { setting ->
            setting.accountId to AccountNotificationSettings(
                accountId = setting.accountId,
                spendingLimitEnabled = setting.spendingLimitEnabled,
                spendingLimit = setting.spendingLimit,
                lowBalanceEnabled = setting.lowBalanceEnabled,
                lowBalanceThreshold = setting.lowBalanceThreshold,
                unusualSpendingEnabled = setting.unusualSpendingEnabled
            )
        }
        
        return NotificationPreferences(
            id = entity.id,
            billRemindersEnabled = entity.billRemindersEnabled,
            billReminderDaysBefore = entity.billReminderDaysBefore,
            spendingLimitAlertsEnabled = entity.spendingLimitAlertsEnabled,
            lowBalanceWarningsEnabled = entity.lowBalanceWarningsEnabled,
            lowBalanceThreshold = entity.lowBalanceThreshold,
            unusualSpendingAlertsEnabled = entity.unusualSpendingAlertsEnabled,
            budgetExceededAlertsEnabled = entity.budgetExceededAlertsEnabled,
            largeTransactionAlertsEnabled = entity.largeTransactionAlertsEnabled,
            largeTransactionThreshold = entity.largeTransactionThreshold,
            quietHoursEnabled = entity.quietHoursEnabled,
            quietHoursStart = entity.quietHoursStart,
            quietHoursEnd = entity.quietHoursEnd,
            accountSpecificSettings = accountSpecificSettings
        )
    }
    
    fun toEntity(settings: AccountNotificationSettings): AccountNotificationSettingsEntity {
        return AccountNotificationSettingsEntity(
            accountId = settings.accountId,
            spendingLimitEnabled = settings.spendingLimitEnabled,
            spendingLimit = settings.spendingLimit,
            lowBalanceEnabled = settings.lowBalanceEnabled,
            lowBalanceThreshold = settings.lowBalanceThreshold,
            unusualSpendingEnabled = settings.unusualSpendingEnabled
        )
    }
    
    fun toDomain(entity: AccountNotificationSettingsEntity): AccountNotificationSettings {
        return AccountNotificationSettings(
            accountId = entity.accountId,
            spendingLimitEnabled = entity.spendingLimitEnabled,
            spendingLimit = entity.spendingLimit,
            lowBalanceEnabled = entity.lowBalanceEnabled,
            lowBalanceThreshold = entity.lowBalanceThreshold,
            unusualSpendingEnabled = entity.unusualSpendingEnabled
        )
    }
}