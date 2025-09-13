package com.expensetracker.domain.model

/**
 * Application-wide settings and preferences
 */
data class AppSettings(
    val id: Long = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val smsPermissionEnabled: Boolean = false,
    val autoCategorizationEnabled: Boolean = true,
    val biometricAuthEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val currencyCode: String = "INR",
    val dateFormat: String = "dd/MM/yyyy",
    val firstDayOfWeek: Int = 1, // Monday
    val backupEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val dataRetentionMonths: Int = 24,
    val crashReportingEnabled: Boolean = true,
    val analyticsEnabled: Boolean = false
)

/**
 * Theme mode options
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Settings for data management
 */
data class DataManagementSettings(
    val autoDeleteOldTransactions: Boolean = false,
    val retentionPeriodMonths: Int = 24,
    val autoBackupEnabled: Boolean = false,
    val backupFrequencyDays: Int = 7,
    val includeAccountsInBackup: Boolean = true,
    val includeCategoriesInBackup: Boolean = true,
    val includeNotificationSettingsInBackup: Boolean = true
)

/**
 * Privacy and security settings
 */
data class PrivacySettings(
    val smsDataProcessingEnabled: Boolean = false,
    val localDataOnlyMode: Boolean = true,
    val requireAuthForSensitiveActions: Boolean = true,
    val hideBalancesInRecents: Boolean = false,
    val autoLockEnabled: Boolean = true,
    val screenshotBlocked: Boolean = false
)