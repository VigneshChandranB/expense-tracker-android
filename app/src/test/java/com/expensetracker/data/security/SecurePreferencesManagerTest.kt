package com.expensetracker.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk

/**
 * Unit tests for SecurePreferencesManager
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SecurePreferencesManagerTest {
    
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var context: Context
    private val mockKeystoreManager = mockk<KeystoreManager>(relaxed = true)
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securePreferencesManager = SecurePreferencesManager(context, mockKeystoreManager)
    }
    
    @Test
    fun `store and retrieve database passphrase`() {
        // Given
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        val testIv = byteArrayOf(6, 7, 8, 9, 10)
        val encryptedData = EncryptedData(testData, testIv)
        
        // When
        securePreferencesManager.storeDatabasePassphrase(encryptedData)
        val retrieved = securePreferencesManager.getDatabasePassphrase()
        
        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.data).isEqualTo(testData)
        assertThat(retrieved?.iv).isEqualTo(testIv)
    }
    
    @Test
    fun `clear database passphrase removes stored data`() {
        // Given
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        val testIv = byteArrayOf(6, 7, 8, 9, 10)
        val encryptedData = EncryptedData(testData, testIv)
        securePreferencesManager.storeDatabasePassphrase(encryptedData)
        
        // When
        securePreferencesManager.clearDatabasePassphrase()
        val retrieved = securePreferencesManager.getDatabasePassphrase()
        
        // Then
        assertThat(retrieved).isNull()
    }
    
    @Test
    fun `biometric enabled preference works correctly`() {
        // Given
        assertThat(securePreferencesManager.isBiometricEnabled()).isFalse() // Default
        
        // When
        securePreferencesManager.setBiometricEnabled(true)
        
        // Then
        assertThat(securePreferencesManager.isBiometricEnabled()).isTrue()
        
        // When
        securePreferencesManager.setBiometricEnabled(false)
        
        // Then
        assertThat(securePreferencesManager.isBiometricEnabled()).isFalse()
    }
    
    @Test
    fun `auto lock timeout preference works correctly`() {
        // Given
        assertThat(securePreferencesManager.getAutoLockTimeout()).isEqualTo(5) // Default 5 minutes
        
        // When
        securePreferencesManager.setAutoLockTimeout(10)
        
        // Then
        assertThat(securePreferencesManager.getAutoLockTimeout()).isEqualTo(10)
    }
    
    @Test
    fun `backup hash preference works correctly`() {
        // Given
        assertThat(securePreferencesManager.getLastBackupHash()).isNull() // Default null
        
        // When
        val testHash = "abc123def456"
        securePreferencesManager.setLastBackupHash(testHash)
        
        // Then
        assertThat(securePreferencesManager.getLastBackupHash()).isEqualTo(testHash)
    }
    
    @Test
    fun `secure wipe clears all preferences`() {
        // Given
        securePreferencesManager.setBiometricEnabled(true)
        securePreferencesManager.setAutoLockTimeout(15)
        securePreferencesManager.setLastBackupHash("test_hash")
        
        val testData = byteArrayOf(1, 2, 3)
        val testIv = byteArrayOf(4, 5, 6)
        securePreferencesManager.storeDatabasePassphrase(EncryptedData(testData, testIv))
        
        // When
        securePreferencesManager.secureWipe()
        
        // Then
        assertThat(securePreferencesManager.isBiometricEnabled()).isFalse()
        assertThat(securePreferencesManager.getAutoLockTimeout()).isEqualTo(5) // Back to default
        assertThat(securePreferencesManager.getLastBackupHash()).isNull()
        assertThat(securePreferencesManager.getDatabasePassphrase()).isNull()
    }
    
    @Test
    fun `validate integrity returns true for valid preferences`() {
        // Given
        securePreferencesManager.setBiometricEnabled(true)
        securePreferencesManager.setAutoLockTimeout(10)
        
        // When
        val isValid = securePreferencesManager.validateIntegrity()
        
        // Then
        assertThat(isValid).isTrue()
    }
    
    @Test
    fun `get database passphrase returns null when not stored`() {
        // When
        val retrieved = securePreferencesManager.getDatabasePassphrase()
        
        // Then
        assertThat(retrieved).isNull()
    }
    
    @Test
    fun `store multiple different encrypted data`() {
        // Given
        val data1 = EncryptedData(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
        val data2 = EncryptedData(byteArrayOf(7, 8, 9), byteArrayOf(10, 11, 12))
        
        // When
        securePreferencesManager.storeDatabasePassphrase(data1)
        val retrieved1 = securePreferencesManager.getDatabasePassphrase()
        
        securePreferencesManager.storeDatabasePassphrase(data2)
        val retrieved2 = securePreferencesManager.getDatabasePassphrase()
        
        // Then
        assertThat(retrieved1).isEqualTo(data1)
        assertThat(retrieved2).isEqualTo(data2)
        assertThat(retrieved1).isNotEqualTo(retrieved2)
    }
}