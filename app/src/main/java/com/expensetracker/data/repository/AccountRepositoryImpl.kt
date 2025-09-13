package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.AccountDao
import com.expensetracker.data.mapper.AccountMapper
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AccountRepository
 * Handles account data operations with local database
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val accountMapper: AccountMapper
) : AccountRepository {

    override suspend fun insertAccount(account: Account): Long {
        val entity = accountMapper.toEntity(account)
        return accountDao.insertAccount(entity)
    }

    override suspend fun updateAccount(account: Account) {
        val entity = accountMapper.toEntity(account)
        accountDao.updateAccount(entity)
    }

    override suspend fun deleteAccount(id: Long) {
        accountDao.deleteAccountById(id)
    }

    override suspend fun getAccount(id: Long): Account? {
        return accountDao.getAccountById(id)?.let { entity ->
            accountMapper.toDomain(entity)
        }
    }

    override suspend fun getAllAccounts(): List<Account> {
        return accountDao.getAllAccounts().map { entity ->
            accountMapper.toDomain(entity)
        }
    }

    override suspend fun getActiveAccounts(): List<Account> {
        return accountDao.getActiveAccounts().map { entity ->
            accountMapper.toDomain(entity)
        }
    }

    override fun observeAccounts(): Flow<List<Account>> {
        return accountDao.observeAllAccounts().map { entities ->
            entities.map { entity -> accountMapper.toDomain(entity) }
        }
    }

    override fun observeActiveAccounts(): Flow<List<Account>> {
        return accountDao.observeActiveAccounts().map { entities ->
            entities.map { entity -> accountMapper.toDomain(entity) }
        }
    }

    suspend fun updateAccountStatus(id: Long, isActive: Boolean) {
        accountDao.updateAccountStatus(id, isActive)
    }

    suspend fun updateAccountBalance(id: Long, balance: java.math.BigDecimal) {
        accountDao.updateAccountBalance(id, balance.toString())
    }

    suspend fun getAccountsByBank(bankName: String): List<Account> {
        return accountDao.getAccountsByBank(bankName).map { entity ->
            accountMapper.toDomain(entity)
        }
    }

    suspend fun getTotalBalance(): java.math.BigDecimal {
        val balance = accountDao.getTotalBalance() ?: 0.0
        return java.math.BigDecimal.valueOf(balance)
    }

    suspend fun getTotalCreditCardBalance(): java.math.BigDecimal {
        val balance = accountDao.getTotalCreditCardBalance() ?: 0.0
        return java.math.BigDecimal.valueOf(balance)
    }

    suspend fun searchAccountsByNumber(accountNumber: String): List<Account> {
        return accountDao.searchAccountsByNumber(accountNumber).map { entity ->
            accountMapper.toDomain(entity)
        }
    }
}