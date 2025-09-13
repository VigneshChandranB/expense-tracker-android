package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.model.BackupMetadata
import com.expensetracker.domain.model.BackupResult
import com.expensetracker.domain.model.RestoreResult
import com.expensetracker.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * Use case for backup and restore operations
 */
class BackupRestoreUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    
    /**
     * Creates a backup of all application data
     */
    suspend fun createBackup(): BackupResult {
        return backupRepository.createBackup()
    }
    
    /**
     * Creates a backup to a specific output stream (for sharing)
     */
    suspend fun createBackup(outputStream: OutputStream): BackupResult {
        return backupRepository.createBackup(outputStream)
    }
    
    /**
     * Restores data from a backup
     */
    suspend fun restoreFromBackup(inputStream: InputStream): RestoreResult {
        // Validate backup first
        val validationResult = backupRepository.validateBackup(inputStream)
        if (validationResult is RestoreResult.Error || validationResult is RestoreResult.ValidationError) {
            return validationResult
        }
        
        // Proceed with restore
        return backupRepository.restoreFromBackup(inputStream)
    }
    
    /**
     * Gets list of available backups
     */
    suspend fun getAvailableBackups(): List<BackupMetadata> {
        return backupRepository.getAvailableBackups()
    }
    
    /**
     * Validates a backup file
     */
    suspend fun validateBackup(inputStream: InputStream): RestoreResult {
        return backupRepository.validateBackup(inputStream)
    }
    
    /**
     * Deletes a backup file
     */
    suspend fun deleteBackup(fileName: String): Boolean {
        return backupRepository.deleteBackup(fileName)
    }
    
    /**
     * Gets backup metadata
     */
    suspend fun getBackupMetadata(inputStream: InputStream): BackupMetadata? {
        return backupRepository.getBackupMetadata(inputStream)
    }
    
    /**
     * Observes backup progress
     */
    fun observeBackupProgress(): Flow<Int> {
        return backupRepository.observeBackupProgress()
    }
    
    /**
     * Observes restore progress
     */
    fun observeRestoreProgress(): Flow<Int> {
        return backupRepository.observeRestoreProgress()
    }
}