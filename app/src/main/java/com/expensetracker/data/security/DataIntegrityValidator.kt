package com.expensetracker.data.security

import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates data integrity and detects potential tampering
 */
@Singleton
class DataIntegrityValidator @Inject constructor(
    private val database: ExpenseDatabase
) {
    
    /**
     * Validates the integrity of all financial data
     */
    suspend fun validateAllData(): DataIntegrityReport = withContext(Dispatchers.IO) {
        val report = DataIntegrityReport()
        
        try {
            // Validate transactions
            val transactionValidation = validateTransactions()
            report.transactionIntegrity = transactionValidation
            
            // Validate accounts
            val accountValidation = validateAccounts()
            report.accountIntegrity = accountValidation
            
            // Validate balance consistency
            val balanceValidation = validateBalanceConsistency()
            report.balanceConsistency = balanceValidation
            
            // Validate foreign key relationships
            val relationshipValidation = validateRelationships()
            report.relationshipIntegrity = relationshipValidation
            
            // Calculate overall integrity score
            report.overallIntegrity = calculateOverallIntegrity(report)
            
        } catch (e: Exception) {
            report.errors.add("Critical error during validation: ${e.message}")
            report.overallIntegrity = IntegrityLevel.CRITICAL
        }
        
        report
    }
    
    /**
     * Validates transaction data integrity
     */
    private suspend fun validateTransactions(): IntegrityLevel {
        val transactions = database.transactionDao().getAllTransactions()
        var issues = 0
        
        transactions.forEach { transaction ->
            // Validate amount is positive
            if (transaction.amount <= BigDecimal.ZERO) {
                issues++
            }
            
            // Validate date is not in future
            if (transaction.date.isAfter(LocalDateTime.now())) {
                issues++
            }
            
            // Validate required fields
            if (transaction.merchant.isBlank() || transaction.categoryId <= 0) {
                issues++
            }
            
            // Validate transfer transactions have proper linking
            if (transaction.transferAccountId != null && transaction.transferTransactionId == null) {
                issues++
            }
        }
        
        return when {
            issues == 0 -> IntegrityLevel.GOOD
            issues < transactions.size * 0.05 -> IntegrityLevel.WARNING
            issues < transactions.size * 0.15 -> IntegrityLevel.POOR
            else -> IntegrityLevel.CRITICAL
        }
    }
    
    /**
     * Validates account data integrity
     */
    private suspend fun validateAccounts(): IntegrityLevel {
        val accounts = database.accountDao().getAllAccounts()
        var issues = 0
        
        accounts.forEach { account ->
            // Validate account has required fields
            if (account.bankName.isBlank() || account.nickname.isBlank()) {
                issues++
            }
            
            // Validate account number format (basic check)
            if (account.accountNumber.length < 4) {
                issues++
            }
        }
        
        return when {
            issues == 0 -> IntegrityLevel.GOOD
            issues < accounts.size * 0.1 -> IntegrityLevel.WARNING
            else -> IntegrityLevel.CRITICAL
        }
    }
    
    /**
     * Validates balance consistency across accounts and transactions
     */
    private suspend fun validateBalanceConsistency(): IntegrityLevel {
        val accounts = database.accountDao().getAllAccounts()
        var inconsistencies = 0
        
        accounts.forEach { account ->
            val calculatedBalance = calculateAccountBalance(account.id)
            val storedBalance = account.currentBalance
            
            // Allow small rounding differences (0.01)
            if ((calculatedBalance - storedBalance).abs() > BigDecimal("0.01")) {
                inconsistencies++
            }
        }
        
        return when {
            inconsistencies == 0 -> IntegrityLevel.GOOD
            inconsistencies < accounts.size * 0.1 -> IntegrityLevel.WARNING
            else -> IntegrityLevel.CRITICAL
        }
    }
    
    /**
     * Validates foreign key relationships
     */
    private suspend fun validateRelationships(): IntegrityLevel {
        var issues = 0
        
        // Check transaction-account relationships
        val orphanedTransactions = database.transactionDao().getOrphanedTransactions()
        issues += orphanedTransactions.size
        
        // Check transaction-category relationships
        val invalidCategories = database.transactionDao().getTransactionsWithInvalidCategories()
        issues += invalidCategories.size
        
        // Check transfer transaction linking
        val unlinkededTransfers = database.transactionDao().getUnlinkedTransferTransactions()
        issues += unlinkededTransfers.size
        
        return when {
            issues == 0 -> IntegrityLevel.GOOD
            issues < 10 -> IntegrityLevel.WARNING
            issues < 50 -> IntegrityLevel.POOR
            else -> IntegrityLevel.CRITICAL
        }
    }
    
    /**
     * Calculates account balance from transaction history
     */
    private suspend fun calculateAccountBalance(accountId: Long): BigDecimal {
        val transactions = database.transactionDao().getTransactionsByAccount(accountId)
        var balance = BigDecimal.ZERO
        
        transactions.forEach { transaction ->
            when (transaction.type) {
                "INCOME", "TRANSFER_IN" -> balance = balance.add(transaction.amount)
                "EXPENSE", "TRANSFER_OUT" -> balance = balance.subtract(transaction.amount)
            }
        }
        
        return balance
    }
    
    /**
     * Calculates overall integrity level
     */
    private fun calculateOverallIntegrity(report: DataIntegrityReport): IntegrityLevel {
        val levels = listOf(
            report.transactionIntegrity,
            report.accountIntegrity,
            report.balanceConsistency,
            report.relationshipIntegrity
        )
        
        return when {
            levels.any { it == IntegrityLevel.CRITICAL } -> IntegrityLevel.CRITICAL
            levels.any { it == IntegrityLevel.POOR } -> IntegrityLevel.POOR
            levels.any { it == IntegrityLevel.WARNING } -> IntegrityLevel.WARNING
            else -> IntegrityLevel.GOOD
        }
    }
    
    /**
     * Generates a hash of critical data for tamper detection
     */
    suspend fun generateDataHash(): String = withContext(Dispatchers.IO) {
        val transactions = database.transactionDao().getAllTransactions()
        val accounts = database.accountDao().getAllAccounts()
        
        val dataString = buildString {
            // Include transaction data
            transactions.sortedBy { it.id }.forEach { transaction ->
                append("${transaction.id}|${transaction.amount}|${transaction.type}|${transaction.date}")
            }
            
            // Include account data
            accounts.sortedBy { it.id }.forEach { account ->
                append("${account.id}|${account.currentBalance}|${account.bankName}")
            }
        }
        
        return@withContext hashString(dataString)
    }
    
    /**
     * Verifies data hasn't been tampered with since last hash
     */
    suspend fun verifyDataIntegrity(previousHash: String): Boolean {
        val currentHash = generateDataHash()
        return currentHash == previousHash
    }
    
    /**
     * Generates SHA-256 hash of a string
     */
    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Repairs common data integrity issues
     */
    suspend fun repairIntegrityIssues(): RepairResult = withContext(Dispatchers.IO) {
        val result = RepairResult()
        
        try {
            // Repair balance inconsistencies
            val accounts = database.accountDao().getAllAccounts()
            accounts.forEach { account ->
                val calculatedBalance = calculateAccountBalance(account.id)
                if ((calculatedBalance - account.currentBalance).abs() > BigDecimal("0.01")) {
                    database.accountDao().updateAccountBalance(account.id, calculatedBalance)
                    result.repairedBalances++
                }
            }
            
            // Remove orphaned transactions (if any)
            val orphanedCount = database.transactionDao().deleteOrphanedTransactions()
            result.removedOrphanedRecords += orphanedCount
            
            result.success = true
            
        } catch (e: Exception) {
            result.success = false
            result.error = e.message
        }
        
        result
    }
}

/**
 * Data integrity report
 */
data class DataIntegrityReport(
    var transactionIntegrity: IntegrityLevel = IntegrityLevel.GOOD,
    var accountIntegrity: IntegrityLevel = IntegrityLevel.GOOD,
    var balanceConsistency: IntegrityLevel = IntegrityLevel.GOOD,
    var relationshipIntegrity: IntegrityLevel = IntegrityLevel.GOOD,
    var overallIntegrity: IntegrityLevel = IntegrityLevel.GOOD,
    val errors: MutableList<String> = mutableListOf(),
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Integrity levels
 */
enum class IntegrityLevel {
    GOOD,
    WARNING,
    POOR,
    CRITICAL
}

/**
 * Repair operation result
 */
data class RepairResult(
    var success: Boolean = false,
    var repairedBalances: Int = 0,
    var removedOrphanedRecords: Int = 0,
    var error: String? = null
)