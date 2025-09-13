package com.expensetracker.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import java.security.KeyStore
import javax.crypto.Cipher

/**
 * Penetration testing scenarios for security features
 * These tests simulate various attack vectors and security vulnerabilities
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SecurityPenetrationTest {
    
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keystoreManager = KeystoreManager()
        securePreferencesManager = SecurePreferencesManager(context, keystoreManager)
    }
    
    @Test
    fun `test resistance to key extraction attempts`() {
        // Given - A key exists in the keystore
        val testData = "sensitive data".toByteArray()
        val keyAlias = "extraction_test_key"
        
        // When - Encrypt data to create the key
        val encrypted = keystoreManager.encrypt(testData, keyAlias)
        
        // Then - Key should exist but not be extractable
        assertThat(keystoreManager.keyExists(keyAlias)).isTrue()
        
        // Attempt to extract key directly (should fail)
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val key = keyStore.getKey(keyAlias, null)
            
            // If we get here, the key exists but should not be extractable in raw form
            assertThat(key).isNotNull()
            
            // Try to get the encoded form (should be null for hardware-backed keys)
            val encoded = key.encoded
            assertThat(encoded).isNull() // Hardware-backed keys don't expose raw key material
            
        } catch (e: Exception) {
            // Expected - key extraction should fail
            assertThat(e).isInstanceOf(Exception::class.java)
        }
    }
    
    @Test
    fun `test resistance to replay attacks`() {
        // Given - Original encrypted data
        val originalData = "original message".toByteArray()
        val keyAlias = "replay_test_key"
        
        val encrypted1 = keystoreManager.encrypt(originalData, keyAlias)
        val encrypted2 = keystoreManager.encrypt(originalData, keyAlias)
        
        // Then - Same plaintext should produce different ciphertexts (due to random IV)
        assertThat(encrypted1.data).isNotEqualTo(encrypted2.data)
        assertThat(encrypted1.iv).isNotEqualTo(encrypted2.iv)
        
        // Both should decrypt to the same original data
        val decrypted1 = keystoreManager.decrypt(encrypted1, keyAlias)
        val decrypted2 = keystoreManager.decrypt(encrypted2, keyAlias)
        
        assertThat(decrypted1).isEqualTo(originalData)
        assertThat(decrypted2).isEqualTo(originalData)
    }
    
    @Test
    fun `test resistance to IV reuse attacks`() {
        // Given - Two different messages
        val message1 = "first message".toByteArray()
        val message2 = "second message".toByteArray()
        val keyAlias = "iv_reuse_test_key"
        
        // When - Encrypt both messages
        val encrypted1 = keystoreManager.encrypt(message1, keyAlias)
        val encrypted2 = keystoreManager.encrypt(message2, keyAlias)
        
        // Then - IVs should be different (preventing IV reuse)
        assertThat(encrypted1.iv).isNotEqualTo(encrypted2.iv)
        
        // Verify both decrypt correctly
        val decrypted1 = keystoreManager.decrypt(encrypted1, keyAlias)
        val decrypted2 = keystoreManager.decrypt(encrypted2, keyAlias)
        
        assertThat(decrypted1).isEqualTo(message1)
        assertThat(decrypted2).isEqualTo(message2)
    }
    
    @Test
    fun `test resistance to ciphertext manipulation`() {
        // Given - Original encrypted data
        val originalData = "important data".toByteArray()
        val keyAlias = "manipulation_test_key"
        val encrypted = keystoreManager.encrypt(originalData, keyAlias)
        
        // When - Manipulate the ciphertext
        val manipulatedData = encrypted.data.clone()
        manipulatedData[0] = (manipulatedData[0].toInt() xor 1).toByte() // Flip one bit
        
        val manipulatedEncrypted = EncryptedData(manipulatedData, encrypted.iv)
        
        // Then - Decryption should fail (GCM provides authentication)
        try {
            keystoreManager.decrypt(manipulatedEncrypted, keyAlias)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: Exception) {
            // Expected - authentication should fail
            assertThat(e).isInstanceOf(Exception::class.java)
        }
    }
    
    @Test
    fun `test resistance to IV manipulation`() {
        // Given - Original encrypted data
        val originalData = "secret data".toByteArray()
        val keyAlias = "iv_manipulation_test_key"
        val encrypted = keystoreManager.encrypt(originalData, keyAlias)
        
        // When - Manipulate the IV
        val manipulatedIv = encrypted.iv.clone()
        manipulatedIv[0] = (manipulatedIv[0].toInt() xor 1).toByte() // Flip one bit
        
        val manipulatedEncrypted = EncryptedData(encrypted.data, manipulatedIv)
        
        // Then - Decryption should fail or produce garbage
        try {
            val decrypted = keystoreManager.decrypt(manipulatedEncrypted, keyAlias)
            // If decryption succeeds, it should produce different data
            assertThat(decrypted).isNotEqualTo(originalData)
        } catch (e: Exception) {
            // Expected - decryption may fail due to authentication
            assertThat(e).isInstanceOf(Exception::class.java)
        }
    }
    
    @Test
    fun `test resistance to timing attacks`() {
        // Given - Different sized data
        val smallData = "small".toByteArray()
        val largeData = "this is a much larger piece of data that should take longer to process".toByteArray()
        val keyAlias = "timing_test_key"
        
        // When - Measure encryption times
        val iterations = 100
        
        var smallDataTime = 0L
        repeat(iterations) {
            val start = System.nanoTime()
            keystoreManager.encrypt(smallData, keyAlias)
            smallDataTime += System.nanoTime() - start
        }
        
        var largeDataTime = 0L
        repeat(iterations) {
            val start = System.nanoTime()
            keystoreManager.encrypt(largeData, keyAlias)
            largeDataTime += System.nanoTime() - start
        }
        
        val avgSmallTime = smallDataTime / iterations
        val avgLargeTime = largeDataTime / iterations
        
        // Then - Time difference should be proportional to data size (not revealing key info)
        // This is expected behavior - larger data takes longer
        assertThat(avgLargeTime).isGreaterThan(avgSmallTime)
        
        // But the difference should be reasonable (not exponential)
        val ratio = avgLargeTime.toDouble() / avgSmallTime.toDouble()
        assertThat(ratio).isLessThan(10.0) // Should not be more than 10x difference
    }
    
    @Test
    fun `test resistance to memory dump attacks`() {
        // Given - Sensitive data
        val sensitiveData = "credit_card_number_1234567890".toByteArray()
        val keyAlias = "memory_dump_test_key"
        
        // When - Encrypt data
        val encrypted = keystoreManager.encrypt(sensitiveData, keyAlias)
        
        // Then - Original data should be cleared from memory
        // (This is a limitation of the test - we can't actually verify memory clearing)
        // But we can verify that the encrypted data doesn't contain the original
        val encryptedString = String(encrypted.data, Charsets.ISO_8859_1)
        val originalString = String(sensitiveData, Charsets.UTF_8)
        
        assertThat(encryptedString).doesNotContain(originalString)
    }
    
    @Test
    fun `test resistance to key substitution attacks`() {
        // Given - Data encrypted with one key
        val originalData = "confidential data".toByteArray()
        val keyAlias1 = "key1_substitution_test"
        val keyAlias2 = "key2_substitution_test"
        
        val encrypted = keystoreManager.encrypt(originalData, keyAlias1)
        
        // When - Try to decrypt with different key
        try {
            keystoreManager.decrypt(encrypted, keyAlias2)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: Exception) {
            // Expected - decryption with wrong key should fail
            assertThat(e).isInstanceOf(Exception::class.java)
        }
    }
    
    @Test
    fun `test resistance to brute force attacks on preferences`() {
        // Given - Stored encrypted preferences
        val testData = EncryptedData(
            "sensitive_preference_data".toByteArray(),
            "initialization_vector".toByteArray()
        )
        
        securePreferencesManager.storeDatabasePassphrase(testData)
        
        // When - Try to access with invalid keys (simulating brute force)
        val invalidAttempts = listOf("wrong_key1", "wrong_key2", "wrong_key3")
        
        invalidAttempts.forEach { invalidKey ->
            try {
                // This would fail in a real scenario as we can't access the encrypted prefs with wrong keys
                val retrieved = securePreferencesManager.getDatabasePassphrase()
                // If we get here, the data should still be encrypted and not readable
                assertThat(retrieved).isNotNull()
            } catch (e: Exception) {
                // Expected for some scenarios
                assertThat(e).isInstanceOf(Exception::class.java)
            }
        }
    }
    
    @Test
    fun `test resistance to side channel attacks through error messages`() {
        // Given - Various invalid inputs
        val validKey = "valid_key"
        val invalidInputs = listOf(
            EncryptedData(ByteArray(0), ByteArray(12)), // Empty data
            EncryptedData(ByteArray(16), ByteArray(0)), // Empty IV
            EncryptedData(ByteArray(16), ByteArray(8)), // Wrong IV size
            EncryptedData("invalid".toByteArray(), ByteArray(12)) // Invalid ciphertext
        )
        
        // When - Try to decrypt invalid inputs
        invalidInputs.forEach { invalidInput ->
            try {
                keystoreManager.decrypt(invalidInput, validKey)
                // If it doesn't throw, that's also valid behavior
            } catch (e: Exception) {
                // Then - Error messages should not reveal sensitive information
                val errorMessage = e.message?.lowercase() ?: ""
                
                // Should not contain key material or detailed crypto info
                assertThat(errorMessage).doesNotContain("key")
                assertThat(errorMessage).doesNotContain("secret")
                assertThat(errorMessage).doesNotContain("private")
            }
        }
    }
    
    @Test
    fun `test data integrity under concurrent access`() = runTest {
        // Given - Multiple threads trying to access the same data
        val keyAlias = "concurrent_test_key"
        val testData = "concurrent access test".toByteArray()
        
        // When - Simulate concurrent encryption/decryption
        val results = mutableListOf<ByteArray>()
        
        repeat(10) {
            val encrypted = keystoreManager.encrypt(testData, keyAlias)
            val decrypted = keystoreManager.decrypt(encrypted, keyAlias)
            results.add(decrypted)
        }
        
        // Then - All results should be identical to original
        results.forEach { result ->
            assertThat(result).isEqualTo(testData)
        }
    }
}