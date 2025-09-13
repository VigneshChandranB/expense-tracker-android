package com.expensetracker.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android Keystore operations for secure key generation and encryption/decryption
 */
@Singleton
class KeystoreManager @Inject constructor() {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_DATABASE = "expense_tracker_db_key"
        private const val KEY_ALIAS_PREFERENCES = "expense_tracker_prefs_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    /**
     * Generates or retrieves the database encryption key
     */
    fun getDatabaseKey(): SecretKey {
        return getOrCreateKey(KEY_ALIAS_DATABASE)
    }
    
    /**
     * Generates or retrieves the preferences encryption key
     */
    fun getPreferencesKey(): SecretKey {
        return getOrCreateKey(KEY_ALIAS_PREFERENCES)
    }
    
    /**
     * Encrypts data using the specified key alias
     */
    fun encrypt(data: ByteArray, keyAlias: String): EncryptedData {
        val secretKey = getOrCreateKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        return EncryptedData(encryptedData, iv)
    }
    
    /**
     * Decrypts data using the specified key alias
     */
    fun decrypt(encryptedData: EncryptedData, keyAlias: String): ByteArray {
        val secretKey = getOrCreateKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        return cipher.doFinal(encryptedData.data)
    }
    
    /**
     * Rotates the encryption key for the specified alias
     */
    fun rotateKey(keyAlias: String): SecretKey {
        // Delete existing key
        keyStore.deleteEntry(keyAlias)
        
        // Generate new key
        return generateKey(keyAlias)
    }
    
    /**
     * Checks if a key exists for the given alias
     */
    fun keyExists(keyAlias: String): Boolean {
        return keyStore.containsAlias(keyAlias)
    }
    
    /**
     * Deletes a key for the given alias
     */
    fun deleteKey(keyAlias: String) {
        if (keyExists(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }
    
    private fun getOrCreateKey(keyAlias: String): SecretKey {
        return if (keyExists(keyAlias)) {
            keyStore.getKey(keyAlias, null) as SecretKey
        } else {
            generateKey(keyAlias)
        }
    }
    
    private fun generateKey(keyAlias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set to true if you want biometric authentication
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}

/**
 * Data class to hold encrypted data and initialization vector
 */
data class EncryptedData(
    val data: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!data.contentEquals(other.data)) return false
        if (!iv.contentEquals(other.iv)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}