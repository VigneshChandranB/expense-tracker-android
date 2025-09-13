package com.expensetracker.domain.usecase.security

import android.content.Context
import com.expensetracker.data.security.DatabaseEncryptionManager
import com.expensetracker.data.security.DataIntegrityValidator
import com.expensetracker.data.security.KeystoreManager
import com.expensetracker.data.security.SecurePreferencesManager
import com.expensetracker.data.security.DatabaseIntegrityResult
import com.expensetracker.data.security.DataIntegrityReport
import com.expensetracker.data.security.IntegrityLevel
import com.expensetracker.data.security.RepairResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for SecurityManagerUseCase
 */
class SecurityManagerUseCaseTest {
    
    private lateinit var securityManagerUseCase: SecurityManagerUseCase
    private val mockKeystoreManager = mockk<KeystoreManager>(relaxed = true)
    private val mockDatabaseEncryptionManager = mockk<DatabaseEncryptionManager>(relaxed = true)
    private val mockDataIntegrityValidator = mockk<DataIntegrityValidator>(relaxed = true)
    private val mockSecurePreferencesManager = mockk<SecurePreferencesManager>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)
    
    @Before
    fun setup() {
        securityManagerUseCase = SecurityManagerUseCase(
            mockKeystoreManager,
            mockDatabaseEncryptionManager,
            mockDataIntegrityValidator,
            mockSecurePreferencesManager
        )
    }
    
    @Test
    fun `performSecurityCheck returns successful result for valid system`() = runTest {
        // Given
        val mockDataIntegrityReport = DataIntegrityReport(
            overallIntegrity = IntegrityLevel.GOOD
        )
        
        coEvery { mockDatabaseEncryptionManager.validateDatabaseIntegrity(mockContext) } returns DatabaseIntegrityResult.VALID
        coEvery { mockDataIntegrityValidator.validateAllData() } returns mockDataIntegrityReport
        every { mockSecurePreferencesManager.validateIntegrity() } returns true
        coEvery { mockDataIntegrityValidator.generateDataHash() } returns "test_hash_123"
        every { mockSecurePreferencesManager.getLastBackupHash() } returns "test_hash_123"
        coEvery { mockDataIntegrityValidator.verifyDataIntegrity("test_hash_123") } returns true
        
        // When
        val result = securityManagerUseCase.performSecurityCheck(mockContext)
        
        // Then
        assertThat(result.success).isTrue()
        assertThat(result.databaseIntegrity).isEqualTo(DatabaseIntegrityResult.VALID)
        assertThat(result.dataIntegrityReport.overallIntegrity).isEqualTo(IntegrityLevel.GOOD)
        assertThat(result.preferencesIntegrity).isTrue()
        assertThat(result.currentDataHash).isEqualTo("test_hash_123")
        assertThat(result.dataUnchanged).isTrue()
        assertThat(result.error).isNull()
    }
    
    @Test
    fun `performSecurityCheck handles database integrity failure`() = runTest {
        // Given
        coEvery { mockDatabaseEncryptionManager.validateDatabaseIntegrity(mockContext) } returns DatabaseIntegrityResult.CORRUPTED
        coEvery { mockDataIntegrityValidator.validateAllData() } returns DataIntegrityReport()
        every { mockSecurePreferencesManager.validateIntegrity() } returns true
        coEvery { mockDataIntegrityValidator.generateDataHash() } returns "test_hash"
        every { mockSecurePreferencesManager.getLastBackupHash() } returns null
        
        // When
        val result = securityManagerUseCase.performSecurityCheck(mockContext)
        
        // Then
        assertThat(result.success).isTrue()
        assertThat(result.databaseIntegrity).isEqualTo(DatabaseIntegrityResult.CORRUPTED)
        assertThat(result.dataUnchanged).isNull() // No previous hash to compare
    }
    
    @Test
    fun `performSecurityCheck handles exception gracefully`() = runTest {
        // Given
        coEvery { mockDatabaseEncryptionManager.validateDatabaseIntegrity(mockContext) } throws RuntimeException("Test error")
        
        // When
        val result = securityManagerUseCase.performSecurityCheck(mockContext)
        
        // Then
        assertThat(result.success).isFalse()
        assertThat(result.error).isEqualTo("Test error")
    }
    
    @Test
    fun `rotateAllKeys successfully rotates all keys`() = runTest {
        // Given
        coEvery { mockDatabaseEncryptionManager.rotateDatabaseKey(mockContext) } returns true
        every { mockKeystoreManager.keyExists("expense_tracker_prefs_key") } returns true
        every { mockKeystoreManager.rotateKey("expense_tracker_prefs_key") } returns mockk()
        
        // When
        val result = securityManagerUseCase.rotateAllKeys(mockContext)
        
        // Then
        assertThat(result.success).isTrue()
        assertThat(result.databaseKeyRotated).isTrue()
        assertThat(result.preferencesKeyRotated).isTrue()
        assertThat(result.error).isNull()
        
        verify { mockKeystoreManager.rotateKey("expense_tracker_prefs_key") }
    }
    
    @Test
    fun `rotateAllKeys handles database key rotation failure`() = runTest {
        // Given
        coEvery { mockDatabaseEncryptionManager.rotateDatabaseKey(mockContext) } returns false
        every { mockKeystoreManager.keyExists("expense_tracker_prefs_key") } returns true
        every { mockKeystoreManager.rotateKey("expense_tracker_prefs_key") } returns mockk()
        
        // When
        val result = securityManagerUseCase.rotateAllKeys(mockContext)
        
        // Then
        assertThat(result.success).isFalse()
        assertThat(result.databaseKeyRotated).isFalse()
        assertThat(result.preferencesKeyRotated).isTrue()
    }
    
    @Test
    fun `rotateAllKeys handles missing preferences key`() = runTest {
        // Given
        coEvery { mockDatabaseEncryptionManager.rotateDatabaseKey(mockContext) } returns true
        every { mockKeystoreManager.keyExists("expense_tracker_prefs_key") } returns false
        
        // When
        val result = securityManagerUseCase.rotateAllKeys(mockContext)
        
        // Then
        assertThat(result.success).isFalse()
        assertThat(result.databaseKeyRotated).isTrue()
        assertThat(result.preferencesKeyRotated).isFalse()
        
        verify(exactly = 0) { mockKeystoreManager.rotateKey("expense_tracker_prefs_key") }
    }
    
    @Test
    fun `repairDataIntegrity delegates to validator`() = runTest {
        // Given
        val mockRepairResult = RepairResult(
            success = true,
            repairedBalances = 2,
            removedOrphanedRecords = 1
        )
        coEvery { mockDataIntegrityValidator.repairIntegrityIssues() } returns mockRepairResult
        
        // When
        val result = securityManagerUseCase.repairDataIntegrity()
        
        // Then
        assertThat(result).isEqualTo(mockRepairResult)
        coVerify { mockDataIntegrityValidator.repairIntegrityIssues() }
    }
    
    @Test
    fun `performSecureWipe successfully wipes all data`() = runTest {
        // Given
        coEvery { mockDatabaseEncryptionManager.secureWipeDatabase(mockContext) } returns true
        every { mockKeystoreManager.keyExists("expense_tracker_db_key") } returns true
        every { mockKeystoreManager.keyExists("expense_tracker_prefs_key") } returns true
        every { mockKeystoreManager.deleteKey(any()) } returns Unit
        every { mockSecurePreferencesManager.secureWipe() } returns Unit
        
        // When
        val result = securityManagerUseCase.performSecureWipe(mockContext)
        
        // Then
        assertThat(result.success).isTrue()
        assertThat(result.databaseWiped).isTrue()
        assertThat(result.preferencesWiped).isTrue()
        assertThat(result.keystoreWiped).isTrue()
        assertThat(result.error).isNull()
        
        verify { mockSecurePreferencesManager.secureWipe() }
        verify { mockKeystoreManager.deleteKey("expense_tracker_db_key") }
        verify { mockKeystoreManager.deleteKey("expense_tracker_prefs_key") }
    }
    
    @Test
    fun `performSecureWipe handles database wipe failure`() = runTest {
        // Given
        coEvery { mockDatabaseEncryptionManager.secureWipeDatabase(mockContext) } returns false
        every { mockSecurePreferencesManager.secureWipe() } returns Unit
        every { mockKeystoreManager.keyExists(any()) } returns false
        
        // When
        val result = securityManagerUseCase.performSecureWipe(mockContext)
        
        // Then
        assertThat(result.success).isFalse()
        assertThat(result.databaseWiped).isFalse()
        assertThat(result.preferencesWiped).isTrue()
        assertThat(result.keystoreWiped).isTrue()
    }
    
    @Test
    fun `updateDataIntegrityHash generates and stores hash`() = runTest {
        // Given
        coEvery { mockDataIntegrityValidator.generateDataHash() } returns "new_hash_456"
        every { mockSecurePreferencesManager.setLastBackupHash("new_hash_456") } returns Unit
        
        // When
        securityManagerUseCase.updateDataIntegrityHash()
        
        // Then
        coVerify { mockDataIntegrityValidator.generateDataHash() }
        verify { mockSecurePreferencesManager.setLastBackupHash("new_hash_456") }
    }
    
    @Test
    fun `biometric authentication methods work correctly`() {
        // Given
        every { mockSecurePreferencesManager.setBiometricEnabled(true) } returns Unit
        every { mockSecurePreferencesManager.isBiometricEnabled() } returns true
        
        // When
        securityManagerUseCase.setBiometricAuthentication(true)
        val isEnabled = securityManagerUseCase.isBiometricEnabled()
        
        // Then
        assertThat(isEnabled).isTrue()
        verify { mockSecurePreferencesManager.setBiometricEnabled(true) }
        verify { mockSecurePreferencesManager.isBiometricEnabled() }
    }
    
    @Test
    fun `auto lock timeout methods work correctly`() {
        // Given
        every { mockSecurePreferencesManager.setAutoLockTimeout(10) } returns Unit
        every { mockSecurePreferencesManager.getAutoLockTimeout() } returns 10
        
        // When
        securityManagerUseCase.setAutoLockTimeout(10)
        val timeout = securityManagerUseCase.getAutoLockTimeout()
        
        // Then
        assertThat(timeout).isEqualTo(10)
        verify { mockSecurePreferencesManager.setAutoLockTimeout(10) }
        verify { mockSecurePreferencesManager.getAutoLockTimeout() }
    }
    
    @Test
    fun `validateSecuritySetup returns valid for properly setup system`() = runTest {
        // Given
        every { mockKeystoreManager.keyExists("expense_tracker_db_key") } returns true
        every { mockKeystoreManager.keyExists("expense_tracker_prefs_key") } returns true
        coEvery { mockDatabaseEncryptionManager.validateDatabaseIntegrity(mockContext) } returns DatabaseIntegrityResult.VALID
        every { mockSecurePreferencesManager.validateIntegrity() } returns true
        
        // When
        val validation = securityManagerUseCase.validateSecuritySetup(mockContext)
        
        // Then
        assertThat(validation.isValid).isTrue()
        assertThat(validation.databaseKeyExists).isTrue()
        assertThat(validation.preferencesKeyExists).isTrue()
        assertThat(validation.databaseAccessible).isTrue()
        assertThat(validation.preferencesAccessible).isTrue()
        assertThat(validation.error).isNull()
    }
    
    @Test
    fun `validateSecuritySetup detects missing keys`() = runTest {
        // Given
        every { mockKeystoreManager.keyExists("expense_tracker_db_key") } returns false
        every { mockKeystoreManager.keyExists("expense_tracker_prefs_key") } returns true
        coEvery { mockDatabaseEncryptionManager.validateDatabaseIntegrity(mockContext) } returns DatabaseIntegrityResult.VALID
        every { mockSecurePreferencesManager.validateIntegrity() } returns true
        
        // When
        val validation = securityManagerUseCase.validateSecuritySetup(mockContext)
        
        // Then
        assertThat(validation.isValid).isFalse()
        assertThat(validation.databaseKeyExists).isFalse()
        assertThat(validation.preferencesKeyExists).isTrue()
    }
    
    @Test
    fun `validateSecuritySetup handles database access error`() = runTest {
        // Given
        every { mockKeystoreManager.keyExists("expense_tracker_db_key") } returns true
        every { mockKeystoreManager.keyExists("expense_tracker_prefs_key") } returns true
        coEvery { mockDatabaseEncryptionManager.validateDatabaseIntegrity(mockContext) } returns DatabaseIntegrityResult.ACCESS_ERROR
        every { mockSecurePreferencesManager.validateIntegrity() } returns true
        
        // When
        val validation = securityManagerUseCase.validateSecuritySetup(mockContext)
        
        // Then
        assertThat(validation.isValid).isFalse()
        assertThat(validation.databaseAccessible).isFalse()
    }
}