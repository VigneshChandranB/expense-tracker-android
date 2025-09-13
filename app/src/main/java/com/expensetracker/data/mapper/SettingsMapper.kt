package com.expensetracker.data.mapper

import com.expensetracker.data.local.entity.AppSettingsEntity
import com.expensetracker.data.local.entity.DataManagementSettingsEntity
import com.expensetracker.data.local.entity.PrivacySettingsEntity
import com.expensetracker.data.local.entity.NotificationPreferencesEntity
import com.expensetracker.data.local.entity.AccountNotificationPreferencesEntity
import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.DataManagementSettings
import com.expensetracker.domain.model.PrivacySettings
import com.expensetracker.domain.model.NotificationPreferences
import com.expensetracker.domain.model.AccountNotificationPreferences
import com.expensetracker.domain.model.ThemeMode

/**
 * Mapper for converting between settings entities and domain models
 */
object SettingsMapper {
    
    // App Settings Mapping
    fun AppSettingsEntity.toDomain(): AppSettings {
        return AppSettings(
            id = id,
            themeMode = ThemeMode.valueOf(themeMode),
            smsPermissionEnabled = smsPermissionEnabled,
            autoCategorizationEnabled = autoCategorizationEnabled,
            biometricAuthEnabled = biometricAuthEnabled,
            autoLockTimeoutMinutes = autoLockTimeoutMinutes,
            currencyCode = currencyCode,
            dateFormat = dateFormat,
            firstDayOfWeek = firstDayOfWeek,
            backupEnabled = backupEnabled,
            lastBackupTimestamp = lastBackupTimestamp,
            dataRetentionMonths = dataRetentionMonths,
            crashReportingEnabled = crashReportingEnabled,
            analyticsEnabled = analyticsEnabled
        )
    }
    
    fun AppSettings.toEntity(): AppSettingsEntity {
        return AppSettingsEntity(
            id = id,
            themeMode = themeMode.name,
            smsPermissionEnabled = smsPermissionEnabled,
            autoCategorizationEnabled = autoCategorizationEnabled,
            biometricAuthEnabled = biometricAuthEnabled,
            autoLockTimeoutMinutes = autoLockTimeoutMinutes,
            currencyCode = currencyCode,
            dateFormat = dateFormat,
            firstDayOfWeek = firstDayOfWeek,
            backupEnabled = backupEnabled,
            lastBackupTimestamp = lastBackupTimestamp,
            dataRetentionMonths = dataRetentionMonths,
            crashReportingEnabled = crashReportingEnabled,
            analyticsEnabled = analyticsEnabled,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Data Management Settings Mapping
    fun DataManagementSettingsEntity.toDomain(): DataManagementSettings {
        return DataManagementSettings(
            autoDeleteOldTransactions = autoDeleteOldTransactions,
            retentionPeriodMonths = retentionPeriodMonths,
            autoBackupEnabled = autoBackupEnabled,
            backupFrequencyDays = backupFrequencyDays,
            includeAccountsInBackup = includeAccountsInBackup,
            includeCategoriesInBackup = includeCategoriesInBackup,
            includeNotificationSettingsInBackup = includeNotificationSettingsInBackup
        )
    }
    
    fun DataManagementSettings.toEntity(): DataManagementSettingsEntity {
        return DataManagementSettingsEntity(
            autoDeleteOldTransactions = autoDeleteOldTransactions,
            retentionPeriodMonths = retentionPeriodMonths,
            autoBackupEnabled = autoBackupEnabled,
            backupFrequencyDays = backupFrequencyDays,
            includeAccountsInBackup = includeAccountsInBackup,
            includeCategoriesInBackup = includeCategoriesInBackup,
            includeNotificationSettingsInBackup = includeNotificationSettingsInBackup,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Privacy Settings Mapping
    fun PrivacySettingsEntity.toDomain(): PrivacySettings {
        return PrivacySettings(
            smsDataProcessingEnabled = smsDataProcessingEnabled,
            localDataOnlyMode = localDataOnlyMode,
            requireAuthForSensitiveActions = requireAuthForSensitiveActions,
            hideBalancesInRecents = hideBalancesInRecents,
            autoLockEnabled = autoLockEnabled,
            screenshotBlocked = screenshotBlocked
        )
    }
    
    fun PrivacySettings.toEntity(): PrivacySettingsEntity {
        return PrivacySettingsEntity(
            smsDataProcessingEnabled = smsDataProcessingEnabled,
            localDataOnlyMode = localDataOnlyMode,
            requireAuthForSensitiveActions = requireAuthForSensitiveActions,
            hideBalancesInRecents = hideBalancesInRecents,
            autoLockEnabled = autoLockEnabled,
            screenshotBlocked = screenshotBlocked,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Notification Preferences Mapping
    fun NotificationPreferencesEntity.toDomain(): NotificationPreferences {
        return NotificationPreferences(
            id = id,
            billRemindersEnabled = billRemindersEnabled,
            billReminderDaysBefore = billReminderDaysBefore,
            spendingLimitAlertsEnabled = spendingLimitAlertsEnabled,
            lowBalanceAlertsEnabled = lowBalanceAlertsEnabled,
            lowBalanceThreshold = lowBalanceThreshold,
            unusualSpendingAlertsEnabled = unusualSpendingAlertsEnabled,
            transactionNotificationsEnabled = transactionNotificationsEnabled,
            weeklyReportsEnabled = weeklyReportsEnabled,
            monthlyReportsEnabled = monthlyReportsEnabled,
            notificationSound = notificationSound,
            vibrationEnabled = vibrationEnabled,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd
        )
    }
    
    fun NotificationPreferences.toEntity(): NotificationPreferencesEntity {
        return NotificationPreferencesEntity(
            id = id,
            billRemindersEnabled = billRemindersEnabled,
            billReminderDaysBefore = billReminderDaysBefore,
            spendingLimitAlertsEnabled = spendingLimitAlertsEnabled,
            lowBalanceAlertsEnabled = lowBalanceAlertsEnabled,
            lowBalanceThreshold = lowBalanceThreshold,
            unusualSpendingAlertsEnabled = unusualSpendingAlertsEnabled,
            transactionNotificationsEnabled = transactionNotificationsEnabled,
            weeklyReportsEnabled = weeklyReportsEnabled,
            monthlyReportsEnabled = monthlyReportsEnabled,
            notificationSound = notificationSound,
            vibrationEnabled = vibrationEnabled,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Account Notification Preferences Mapping
    fun AccountNotificationPreferencesEntity.toDomain(): AccountNotificationPreferences {
        return AccountNotificationPreferences(
            id = id,
            accountId = accountId,
            spendingLimitEnabled = spendingLimitEnabled,
            spendingLimit = spendingLimit,
            lowBalanceEnabled = lowBalanceEnabled,
            lowBalanceThreshold = lowBalanceThreshold,
            transactionAlertsEnabled = transactionAlertsEnabled,
            billRemindersEnabled = billRemindersEnabled
        )
    }
    
    fun AccountNotificationPreferences.toEntity(): AccountNotificationPreferencesEntity {
        return AccountNotificationPreferencesEntity(
            id = id,
            accountId = accountId,
            spendingLimitEnabled = spendingLimitEnabled,
            spendingLimit = spendingLimit,
            lowBalanceEnabled = lowBalanceEnabled,
            lowBalanceThreshold = lowBalanceThreshold,
            transactionAlertsEnabled = transactionAlertsEnabled,
            billRemindersEnabled = billRemindersEnabled,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Default values
    fun getDefaultAppSettings(): AppSettings {
        return AppSettings()
    }
    
    fun getDefaultDataManagementSettings(): DataManagementSettings {
        return DataManagementSettings()
    }
    
    fun getDefaultPrivacySettings(): PrivacySettings {
        return PrivacySettings()
    }
    
    fun getDefaultNotificationPreferences(): NotificationPreferences {
        return NotificationPreferences()
    }
}