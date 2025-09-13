package com.expensetracker.domain.model

import java.time.LocalDateTime

/**
 * Data structure for backup and restore operations
 */
data class BackupData(
    val version: Int = 1,
    val timestamp: LocalDateTime,
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val notificationPreferences: NotificationPreferences,
    val appSettings: AppSettings,
    val dataManagementSettings: DataManagementSettings,
    val privacySettings: PrivacySettings,
    val checksum: String
)

/**
 * Backup metadata for listing and validation
 */
data class BackupMetadata(
    val fileName: String,
    val timestamp: LocalDateTime,
    val version: Int,
    val size: Long,
    val accountCount: Int,
    val transactionCount: Int,
    val isValid: Boolean
)

/**
 * Result of backup operation
 */
sealed class BackupResult {
    data class Success(val filePath: String, val metadata: BackupMetadata) : BackupResult()
    data class Error(val message: String, val cause: Throwable? = null) : BackupResult()
}

/**
 * Result of restore operation
 */
sealed class RestoreResult {
    data class Success(val restoredData: BackupData) : RestoreResult()
    data class Error(val message: String, val cause: Throwable? = null) : RestoreResult()
    data class ValidationError(val issues: List<String>) : RestoreResult()
}