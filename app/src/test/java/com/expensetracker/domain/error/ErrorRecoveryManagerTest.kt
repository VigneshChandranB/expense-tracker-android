package com.expensetracker.domain.error

import android.content.Context
import com.expensetracker.data.file.FileSystemErrorHandler
import com.expensetracker.data.local.error.DatabaseErrorHandler
import com.expensetracker.data.sms.SmsErrorHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class ErrorRecoveryManagerTest {
    
    private lateinit var errorRecoveryManager: ErrorRecoveryManager
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockErrorHandler: ErrorHandler
    
    @Mock
    private lateinit var mockSmsErrorHandler: SmsErrorHandler
    
    @Mock
    private lateinit var mockDatabaseErrorHandler: DatabaseErrorHandler
    
    @Mock
    private lateinit var mockFileSystemErrorHandler: FileSystemErrorHandler
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        errorRecoveryManager = ErrorRecoveryManager(
            mockContext,
            mockErrorHandler,
            mockSmsErrorHandler,
            mockDatabaseErrorHandler,
            mockFileSystemErrorHandler
        )
    }
    
    @Test
    fun `initial recovery state should be idle`() = runTest {
        val initialState = errorRecoveryManager.recoveryState.first()
        
        assertTrue(initialState is RecoveryState.Idle)
    }
    
    @Test
    fun `attemptRecovery should handle SMS processing timeout`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.SmsError.ProcessingTimeout(5000L),
            "SMS processing timed out"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.Success)
        assertEquals("Retrying with extended timeout", (result as RecoveryResult.Success).message)
        
        val finalState = errorRecoveryManager.recoveryState.first()
        assertTrue(finalState is RecoveryState.Success)
    }
    
    @Test
    fun `attemptRecovery should handle SMS invalid format`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.SmsError.InvalidFormat,
            "SMS format not recognized"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.MANUAL_ENTRY, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("manually"))
        
        val finalState = errorRecoveryManager.recoveryState.first()
        assertTrue(finalState is RecoveryState.RequiresUserAction)
    }
    
    @Test
    fun `attemptRecovery should handle database connection failure`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.DatabaseError.ConnectionFailed,
            "Database connection failed"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.Success)
        assertEquals("Retrying database operation", (result as RecoveryResult.Success).message)
    }
    
    @Test
    fun `attemptRecovery should handle database disk full`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.DatabaseError.DiskSpaceFull,
            "Database disk full"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.FREE_SPACE, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("free up space"))
    }
    
    @Test
    fun `attemptRecovery should handle file system insufficient storage`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.FileSystemError.InsufficientStorage,
            "Insufficient storage"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.FREE_SPACE, userActionResult.action)
    }
    
    @Test
    fun `attemptRecovery should handle security errors`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.SecurityError.EncryptionFailed,
            "Encryption failed"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.REINSTALL_APP, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("reinstall"))
    }
    
    @Test
    fun `attemptRecovery should handle export data too large`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.ExportError.DataTooLarge,
            "Export data too large"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.REDUCE_DATA_SIZE, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("smaller date range"))
    }
    
    @Test
    fun `attemptRecovery should handle validation errors`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.GeneralError.ValidationFailed("amount", "must be positive"),
            "Validation failed"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.CORRECT_INPUT, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("must be positive"))
    }
    
    @Test
    fun `attemptRecovery should handle unknown errors`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.GeneralError.Unknown(RuntimeException("Unknown error")),
            "Unknown error occurred"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        assertTrue(result is RecoveryResult.Success)
        assertEquals("Retrying operation", (result as RecoveryResult.Success).message)
    }
    
    @Test
    fun `getRecoveryInstructions should provide comprehensive instructions`() {
        val errorType = ErrorType.SmsError.PermissionDenied
        
        // Mock the error handler responses
        whenever(mockErrorHandler.getRecoveryAction(errorType)).thenReturn(RecoveryAction.REQUEST_PERMISSION)
        whenever(mockErrorHandler.getErrorSeverity(errorType)).thenReturn(ErrorSeverity.WARNING)
        whenever(mockErrorHandler.getErrorMessage(errorType)).thenReturn("SMS permission denied")
        
        val instructions = errorRecoveryManager.getRecoveryInstructions(errorType)
        
        assertEquals(errorType, instructions.errorType)
        assertEquals("SMS permission denied", instructions.message)
        assertEquals(ErrorSeverity.WARNING, instructions.severity)
        assertEquals(RecoveryAction.REQUEST_PERMISSION, instructions.recoveryAction)
        assertFalse(instructions.canAutoRecover)
        assertEquals("1-2 minutes", instructions.estimatedRecoveryTime)
        assertTrue(instructions.steps.isNotEmpty())
        assertTrue(instructions.steps.any { it.contains("Grant Permission") })
    }
    
    @Test
    fun `executeUserRecovery should handle retry action`() = runTest {
        val result = errorRecoveryManager.executeUserRecovery(
            ErrorType.SmsError.ProcessingTimeout(5000L),
            RecoveryAction.RETRY
        )
        
        assertTrue(result is RecoveryResult.Success)
        assertEquals("Ready to retry", (result as RecoveryResult.Success).message)
    }
    
    @Test
    fun `executeUserRecovery should handle permission request`() = runTest {
        // Mock package name for context
        whenever(mockContext.packageName).thenReturn("com.expensetracker")
        
        val result = errorRecoveryManager.executeUserRecovery(
            ErrorType.SmsError.PermissionDenied,
            RecoveryAction.REQUEST_PERMISSION
        )
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.REQUEST_PERMISSION, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("grant the required permissions"))
    }
    
    @Test
    fun `executeUserRecovery should handle manual entry`() = runTest {
        val result = errorRecoveryManager.executeUserRecovery(
            ErrorType.SmsError.InvalidFormat,
            RecoveryAction.MANUAL_ENTRY
        )
        
        assertTrue(result is RecoveryResult.Success)
        assertEquals("Switch to manual entry mode", (result as RecoveryResult.Success).message)
    }
    
    @Test
    fun `executeUserRecovery should handle issue reporting`() = runTest {
        val result = errorRecoveryManager.executeUserRecovery(
            ErrorType.SmsError.UnknownBankFormat,
            RecoveryAction.REPORT_ISSUE
        )
        
        assertTrue(result is RecoveryResult.Success)
        assertEquals("Issue reporting opened", (result as RecoveryResult.Success).message)
    }
    
    @Test
    fun `executeUserRecovery should handle backup restore`() = runTest {
        val result = errorRecoveryManager.executeUserRecovery(
            ErrorType.DatabaseError.DataCorruption,
            RecoveryAction.RESTORE_BACKUP
        )
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.RESTORE_BACKUP, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("select a backup file"))
    }
    
    @Test
    fun `executeUserRecovery should handle free space action`() = runTest {
        val result = errorRecoveryManager.executeUserRecovery(
            ErrorType.DatabaseError.DiskSpaceFull,
            RecoveryAction.FREE_SPACE
        )
        
        assertTrue(result is RecoveryResult.RequiresUserAction)
        val userActionResult = result as RecoveryResult.RequiresUserAction
        assertEquals(RecoveryAction.FREE_SPACE, userActionResult.action)
        assertTrue(userActionResult.instructions.contains("free up storage space"))
    }
    
    @Test
    fun `recovery state should update correctly during recovery process`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.SmsError.ProcessingTimeout(5000L),
            "SMS processing timed out"
        )
        
        // Initial state should be idle
        assertTrue(errorRecoveryManager.recoveryState.first() is RecoveryState.Idle)
        
        // Start recovery
        val result = errorRecoveryManager.attemptRecovery(error)
        
        // Should succeed and update state
        assertTrue(result is RecoveryResult.Success)
        val finalState = errorRecoveryManager.recoveryState.first()
        assertTrue(finalState is RecoveryState.Success)
        assertEquals(error.errorType, (finalState as RecoveryState.Success).errorType)
    }
    
    @Test
    fun `recovery state should handle failed recovery`() = runTest {
        val error = ErrorResult.Error(
            ErrorType.NetworkError.NoConnection, // This should fail recovery
            "No network connection"
        )
        
        val result = errorRecoveryManager.attemptRecovery(error)
        
        // Network errors don't have auto-recovery, so should fail
        assertTrue(result is RecoveryResult.Failed)
        
        val finalState = errorRecoveryManager.recoveryState.first()
        assertTrue(finalState is RecoveryState.Failed)
    }
}