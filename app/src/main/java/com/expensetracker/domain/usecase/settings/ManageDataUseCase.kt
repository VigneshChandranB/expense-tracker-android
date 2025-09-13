package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.repository.SettingsRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.permission.PermissionManager
import javax.inject.Inject

/**
 * Use case for managing application data (deletion, cleanup)
 */
class ManageDataUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val permissionManager: PermissionManager
) {
    
    /**
     * Deletes all SMS-derived transaction data
     */
    suspend fun deleteSmsData(): Result<Unit> {
        return try {
            transactionRepository.deleteSmsTransactions()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes all application data
     */
    suspend fun deleteAllData(): Result<Unit> {
        return try {
            transactionRepository.deleteAllTransactions()
            accountRepository.deleteAllAccounts()
            categoryRepository.deleteCustomCategories()
            settingsRepository.resetToDefaults()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Revokes SMS permission and deletes SMS data
     */
    suspend fun revokeSmsPermissionAndDeleteData(): Result<Unit> {
        return try {
            // Update settings to disable SMS permission
            val currentSettings = settingsRepository.getAppSettings()
            settingsRepository.updateAppSettings(
                currentSettings.copy(smsPermissionEnabled = false)
            )
            
            // Delete SMS-derived data
            deleteSmsData()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cleans up old transactions based on retention policy
     */
    suspend fun cleanupOldData(): Result<Int> {
        return try {
            val dataSettings = settingsRepository.getDataManagementSettings()
            if (dataSettings.autoDeleteOldTransactions) {
                val deletedCount = transactionRepository.deleteOldTransactions(
                    retentionMonths = dataSettings.retentionPeriodMonths
                )
                Result.success(deletedCount)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets data usage statistics
     */
    suspend fun getDataUsageStats(): DataUsageStats {
        return try {
            val transactionCount = transactionRepository.getTransactionCount()
            val accountCount = accountRepository.getAccountCount()
            val categoryCount = categoryRepository.getCategoryCount()
            val smsTransactionCount = transactionRepository.getSmsTransactionCount()
            
            DataUsageStats(
                totalTransactions = transactionCount,
                smsTransactions = smsTransactionCount,
                manualTransactions = transactionCount - smsTransactionCount,
                totalAccounts = accountCount,
                totalCategories = categoryCount
            )
        } catch (e: Exception) {
            DataUsageStats()
        }
    }
}

/**
 * Data usage statistics
 */
data class DataUsageStats(
    val totalTransactions: Int = 0,
    val smsTransactions: Int = 0,
    val manualTransactions: Int = 0,
    val totalAccounts: Int = 0,
    val totalCategories: Int = 0
)