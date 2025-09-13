package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.model.BackupData
import com.expensetracker.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for BackupRestoreUseCase
 */
class BackupRestoreUseCaseTest {
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: BackupRestoreUseCase
    
    @Before
    fun setup() {
        settingsRepository = mockk()
        useCase = BackupRestoreUseCase(settingsRepository)
    }
    
    @Test
    fun `createBackup returns backup data from repository`() = runTest {
        // Given
        val expectedBackupData = BackupData(
            version = "1.0",
            timestamp = System.currentTimeMillis(),
            transactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            settings = null
        )
        coEvery { settingsRepository.createBackup() } returns expectedBackupData
        
        // When
        val result = useCase.createBackup()
        
        // Then
        assertEquals(expectedBackupData, result)
        coVerify { settingsRepository.createBackup() }
    }
    
    @Test
    fun `restoreFromBackup calls repository with backup data`() = runTest {
        // Given
        val backupData = BackupData(
            version = "1.0",
            timestamp = System.currentTimeMillis(),
            transactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            settings = null
        )
        coEvery { settingsRepository.restoreFromBackup(backupData) } returns Unit
        
        // When
        useCase.restoreFromBackup(backupData)
        
        // Then
        coVerify { settingsRepository.restoreFromBackup(backupData) }
    }
    
    @Test
    fun `exportBackupToFile creates file successfully`() = runTest {
        // Given
        val backupData = BackupData(
            version = "1.0",
            timestamp = System.currentTimeMillis(),
            transactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            settings = null
        )
        val file = File.createTempFile("backup", ".json")
        coEvery { settingsRepository.exportBackupToFile(backupData, file) } returns Unit
        
        // When
        useCase.exportBackupToFile(backupData, file)
        
        // Then
        coVerify { settingsRepository.exportBackupToFile(backupData, file) }
        
        // Cleanup
        file.delete()
    }
    
    @Test
    fun `importBackupFromFile reads file successfully`() = runTest {
        // Given
        val file = File.createTempFile("backup", ".json")
        val expectedBackupData = BackupData(
            version = "1.0",
            timestamp = System.currentTimeMillis(),
            transactions = emptyList(),
            accounts = emptyList(),
            categories = emptyList(),
            settings = null
        )
        coEvery { settingsRepository.importBackupFromFile(file) } returns expectedBackupData
        
        // When
        val result = useCase.importBackupFromFile(file)
        
        // Then
        assertEquals(expectedBackupData, result)
        coVerify { settingsRepository.importBackupFromFile(file) }
        
        // Cleanup
        file.delete()
    }
}