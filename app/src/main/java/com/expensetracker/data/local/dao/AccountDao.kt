package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Account operations
 */
@Dao
interface AccountDao {
    
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun observeAllAccounts(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActiveAccounts(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?
    
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>
    
    @Query("SELECT * FROM accounts WHERE isActive = 1")
    suspend fun getActiveAccounts(): List<AccountEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long
    
    @Update
    suspend fun updateAccount(account: AccountEntity)
    
    @Delete
    suspend fun deleteAccount(account: AccountEntity)
    
    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)
    
    @Query("SELECT * FROM accounts WHERE bankName = :bankName")
    suspend fun getAccountsByBank(bankName: String): List<AccountEntity>
    
    @Query("SELECT * FROM accounts WHERE accountType = :accountType AND isActive = 1")
    suspend fun getAccountsByType(accountType: String): List<AccountEntity>
    
    @Query("UPDATE accounts SET isActive = :isActive WHERE id = :id")
    suspend fun updateAccountStatus(id: Long, isActive: Boolean)
    
    @Query("UPDATE accounts SET currentBalance = :balance WHERE id = :id")
    suspend fun updateAccountBalance(id: Long, balance: String)
    
    // Overload for BigDecimal
    suspend fun updateAccountBalance(id: Long, balance: java.math.BigDecimal) {
        updateAccountBalance(id, balance.toString())
    }
    
    @Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
    suspend fun getActiveAccountCount(): Int
    
    @Query("SELECT SUM(CAST(currentBalance AS REAL)) FROM accounts WHERE isActive = 1 AND accountType != 'CREDIT_CARD'")
    suspend fun getTotalBalance(): Double?
    
    @Query("SELECT SUM(CAST(currentBalance AS REAL)) FROM accounts WHERE isActive = 1 AND accountType = 'CREDIT_CARD'")
    suspend fun getTotalCreditCardBalance(): Double?
    
    @Query("SELECT * FROM accounts WHERE accountNumber LIKE '%' || :accountNumber || '%'")
    suspend fun searchAccountsByNumber(accountNumber: String): List<AccountEntity>
    
    // Data management methods
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}