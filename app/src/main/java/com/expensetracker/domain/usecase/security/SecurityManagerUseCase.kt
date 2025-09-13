package com.expensetracker.domain.usecase.security

import android.content.Context
import com.expensetracker.data.security.DatabaseEncryptionManager
import com.expensetracker.data.security.DataIntegrityValidator
import com.expensetracker.data.security.KeystoreManager
import com.expensetracker.data.security.SecurePreferencesManager
import com.expensetracker.data.security.DatabaseIntegrityResult
import com.expensetracker.data.security.DataIntegrityReport
import com.expensetracker.data.security.RepairResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing security operations
 */
@Singleton
class SecurityManagerUseCase @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val databaseEncryptionManager: DatabaseEncryptionManager,
    private val dataIntegrityValidator: DataIntegrityValidator,
    private val securePreferencesManager: SecurePreferencesManager
) {
    
    /**
     * Performs a comprehensive security check
     */
    suspend fun performSecurityCheck(context: Context): SecurityCheckResult = withContext(Dispatchers.IO) {
        val result = SecurityCheckResult()
        
        try {
            // Check database integrity
            result.databaseIntegrity = databaseEncryptionManager.validateDatabaseIntegrity(context)
            
            // Check data integrity
            result.dataIntegrityReport = dataIntegrityValidator.validateAllData()
            
            // Check preferences integrity
            result.preferencesIntegrity = securePreferencesManager.validateIntegrity()
            
            // Generate current data hash
            result.currentDataHash = dataIntegrityValidator.generateDataHash()
            
            // Check against previous hash if available
            val previousHash = securePreferencesManager.getLastBackupHash()
            if (previousHash != null) {
                result.dataUnchanged = dataIntegrityValidator.verifyDataIntegrity(previousHash)
            }
            
            result.success = true
            
        } catch (e: Exception) {
            result.success = false
            result.error = e.message
        }
        
        result
    }
    
    /**
     * Rotates all encryption keys
     */
    suspend fun rotateAllKeys(context: Context): KeyRotationResult = withContext(Dispatchers.IO) {
        val result = KeyRotationResult()
        
        try {
            // Rotate database key
            result.databaseKeyRotated = databaseEncryptionManager.rotateDatabaseKey(context)
            
            // Rotate preferences key
            val prefsKeyAlias = "expense_tracker_prefs_key"
            if (keystoreManager.keyExists(prefsKeyAlias)) {
                keystoreManager.rotateKey(prefsKeyAlias)
                result.preferencesKeyRotated = true
            }
            
            result.success = result.databaseKeyRotated && result.preferencesKeyRotated
            
        } catch (e: Exception) {
            result.success = false
            result.error = e.message
        }
        
        result
    }
    
    /**
     * Repairs data integrity issues
     */
    suspend fun repairDataIntegrity(): RepairResult {
        return dataIntegrityValidator.repairIntegrityIssues()
    }
    
    /**
     * Performs secure wipe of all data
     */
    suspend fun performSecureWipe(context: Context): SecureWipeResult = withContext(Dispatchers.IO) {
        val result = SecureWipeResult()
        
        try {
            // Wipe database
            result.databaseWiped = databaseEncryptionManager.secureWipeDatabase(context)
            
            // Wipe preferences
            securePreferencesManager.secureWipe()
            result.preferencesWiped = true
            
            // Delete all keystore keys
            val keyAliases = listOf(
                "expense_tracker_db_key",
                "expense_tracker_prefs_key"
            )
            
            keyAliases.forEach { alias ->
                if (keystoreManager.keyExists(alias)) {
                    keystoreManager.deleteKey(alias)
                }
            }
            result.keystoreWiped = true
            
            result.success = result.databaseWiped && result.preferencesWiped && result.keystoreWiped
            
        } catch (e: Exception) {
            result.success = false
            result.error = e.message
        }
        
        result
    }
    
    /**
     * Updates the data integrity hash
     */
    suspend fun updateDataIntegrityHash() {
        val currentHash = dataIntegrityValidator.generateDataHash()
        securePreferencesManager.setLastBackupHash(currentHash)
    }
    
    /**
     * Enables or disables biometric authentication
     */
    fun setBiometricAuthentication(enabled: Boolean) {
        securePreferencesManager.setBiometricEnabled(enabled)
    }
    
    /**
     * Gets biometric authentication status
     */
    fun isBiometricEnabled(): Boolean {
        return securePreferencesManager.isBiometricEnabled()
    }
    
    /**
     * Sets auto-lock timeout
     */
    fun setAutoLockTimeout(timeoutMinutes: Int) {
        securePreferencesManager.setAutoLockTimeout(timeoutMinutes)
    }
    
    /**
     * Gets auto-lock timeout
     */
    fun getAutoLockTimeout(): Int {
        return securePreferencesManager.getAutoLockTimeout()
    }
    
    /**
     * Validates that all security components are properly initialized
     */
    suspend fun validateSecuritySetup(context: Context): SecuritySetupValidation = withContext(Dispatchers.IO) {
        val validation = SecuritySetupValidation()
        
        try {
            // Check if database key exists
            validation.databaseKeyExists = keystoreManager.keyExists("expense_tracker_db_key")
            
            // Check if preferences key exists
            validation.preferencesKeyExists = keystoreManager.keyExists("expense_tracker_prefs_key")
            
            // Check if database is accessible
            validation.databaseAccessible = try {
                databaseEncryptionManager.validateDatabaseIntegrity(context) != DatabaseIntegrityResult.ACCESS_ERROR
            } catch (e: Exception) {
                false
            }
            
            // Check if preferences are accessible
            validation.preferencesAccessible = securePreferencesManager.validateIntegrity()
            
            validation.isValid = validation.databaseKeyExists && 
                                validation.preferencesKeyExists && 
                                validation.databaseAccessible && 
                                validation.preferencesAccessible
            
        } catch (e: Exception) {
            validation.isValid = false
            validation.error = e.message
        }
        
        validation
    }
}

/**
 * Result of security check operation
 */
data class SecurityCheckResult(
    var success: Boolean = false,
    var databaseIntegrity: DatabaseIntegrityResult = DatabaseIntegrityResult.UNKNOWN_ERROR,
    var dataIntegrityReport: DataIntegrityReport = DataIntegrityReport(),
    var preferencesIntegrity: Boolean = false,
    var currentDataHash: String = "",
    var dataUnchanged: Boolean? = null,
    var error: String? = null
)

/**
 * Result of key rotation operation
 */
data class KeyRotationResult(
    var success: Boolean = false,
    var databaseKeyRotated: Boolean = false,
    var preferencesKeyRotated: Boolean = false,
    var error: String? = null
)

/**
 * Result of secure wipe operation
 */
data class SecureWipeResult(
    var success: Boolean = false,
    var databaseWiped: Boolean = false,
    var preferencesWiped: Boolean = false,
    var keystoreWiped: Boolean = false,
    var error: String? = null
)

/**
 * Security setup validation result
 */
data class SecuritySetupValidation(
    var isValid: Boolean = false,
    var databaseKeyExists: Boolean = false,
    var preferencesKeyExists: Boolean = false,
    var databaseAccessible: Boolean = false,
    var preferencesAccessible: Boolean = false,
    var error: String? = null
)