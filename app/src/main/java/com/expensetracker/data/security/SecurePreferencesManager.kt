package com.expensetracker.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encrypted shared preferences for sensitive data storage
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    private val context: Context,
    private val keystoreManager: KeystoreManager
) {
    
    companion object {
        private const val PREFS_FILE_NAME = "expense_tracker_secure_prefs"
        private const val KEY_DATABASE_PASSPHRASE_DATA = "db_passphrase_data"
        private const val KEY_DATABASE_PASSPHRASE_IV = "db_passphrase_iv"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_LAST_BACKUP_HASH = "last_backup_hash"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Stores the encrypted database passphrase
     */
    fun storeDatabasePassphrase(encryptedData: EncryptedData) {
        encryptedPrefs.edit()
            .putString(KEY_DATABASE_PASSPHRASE_DATA, encryptedData.data.toBase64())
            .putString(KEY_DATABASE_PASSPHRASE_IV, encryptedData.iv.toBase64())
            .apply()
    }
    
    /**
     * Retrieves the encrypted database passphrase
     */
    fun getDatabasePassphrase(): EncryptedData? {
        val dataString = encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE_DATA, null)
        val ivString = encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE_IV, null)
        
        return if (dataString != null && ivString != null) {
            EncryptedData(
                data = dataString.fromBase64(),
                iv = ivString.fromBase64()
            )
        } else {
            null
        }
    }
    
    /**
     * Clears the stored database passphrase
     */
    fun clearDatabasePassphrase() {
        encryptedPrefs.edit()
            .remove(KEY_DATABASE_PASSPHRASE_DATA)
            .remove(KEY_DATABASE_PASSPHRASE_IV)
            .apply()
    }
    
    /**
     * Sets biometric authentication preference
     */
    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Gets biometric authentication preference
     */
    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Sets auto-lock timeout in minutes
     */
    fun setAutoLockTimeout(timeoutMinutes: Int) {
        encryptedPrefs.edit()
            .putInt(KEY_AUTO_LOCK_TIMEOUT, timeoutMinutes)
            .apply()
    }
    
    /**
     * Gets auto-lock timeout in minutes
     */
    fun getAutoLockTimeout(): Int {
        return encryptedPrefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 5) // Default 5 minutes
    }
    
    /**
     * Stores the hash of the last backup for integrity verification
     */
    fun setLastBackupHash(hash: String) {
        encryptedPrefs.edit()
            .putString(KEY_LAST_BACKUP_HASH, hash)
            .apply()
    }
    
    /**
     * Gets the hash of the last backup
     */
    fun getLastBackupHash(): String? {
        return encryptedPrefs.getString(KEY_LAST_BACKUP_HASH, null)
    }
    
    /**
     * Securely wipes all stored preferences
     */
    fun secureWipe() {
        encryptedPrefs.edit().clear().apply()
    }
    
    /**
     * Validates the integrity of stored preferences
     */
    fun validateIntegrity(): Boolean {
        return try {
            // Try to access encrypted preferences
            encryptedPrefs.all
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extension function to convert ByteArray to Base64 string
     */
    private fun ByteArray.toBase64(): String {
        return android.util.Base64.encodeToString(this, android.util.Base64.DEFAULT)
    }
    
    /**
     * Extension function to convert Base64 string to ByteArray
     */
    private fun String.fromBase64(): ByteArray {
        return android.util.Base64.decode(this, android.util.Base64.DEFAULT)
    }
}