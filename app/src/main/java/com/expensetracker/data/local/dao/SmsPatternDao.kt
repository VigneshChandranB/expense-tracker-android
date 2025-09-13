package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entities.SmsPatternEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SMS Pattern operations
 */
@Dao
interface SmsPatternDao {
    
    @Query("SELECT * FROM sms_patterns ORDER BY bankName ASC")
    fun observeAllSmsPatterns(): Flow<List<SmsPatternEntity>>
    
    @Query("SELECT * FROM sms_patterns WHERE isActive = 1 ORDER BY bankName ASC")
    fun observeActiveSmsPatterns(): Flow<List<SmsPatternEntity>>
    
    @Query("SELECT * FROM sms_patterns WHERE id = :id")
    suspend fun getSmsPatternById(id: Long): SmsPatternEntity?
    
    @Query("SELECT * FROM sms_patterns WHERE bankName = :bankName AND isActive = 1")
    suspend fun getSmsPatternsByBank(bankName: String): List<SmsPatternEntity>
    
    @Query("SELECT * FROM sms_patterns")
    suspend fun getAllSmsPatterns(): List<SmsPatternEntity>
    
    @Query("SELECT * FROM sms_patterns WHERE isActive = 1")
    suspend fun getActiveSmsPatterns(): List<SmsPatternEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsPattern(smsPattern: SmsPatternEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsPatterns(smsPatterns: List<SmsPatternEntity>): List<Long>
    
    @Update
    suspend fun updateSmsPattern(smsPattern: SmsPatternEntity)
    
    @Delete
    suspend fun deleteSmsPattern(smsPattern: SmsPatternEntity)
    
    @Query("DELETE FROM sms_patterns WHERE id = :id")
    suspend fun deleteSmsPatternById(id: Long)
    
    @Query("UPDATE sms_patterns SET isActive = :isActive WHERE id = :id")
    suspend fun updateSmsPatternStatus(id: Long, isActive: Boolean)
}