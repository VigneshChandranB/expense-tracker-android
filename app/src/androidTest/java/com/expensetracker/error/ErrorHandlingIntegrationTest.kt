package com.expensetracker.error

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.file.FileSystemErrorHandler
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.error.DatabaseErrorHandler
import com.expensetracker.data.sms.SmsErrorHandler
import com.expensetracker.domain.error.*
import com.expensetracker.domain.model.SmsMessage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ErrorHandlingIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var database: ExpenseDatabase
    private lateinit var errorHandler: ErrorHandler
    private lateinit var smsErrorHandler: SmsErrorHandler
    private lateinit var databaseErrorHandler: DatabaseErrorHandler
    private lateinit var fileSystemErrorHandler: FileSystemErrorHandler
    private lateinit var errorRecoveryManager: ErrorRecoveryManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Initialize error handlers
        errorHandler = ErrorHandler(context)
        smsErrorHandler = SmsErrorHandler()
        databaseErrorHandler = DatabaseErrorHandler()
        fileSystemErrorHandler = FileSystemErrorHandler(context)
        
        errorRecoveryManager = ErrorRecoveryManager(
            context,
            errorHandler,
            smsErrorHandler,
            databaseErrorHandler,
            fileSystemErrorHandler
        )
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun smsErrorHandling_completeFlow() = runTest {
        // Test SMS validation error
        val invalidSms = SmsMessage(
            id = "1",
            sender = "FRIEND",
            body = "Hi there!",
            timestamp = LocalDateTime.now()
        )
        
        val validationResult = smsErrorHandler.validateSmsMessage(invalidSms)
        assertTrue(validationResult is ErrorResult.Error)
        
        val error = validationResult as ErrorResult.Error
        assertEquals(ErrorType.SmsError.InvalidFormat, error.errorType)
        
        // Test error recovery
        val recoveryResult = errorRecoveryManager.attemptRecovery(error)
        assertTrue(recoveryResult is RecoveryResult.RequiresUserAction)
        
        val userAction = recoveryResult as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.MANUAL_ENTRY, userAction.action)
        
        // Test recovery instructions
        val instructions = errorRecoveryManager.getRecoveryInstructions(error.errorType)
        assertEquals(ErrorType.SmsError.InvalidFormat, instructions.errorType)
        assertFalse(instructions.canAutoRecover)
        assertTrue(instructions.steps.isNotEmpty())
    }
    
    @Test
    fun smsAmountParsing_withFallback() = runTest {
        // Test amount parsing with multiple patterns
        val smsText = "Transaction of 1,234.56 completed at MERCHANT"
        val patterns = listOf(
            Regex("""Rs\s*([\d,]+\.?\d*)"""), // This will fail
            Regex("""INR\s*([\d,]+\.?\d*)""") // This will also fail
        )
        
        val result = smsErrorHandler.parseAmountWithFallback(smsText, patterns)
        
        // Should succeed with manual extraction fallback
        assertTrue(result is ErrorResult.Success)
        assertEquals(1234.56, (result as ErrorResult.Success).data, 0.01)
    }
    
    @Test
    fun smsProcessing_withRetry() = runTest {
        val validSms = SmsMessage(
            id = "1",
            sender = "HDFC-BANK",
            body = "Amount Rs 500.00 debited from account ending 1234",
            timestamp = LocalDateTime.now()
        )
        
        var attemptCount = 0
        val result = smsErrorHandler.processWithRetry(validSms) {
            attemptCount++
            if (attemptCount < 2) {
                throw RuntimeException("Temporary failure")
            }
            null // Simulate successful processing
        }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(2, attemptCount)
    }
    
    @Test
    fun databaseErrorHandling_transactionFailure() = runTest {
        // Test database transaction with error handling
        val result = databaseErrorHandler.executeTransaction(database) {
            throw RuntimeException("Simulated database error")
        }
        
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        assertEquals(ErrorType.DatabaseError.TransactionFailed, error.errorType)
        
        // Test recovery
        val recoveryResult = errorRecoveryManager.attemptRecovery(error)
        assertTrue(recoveryResult is RecoveryResult.Success)
        assertEquals("Retrying database operation", (recoveryResult as RecoveryResult.Success).message)
    }
    
    @Test
    fun databaseErrorHandling_integrityCheck() = runTest {
        val sqliteDatabase = database.openHelper.writableDatabase
        
        val result = databaseErrorHandler.validateDatabaseIntegrity(sqliteDatabase)
        
        // Should succeed for a healthy in-memory database
        assertTrue(result is ErrorResult.Success)
        assertTrue((result as ErrorResult.Success).data)
    }
    
    @Test
    fun fileSystemErrorHandling_completeFlow() = runTest {
        val testDir = File(context.filesDir, "test_error_handling")
        testDir.mkdirs()
        
        val testFile = File(testDir, "test.txt")
        val testContent = "Test content for error handling"
        
        // Test successful file write
        val writeResult = fileSystemErrorHandler.writeFile(testFile.absolutePath, testContent)
        assertTrue(writeResult is ErrorResult.Success)
        assertTrue((writeResult as ErrorResult.Success).data)
        
        // Test successful file read
        val readResult = fileSystemErrorHandler.readFile(testFile.absolutePath)
        assertTrue(readResult is ErrorResult.Success)
        assertEquals(testContent, (readResult as ErrorResult.Success).data)
        
        // Test file backup
        val backupResult = fileSystemErrorHandler.createFileBackup(testFile.absolutePath)
        assertTrue(backupResult is ErrorResult.Success)
        val backupPath = (backupResult as ErrorResult.Success).data
        assertTrue(File(backupPath).exists())
        
        // Test file restore
        testFile.writeText("Modified content")
        val restoreResult = fileSystemErrorHandler.restoreFromBackup(testFile.absolutePath, backupPath)
        assertTrue(restoreResult is ErrorResult.Success)
        assertEquals(testContent, testFile.readText())
        
        // Test file deletion
        val deleteResult = fileSystemErrorHandler.deleteFile(testFile.absolutePath)
        assertTrue(deleteResult is ErrorResult.Success)
        assertFalse(testFile.exists())
        
        // Cleanup
        File(backupPath).delete()
        testDir.deleteRecursively()
    }
    
    @Test
    fun fileSystemErrorHandling_nonExistentFile() = runTest {
        val nonExistentPath = File(context.filesDir, "nonexistent.txt").absolutePath
        
        val readResult = fileSystemErrorHandler.readFile(nonExistentPath)
        assertTrue(readResult is ErrorResult.Error)
        
        val error = readResult as ErrorResult.Error
        assertEquals(ErrorType.FileSystemError.FileNotFound, error.errorType)
        
        // Test recovery
        val recoveryResult = errorRecoveryManager.attemptRecovery(error)
        assertTrue(recoveryResult is RecoveryResult.Failed)
    }
    
    @Test
    fun errorRecovery_stateManagement() = runTest {
        val error = ErrorResult.Error(
            ErrorType.SmsError.ProcessingTimeout(5000L),
            "Processing timeout"
        )
        
        // Initial state should be idle
        assertTrue(errorRecoveryManager.recoveryState.value is RecoveryState.Idle)
        
        // Attempt recovery
        val result = errorRecoveryManager.attemptRecovery(error)
        assertTrue(result is RecoveryResult.Success)
        
        // State should be updated to success
        assertTrue(errorRecoveryManager.recoveryState.value is RecoveryState.Success)
        val successState = errorRecoveryManager.recoveryState.value as RecoveryState.Success
        assertEquals(error.errorType, successState.errorType)
    }
    
    @Test
    fun errorRecovery_userActionRequired() = runTest {
        val error = ErrorResult.Error(
            ErrorType.DatabaseError.DiskSpaceFull,
            "Disk space full"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        assertTrue(result is RecoveryResult.RequiresUserAction)
        
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.FREE_SPACE, userActionResult.action)
        
        // State should reflect user action required
        assertTrue(errorRecoveryManager.recoveryState.value is RecoveryState.RequiresUserAction)
        val actionState = errorRecoveryManager.recoveryState.value as RecoveryState.RequiresUserAction
        assertEquals(error.errorType, actionState.errorType)
        assertEquals(RecoveryAction.FREE_SPACE, actionState.action)
    }
    
    @Test
    fun errorHandler_messageLocalization() {
        // Test that error messages are properly localized
        val smsError = ErrorType.SmsError.PermissionDenied
        val message = errorHandler.getErrorMessage(smsError)
        
        assertNotNull(message)
        assertFalse(message.isEmpty())
        assertTrue(message.contains("SMS") || message.contains("permission"))
        
        val databaseError = ErrorType.DatabaseError.ConnectionFailed
        val dbMessage = errorHandler.getErrorMessage(databaseError)
        
        assertNotNull(dbMessage)
        assertFalse(dbMessage.isEmpty())
        assertTrue(dbMessage.contains("database") || dbMessage.contains("connection"))
    }
    
    @Test
    fun errorHandler_severityClassification() {
        // Test error severity classification
        val criticalError = ErrorType.SecurityError.EncryptionFailed
        val criticalSeverity = errorHandler.getErrorSeverity(criticalError)
        assertEquals(ErrorSeverity.CRITICAL, criticalSeverity)
        
        val warningError = ErrorType.SmsError.InvalidFormat
        val warningSeverity = errorHandler.getErrorSeverity(warningError)
        assertEquals(ErrorSeverity.WARNING, warningSeverity)
        
        val mediumError = ErrorType.ExportError.GenerationFailed
        val mediumSeverity = errorHandler.getErrorSeverity(mediumError)
        assertEquals(ErrorSeverity.MEDIUM, mediumSeverity)
    }
    
    @Test
    fun errorHandler_recoveryActionMapping() {
        // Test recovery action mapping
        val permissionError = ErrorType.SmsError.PermissionDenied
        val permissionAction = errorHandler.getRecoveryAction(permissionError)
        assertEquals(RecoveryAction.REQUEST_PERMISSION, permissionAction)
        
        val diskFullError = ErrorType.DatabaseError.DiskSpaceFull
        val diskFullAction = errorHandler.getRecoveryAction(diskFullError)
        assertEquals(RecoveryAction.FREE_SPACE, diskFullAction)
        
        val corruptionError = ErrorType.DatabaseError.DataCorruption
        val corruptionAction = errorHandler.getRecoveryAction(corruptionError)
        assertEquals(RecoveryAction.RESTORE_BACKUP, corruptionAction)
    }
    
    @Test
    fun endToEndErrorFlow_smsToRecovery() = runTest {
        // Simulate complete error flow from SMS processing to recovery
        val problematicSms = SmsMessage(
            id = "1",
            sender = "UNKNOWN-BANK",
            body = "Your transaction of amount has been processed",
            timestamp = LocalDateTime.now()
        )
        
        // 1. SMS validation should pass (contains financial keywords)
        val validationResult = smsErrorHandler.validateSmsMessage(problematicSms)
        assertTrue(validationResult is ErrorResult.Success)
        
        // 2. Amount parsing should fail
        val patterns = listOf(Regex("""Rs\s*([\d,]+\.?\d*)"""))
        val amountResult = smsErrorHandler.parseAmountWithFallback(problematicSms.body, patterns)
        assertTrue(amountResult is ErrorResult.Error)
        
        // 3. Error recovery should suggest manual entry
        val error = amountResult as ErrorResult.Error
        val recoveryResult = errorRecoveryManager.attemptRecovery(error)
        assertTrue(recoveryResult is RecoveryResult.RequiresUserAction)
        
        val userAction = recoveryResult as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.MANUAL_ENTRY, userAction.action)
        
        // 4. User can execute the recovery action
        val userRecoveryResult = errorRecoveryManager.executeUserRecovery(
            error.errorType,
            RecoveryAction.MANUAL_ENTRY
        )
        assertTrue(userRecoveryResult is RecoveryResult.Success)
        assertEquals("Switch to manual entry mode", (userRecoveryResult as RecoveryResult.Success).message)
    }
}