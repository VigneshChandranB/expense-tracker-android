package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import com.expensetracker.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing accounts
 * Handles account creation, editing, activation/deactivation, and balance tracking
 */
@Singleton
class ManageAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {

    /**
     * Creates a new account with validation
     */
    suspend fun createAccount(
        bankName: String,
        accountType: AccountType,
        accountNumber: String,
        nickname: String,
        initialBalance: BigDecimal = BigDecimal.ZERO
    ): Result<Long> {
        return try {
            // Validate input
            if (bankName.isBlank()) {
                return Result.failure(IllegalArgumentException("Bank name cannot be empty"))
            }
            if (accountNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Account number cannot be empty"))
            }
            if (nickname.isBlank()) {
                return Result.failure(IllegalArgumentException("Account nickname cannot be empty"))
            }

            // Check if account number already exists
            val existingAccounts = accountRepository.getAllAccounts()
            if (existingAccounts.any { it.accountNumber == accountNumber }) {
                return Result.failure(IllegalArgumentException("Account number already exists"))
            }

            val account = Account(
                bankName = bankName.trim(),
                accountType = accountType,
                accountNumber = accountNumber.trim(),
                nickname = nickname.trim(),
                currentBalance = initialBalance,
                isActive = true,
                createdAt = LocalDateTime.now()
            )

            val accountId = accountRepository.insertAccount(account)
            Result.success(accountId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing account
     */
    suspend fun updateAccount(
        accountId: Long,
        bankName: String? = null,
        accountType: AccountType? = null,
        accountNumber: String? = null,
        nickname: String? = null,
        currentBalance: BigDecimal? = null
    ): Result<Unit> {
        return try {
            val existingAccount = accountRepository.getAccount(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            // Validate account number uniqueness if it's being changed
            if (accountNumber != null && accountNumber != existingAccount.accountNumber) {
                val allAccounts = accountRepository.getAllAccounts()
                if (allAccounts.any { it.accountNumber == accountNumber && it.id != accountId }) {
                    return Result.failure(IllegalArgumentException("Account number already exists"))
                }
            }

            val updatedAccount = existingAccount.copy(
                bankName = bankName?.trim() ?: existingAccount.bankName,
                accountType = accountType ?: existingAccount.accountType,
                accountNumber = accountNumber?.trim() ?: existingAccount.accountNumber,
                nickname = nickname?.trim() ?: existingAccount.nickname,
                currentBalance = currentBalance ?: existingAccount.currentBalance
            )

            // Validate updated fields
            if (updatedAccount.bankName.isBlank()) {
                return Result.failure(IllegalArgumentException("Bank name cannot be empty"))
            }
            if (updatedAccount.accountNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("Account number cannot be empty"))
            }
            if (updatedAccount.nickname.isBlank()) {
                return Result.failure(IllegalArgumentException("Account nickname cannot be empty"))
            }

            accountRepository.updateAccount(updatedAccount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deactivates an account (soft delete)
     */
    suspend fun deactivateAccount(accountId: Long): Result<Unit> {
        return try {
            val account = accountRepository.getAccount(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            if (!account.isActive) {
                return Result.failure(IllegalArgumentException("Account is already deactivated"))
            }

            val deactivatedAccount = account.copy(isActive = false)
            accountRepository.updateAccount(deactivatedAccount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reactivates a deactivated account
     */
    suspend fun reactivateAccount(accountId: Long): Result<Unit> {
        return try {
            val account = accountRepository.getAccount(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            if (account.isActive) {
                return Result.failure(IllegalArgumentException("Account is already active"))
            }

            val reactivatedAccount = account.copy(isActive = true)
            accountRepository.updateAccount(reactivatedAccount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes an account
     */
    suspend fun deleteAccount(accountId: Long): Result<Unit> {
        return try {
            val account = accountRepository.getAccount(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            accountRepository.deleteAccount(accountId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all accounts
     */
    suspend fun getAllAccounts(): List<Account> {
        return accountRepository.getAllAccounts()
    }

    /**
     * Gets only active accounts
     */
    suspend fun getActiveAccounts(): List<Account> {
        return accountRepository.getActiveAccounts()
    }

    /**
     * Gets a specific account by ID
     */
    suspend fun getAccount(accountId: Long): Account? {
        return accountRepository.getAccount(accountId)
    }

    /**
     * Observes all accounts
     */
    fun observeAccounts(): Flow<List<Account>> {
        return accountRepository.observeAccounts()
    }

    /**
     * Observes only active accounts
     */
    fun observeActiveAccounts(): Flow<List<Account>> {
        return accountRepository.observeActiveAccounts()
    }

    /**
     * Updates account balance (typically called after transaction processing)
     */
    suspend fun updateAccountBalance(accountId: Long, newBalance: BigDecimal): Result<Unit> {
        return try {
            val account = accountRepository.getAccount(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            val updatedAccount = account.copy(currentBalance = newBalance)
            accountRepository.updateAccount(updatedAccount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculates and updates account balance based on transactions
     */
    suspend fun recalculateAccountBalance(accountId: Long): Result<BigDecimal> {
        return try {
            val account = accountRepository.getAccount(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            // This would typically involve calculating balance from transactions
            // For now, we'll return the current balance
            // In a full implementation, this would sum all transactions for the account
            Result.success(account.currentBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}