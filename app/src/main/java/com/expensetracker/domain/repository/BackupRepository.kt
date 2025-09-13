package com.expensetracker.domain.repository

import com.expensetracker.domain.model.BackupData
import com.expensetracker.domain.model.BackupMetadata
import com.expensetracker.domain.model.BackupResult
import com.expensetracker.domain.model.RestoreResult
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

/**
 * Repository interface for backup and restore operations
 */
interface BackupRepository {
    
    /**
     * Creates a backup of all application data
     */
    suspend fun createBackup(): BackupResult
    
    /**
     * Creates a backup to a specific output stream
     */
    suspend fun createBackup(outputStream: OutputStream): BackupResult
    
    /**
     * Restores data from a backup file
     */
    suspend fun restoreFromBackup(inputStream: InputStream): RestoreResult
    
    /**
     * Lists available backup files
     */
    suspend fun getAvailableBackups(): List<BackupMetadata>
    
    /**
     * Validates a backup file without restoring
     */
    suspend fun validateBackup(inputStream: InputStream): RestoreResult
    
    /**
     * Deletes a backup file
     */
    suspend fun deleteBackup(fileName: String): Boolean
    
    /**
     * Gets backup metadata without reading the full file
     */
    suspend fun getBackupMetadata(inputStream: InputStream): BackupMetadata?
    
    /**
     * Observes backup progress
     */
    fun observeBackupProgress(): Flow<Int>
    
    /**
     * Observes restore progress
     */
    fun observeRestoreProgress(): Flow<Int>
}