package com.expensetracker.data.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entities.AccountEntity
import com.expensetracker.data.local.entities.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Integration tests for security features
 */
@RunWith(AndroidJUnit4::class)
class SecurityIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var database: ExpenseDatabase
    private lateinit var keystoreManager: KeystoreManager
    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var databaseEncryptionManager: DatabaseEncryptionManager
    private lateinit var dataIntegrityValidator: DataIntegrityValidator
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Initialize security components
        keystoreManager = KeystoreManager()
        securePreferencesManager = SecurePreferencesManager(context, keystoreManager)
        databaseEncryptionManager = DatabaseEncryptionManager(keystoreManager, securePreferencesManager)
        
        // Create encrypted database
        database = databaseEncryptionManager.createEncryptedDatabase(context)
        dataIntegrityValidator = DataIntegrityValidator(database)
    }
    
    @After
    fun cleanup() {
        database.close()
        databaseEncryptionManager.secureWipeDatabase(context)
    }
    
    @Test
    fun testEncryptedDatabaseOperations() = runTest {
        // Given
        val account = AccountEntity(
            id = 0,
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val transaction = TransactionEntity(
            id = 0,
            amount = "100.00",
            type = "EXPENSE",
            categoryId = 1L,
            accountId = 1L,
            merchant = "Test Merchant",
            description = "Test transaction",
            date = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // When - Insert data into encrypted database
        val accountId = database.accountDao().insertAccount(account)
        val transactionWithAccountId = transaction.copy(accountId = accountId)
        val transactionId = database.transactionDao().insertTransaction(transactionWithAccountId)
        
        // Then - Verify data can be retrieved
        val retrievedAccount = database.accountDao().getAccountById(accountId)
        val retrievedTransaction = database.transactionDao().getTransactionById(transactionId)
        
        assertThat(retrievedAccount).isNotNull()
        assertThat(retrievedAccount?.bankName).isEqualTo("Test Bank")
        assertThat(retrievedTransaction).isNotNull()
        assertThat(retrievedTransaction?.merchant).isEqualTo("Test Merchant")
    }
    
    @Test
    fun testDataIntegrityValidation() = runTest {
        // Given - Insert valid test data
        val account = AccountEntity(
            id = 0,
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "100.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = database.accountDao().insertAccount(account)
        
        val transaction = TransactionEntity(
            id = 0,
            amount = "100.00",
            type = "EXPENSE",
            categoryId = 1L,
            accountId = accountId,
            merchant = "Test Merchant",
            description = "Test transaction",
            date = LocalDateTime.now().minusDays(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        database.transactionDao().insertTransaction(transaction)
        
        // When - Validate data integrity
        val report = dataIntegrityValidator.validateAllData()
        
        // Then - Should have good integrity
        assertThat(report.overallIntegrity).isEqualTo(IntegrityLevel.GOOD)
        assertThat(report.errors).isEmpty()
    }
    
    @Test
    fun testDataHashGeneration() = runTest {
        // Given - Insert test data
        val account = AccountEntity(
            id = 0,
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "100.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        database.accountDao().insertAccount(account)
        
        // When - Generate hash
        val hash1 = dataIntegrityValidator.generateDataHash()
        val hash2 = dataIntegrityValidator.generateDataHash()
        
        // Then - Hashes should be consistent
        assertThat(hash1).isEqualTo(hash2)
        assertThat(hash1).isNotEmpty()
    }
    
    @Test
    fun testDataTamperDetection() = runTest {
        // Given - Insert initial data
        val account = AccountEntity(
            id = 0,
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "100.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = database.accountDao().insertAccount(account)
        val originalHash = dataIntegrityValidator.generateDataHash()
        
        // When - Modify data
        database.accountDao().updateAccountBalance(accountId, "200.00")
        
        // Then - Should detect tampering
        val isValid = dataIntegrityValidator.verifyDataIntegrity(originalHash)
        assertThat(isValid).isFalse()
    }
    
    @Test
    fun testDatabaseIntegrityCheck() {
        // When - Check database integrity
        val result = databaseEncryptionManager.validateDatabaseIntegrity(context)
        
        // Then - Should be valid
        assertThat(result).isEqualTo(DatabaseIntegrityResult.VALID)
    }
    
    @Test
    fun testSecurePreferencesIntegration() {
        // Given
        val testData = "sensitive data".toByteArray()
        val encryptedData = keystoreManager.encrypt(testData, "test_key")
        
        // When - Store in secure preferences
        securePreferencesManager.storeDatabasePassphrase(encryptedData)
        
        // Then - Should be able to retrieve and decrypt
        val retrieved = securePreferencesManager.getDatabasePassphrase()
        assertThat(retrieved).isNotNull()
        
        val decryptedData = keystoreManager.decrypt(retrieved!!, "test_key")
        assertThat(decryptedData).isEqualTo(testData)
    }
    
    @Test
    fun testKeyRotation() {
        // Given - Encrypt data with original key
        val testData = "test data".toByteArray()
        val originalEncrypted = keystoreManager.encrypt(testData, "rotation_test_key")
        
        // When - Rotate key
        keystoreManager.rotateKey("rotation_test_key")
        
        // Then - Should be able to encrypt with new key
        val newEncrypted = keystoreManager.encrypt(testData, "rotation_test_key")
        val decrypted = keystoreManager.decrypt(newEncrypted, "rotation_test_key")
        
        assertThat(decrypted).isEqualTo(testData)
        assertThat(newEncrypted.data).isNotEqualTo(originalEncrypted.data)
    }
    
    @Test
    fun testSecureWipe() {
        // Given - Store some data
        securePreferencesManager.setBiometricEnabled(true)
        securePreferencesManager.setAutoLockTimeout(10)
        
        val account = AccountEntity(
            id = 0,
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "100.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        runTest {
            database.accountDao().insertAccount(account)
        }
        
        // When - Perform secure wipe
        val wipeResult = databaseEncryptionManager.secureWipeDatabase(context)
        securePreferencesManager.secureWipe()
        
        // Then - Data should be gone
        assertThat(wipeResult).isTrue()
        assertThat(securePreferencesManager.isBiometricEnabled()).isFalse()
        assertThat(securePreferencesManager.getAutoLockTimeout()).isEqualTo(5) // Default
    }
    
    @Test
    fun testEncryptionPerformance() = runTest {
        // Given
        val largeData = ByteArray(1024 * 1024) { it.toByte() } // 1MB of data
        
        // When - Measure encryption time
        val startTime = System.currentTimeMillis()
        val encrypted = keystoreManager.encrypt(largeData, "performance_test")
        val encryptTime = System.currentTimeMillis() - startTime
        
        // Measure decryption time
        val decryptStartTime = System.currentTimeMillis()
        val decrypted = keystoreManager.decrypt(encrypted, "performance_test")
        val decryptTime = System.currentTimeMillis() - decryptStartTime
        
        // Then - Should complete in reasonable time (less than 5 seconds each)
        assertThat(encryptTime).isLessThan(5000)
        assertThat(decryptTime).isLessThan(5000)
        assertThat(decrypted).isEqualTo(largeData)
    }
}