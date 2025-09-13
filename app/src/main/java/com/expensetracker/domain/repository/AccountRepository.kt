package com.expensetracker.domain.repository

import com.expensetracker.domain.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Account operations
 */
interface AccountRepository {
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun getAccount(id: Long): Account?
    suspend fun getAllAccounts(): List<Account>
    suspend fun getActiveAccounts(): List<Account>
    fun observeAccounts(): Flow<List<Account>>
    fun observeActiveAccounts(): Flow<List<Account>>
}