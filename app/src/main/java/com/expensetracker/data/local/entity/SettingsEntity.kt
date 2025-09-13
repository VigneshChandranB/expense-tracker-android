package com.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for application settings
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Long = 1L, // Single row for app settings
    val themeMode: String = "SYSTEM",
    val smsPermissionEnabled: Boolean = false,
    val autoCategorizationEnabled: Boolean = true,
    val biometricAuthEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val currencyCode: String = "INR",
    val dateFormat: String = "dd/MM/yyyy",
    val firstDayOfWeek: Int = 1,
    val backupEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val dataRetentionMonths: Int = 24,
    val crashReportingEnabled: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for data management settings
 */
@Entity(tableName = "data_management_settings")
data class DataManagementSettingsEntity(
    @PrimaryKey
    val id: Long = 1L, // Single row for data management settings
    val autoDeleteOldTransactions: Boolean = false,
    val retentionPeriodMonths: Int = 24,
    val autoBackupEnabled: Boolean = false,
    val backupFrequencyDays: Int = 7,
    val includeAccountsInBackup: Boolean = true,
    val includeCategoriesInBackup: Boolean = true,
    val includeNotificationSettingsInBackup: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room entity for privacy settings
 */
@Entity(tableName = "privacy_settings")
data class PrivacySettingsEntity(
    @PrimaryKey
    val id: Long = 1L, // Single row for privacy settings
    val smsDataProcessingEnabled: Boolean = false,
    val localDataOnlyMode: Boolean = true,
    val requireAuthForSensitiveActions: Boolean = true,
    val hideBalancesInRecents: Boolean = false,
    val autoLockEnabled: Boolean = true,
    val screenshotBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)