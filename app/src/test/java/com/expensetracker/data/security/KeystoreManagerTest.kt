package com.expensetracker.data.security

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat
import org.junit.After

/**
 * Unit tests for KeystoreManager
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class KeystoreManagerTest {
    
    private lateinit var keystoreManager: KeystoreManager
    private val testKeyAlias = "test_key_alias"
    
    @Before
    fun setup() {
        keystoreManager = KeystoreManager()
    }
    
    @After
    fun cleanup() {
        // Clean up test keys
        if (keystoreManager.keyExists(testKeyAlias)) {
            keystoreManager.deleteKey(testKeyAlias)
        }
    }
    
    @Test
    fun `encrypt and decrypt data successfully`() {
        // Given
        val originalData = "Test sensitive data".toByteArray()
        
        // When
        val encryptedData = keystoreManager.encrypt(originalData, testKeyAlias)
        val decryptedData = keystoreManager.decrypt(encryptedData, testKeyAlias)
        
        // Then
        assertThat(decryptedData).isEqualTo(originalData)
        assertThat(encryptedData.data).isNotEqualTo(originalData)
        assertThat(encryptedData.iv).isNotEmpty()
    }
    
    @Test
    fun `key exists after creation`() {
        // Given
        val testData = "test".toByteArray()
        
        // When
        keystoreManager.encrypt(testData, testKeyAlias)
        
        // Then
        assertThat(keystoreManager.keyExists(testKeyAlias)).isTrue()
    }
    
    @Test
    fun `key does not exist initially`() {
        // Then
        assertThat(keystoreManager.keyExists(testKeyAlias)).isFalse()
    }
    
    @Test
    fun `delete key removes it from keystore`() {
        // Given
        val testData = "test".toByteArray()
        keystoreManager.encrypt(testData, testKeyAlias)
        assertThat(keystoreManager.keyExists(testKeyAlias)).isTrue()
        
        // When
        keystoreManager.deleteKey(testKeyAlias)
        
        // Then
        assertThat(keystoreManager.keyExists(testKeyAlias)).isFalse()
    }
    
    @Test
    fun `rotate key generates new key`() {
        // Given
        val testData = "test data".toByteArray()
        val originalEncrypted = keystoreManager.encrypt(testData, testKeyAlias)
        
        // When
        keystoreManager.rotateKey(testKeyAlias)
        val newEncrypted = keystoreManager.encrypt(testData, testKeyAlias)
        
        // Then
        assertThat(keystoreManager.keyExists(testKeyAlias)).isTrue()
        assertThat(newEncrypted.data).isNotEqualTo(originalEncrypted.data)
        
        // Should be able to decrypt with new key
        val decrypted = keystoreManager.decrypt(newEncrypted, testKeyAlias)
        assertThat(decrypted).isEqualTo(testData)
    }
    
    @Test
    fun `get database key creates key if not exists`() {
        // When
        val databaseKey = keystoreManager.getDatabaseKey()
        
        // Then
        assertThat(databaseKey).isNotNull()
        assertThat(keystoreManager.keyExists("expense_tracker_db_key")).isTrue()
    }
    
    @Test
    fun `get preferences key creates key if not exists`() {
        // When
        val preferencesKey = keystoreManager.getPreferencesKey()
        
        // Then
        assertThat(preferencesKey).isNotNull()
        assertThat(keystoreManager.keyExists("expense_tracker_prefs_key")).isTrue()
    }
    
    @Test
    fun `encrypted data equals method works correctly`() {
        // Given
        val data1 = byteArrayOf(1, 2, 3)
        val iv1 = byteArrayOf(4, 5, 6)
        val data2 = byteArrayOf(1, 2, 3)
        val iv2 = byteArrayOf(4, 5, 6)
        val data3 = byteArrayOf(7, 8, 9)
        
        val encrypted1 = EncryptedData(data1, iv1)
        val encrypted2 = EncryptedData(data2, iv2)
        val encrypted3 = EncryptedData(data3, iv1)
        
        // Then
        assertThat(encrypted1).isEqualTo(encrypted2)
        assertThat(encrypted1).isNotEqualTo(encrypted3)
    }
    
    @Test
    fun `encrypted data hash code works correctly`() {
        // Given
        val data = byteArrayOf(1, 2, 3)
        val iv = byteArrayOf(4, 5, 6)
        
        val encrypted1 = EncryptedData(data, iv)
        val encrypted2 = EncryptedData(data.clone(), iv.clone())
        
        // Then
        assertThat(encrypted1.hashCode()).isEqualTo(encrypted2.hashCode())
    }
    
    @Test
    fun `encrypt different data produces different results`() {
        // Given
        val data1 = "First test data".toByteArray()
        val data2 = "Second test data".toByteArray()
        
        // When
        val encrypted1 = keystoreManager.encrypt(data1, testKeyAlias)
        val encrypted2 = keystoreManager.encrypt(data2, testKeyAlias)
        
        // Then
        assertThat(encrypted1.data).isNotEqualTo(encrypted2.data)
        assertThat(encrypted1.iv).isNotEqualTo(encrypted2.iv) // Should be different due to randomization
    }
    
    @Test
    fun `encrypt same data twice produces different results due to randomization`() {
        // Given
        val data = "Same test data".toByteArray()
        
        // When
        val encrypted1 = keystoreManager.encrypt(data, testKeyAlias)
        val encrypted2 = keystoreManager.encrypt(data, testKeyAlias)
        
        // Then
        assertThat(encrypted1.data).isNotEqualTo(encrypted2.data)
        assertThat(encrypted1.iv).isNotEqualTo(encrypted2.iv)
        
        // But both should decrypt to the same original data
        val decrypted1 = keystoreManager.decrypt(encrypted1, testKeyAlias)
        val decrypted2 = keystoreManager.decrypt(encrypted2, testKeyAlias)
        assertThat(decrypted1).isEqualTo(data)
        assertThat(decrypted2).isEqualTo(data)
    }
}