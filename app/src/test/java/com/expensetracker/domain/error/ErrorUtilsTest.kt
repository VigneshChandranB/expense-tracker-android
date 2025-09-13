package com.expensetracker.domain.error

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import java.sql.SQLException

class ErrorUtilsTest {
    
    private lateinit var errorUtils: ErrorUtils
    
    @Mock
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        errorUtils = ErrorUtils(mockContext)
    }
    
    @Test
    fun `mapThrowableToErrorType should map CancellationException correctly`() {
        val exception = CancellationException("Operation cancelled")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.GeneralError.OperationCancelled, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map TimeoutCancellationException correctly`() {
        val exception = TimeoutCancellationException("Operation timed out")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.GeneralError.OperationCancelled, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map UnknownHostException correctly`() {
        val exception = UnknownHostException("Host not found")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.NetworkError.NoConnection, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map ConnectException correctly`() {
        val exception = ConnectException("Connection refused")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.NetworkError.NoConnection, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map SocketTimeoutException correctly`() {
        val exception = SocketTimeoutException("Socket timeout")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.NetworkError.Timeout, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map IOException with disk full message`() {
        val exception = IOException("No space left on device")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.FileSystemError.InsufficientStorage, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map IOException with permission denied message`() {
        val exception = IOException("Permission denied")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.FileSystemError.PermissionDenied, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map IOException with file not found message`() {
        val exception = IOException("File not found")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.FileSystemError.FileNotFound, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map IOException with read error message`() {
        val exception = IOException("Read error occurred")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.FileSystemError.ReadError, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map IOException with write error message`() {
        val exception = IOException("Write error occurred")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.FileSystemError.WriteError, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map GeneralSecurityException correctly`() {
        val exception = GeneralSecurityException("Security error")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.SecurityError.EncryptionFailed, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map SecurityException correctly`() {
        val exception = SecurityException("Security violation")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.SecurityError.EncryptionFailed, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map SQLException correctly`() {
        val exception = SQLException("Database error")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertEquals(ErrorType.DatabaseError.TransactionFailed, errorType)
    }
    
    @Test
    fun `mapThrowableToErrorType should map IllegalArgumentException correctly`() {
        val exception = IllegalArgumentException("Invalid argument provided")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertTrue(errorType is ErrorType.GeneralError.ValidationFailed)
        val validationError = errorType as ErrorType.GeneralError.ValidationFailed
        assertEquals("input", validationError.field)
        assertEquals("Invalid argument provided", validationError.reason)
    }
    
    @Test
    fun `mapThrowableToErrorType should map IllegalStateException correctly`() {
        val exception = IllegalStateException("Invalid state")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertTrue(errorType is ErrorType.GeneralError.ValidationFailed)
        val validationError = errorType as ErrorType.GeneralError.ValidationFailed
        assertEquals("state", validationError.field)
        assertEquals("Invalid state", validationError.reason)
    }
    
    @Test
    fun `mapThrowableToErrorType should map unknown exception to GeneralError`() {
        val exception = RuntimeException("Unknown error")
        
        val errorType = errorUtils.mapThrowableToErrorType(exception)
        
        assertTrue(errorType is ErrorType.GeneralError.Unknown)
        val unknownError = errorType as ErrorType.GeneralError.Unknown
        assertEquals(exception, unknownError.throwable)
    }
    
    @Test
    fun `createErrorResult should create proper ErrorResult with default values`() {
        val exception = IOException("File write failed")
        
        val result = errorUtils.createErrorResult(exception)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.FileSystemError.WriteError, result.errorType)
        assertEquals("File operation failed", result.message)
        assertTrue(result.isRecoverable)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `createErrorResult should use custom message when provided`() {
        val exception = IOException("File write failed")
        val customMessage = "Custom error message"
        
        val result = errorUtils.createErrorResult(exception, customMessage)
        
        assertEquals(customMessage, result.message)
    }
    
    @Test
    fun `createErrorResult should use custom recoverable flag when provided`() {
        val exception = IOException("File write failed")
        
        val result = errorUtils.createErrorResult(exception, isRecoverable = false)
        
        assertFalse(result.isRecoverable)
    }
    
    @Test
    fun `isErrorRecoverable should return true for recoverable errors`() {
        val recoverableErrors = listOf(
            ErrorType.SmsError.ProcessingTimeout(5000L),
            ErrorType.SmsError.InvalidFormat,
            ErrorType.DatabaseError.ConnectionFailed,
            ErrorType.FileSystemError.WriteError,
            ErrorType.NetworkError.Timeout,
            ErrorType.ExportError.GenerationFailed,
            ErrorType.GeneralError.ValidationFailed("field", "reason")
        )
        
        recoverableErrors.forEach { errorType ->
            assertTrue("$errorType should be recoverable", errorUtils.isErrorRecoverable(errorType))
        }
    }
    
    @Test
    fun `isErrorRecoverable should return false for non-recoverable errors`() {
        val nonRecoverableErrors = listOf(
            ErrorType.SmsError.UnknownBankFormat,
            ErrorType.DatabaseError.DataCorruption,
            ErrorType.DatabaseError.DiskSpaceFull,
            ErrorType.FileSystemError.InsufficientStorage,
            ErrorType.SecurityError.EncryptionFailed,
            ErrorType.GeneralError.OperationCancelled
        )
        
        nonRecoverableErrors.forEach { errorType ->
            assertFalse("$errorType should not be recoverable", errorUtils.isErrorRecoverable(errorType))
        }
    }
    
    @Test
    fun `createErrorSummary should create comprehensive summary`() {
        val error = ErrorResult.Error(
            ErrorType.SmsError.PermissionDenied,
            "SMS permission denied",
            isRecoverable = true,
            cause = SecurityException("Permission denied")
        )
        
        val summary = errorUtils.createErrorSummary(error)
        
        assertEquals("SMS Processing Issue", summary.title)
        assertEquals("SMS permission denied", summary.message)
        assertEquals(ErrorSeverity.WARNING, summary.severity)
        assertEquals(ErrorCategory.SMS_PROCESSING, summary.category)
        assertTrue(summary.isRecoverable)
        assertTrue(summary.suggestions.isNotEmpty())
        assertTrue(summary.suggestions.any { it.contains("permission") })
        assertEquals("Permission denied", summary.technicalDetails)
    }
    
    @Test
    fun `areErrorsSimilar should return true for same error types`() {
        val error1 = ErrorResult.Error(
            ErrorType.SmsError.InvalidFormat,
            "Message 1"
        )
        val error2 = ErrorResult.Error(
            ErrorType.SmsError.InvalidFormat,
            "Message 2"
        )
        
        assertTrue(errorUtils.areErrorsSimilar(error1, error2))
    }
    
    @Test
    fun `areErrorsSimilar should return false for different error types`() {
        val error1 = ErrorResult.Error(
            ErrorType.SmsError.InvalidFormat,
            "Message 1"
        )
        val error2 = ErrorResult.Error(
            ErrorType.DatabaseError.ConnectionFailed,
            "Message 2"
        )
        
        assertFalse(errorUtils.areErrorsSimilar(error1, error2))
    }
    
    @Test
    fun `createRetryFunction should return function for recoverable errors`() {
        val error = ErrorResult.Error(
            ErrorType.SmsError.ProcessingTimeout(5000L),
            "Timeout error",
            isRecoverable = true
        )
        
        val originalOperation: suspend () -> Unit = {}
        val retryFunction = errorUtils.createRetryFunction(originalOperation, error)
        
        assertNotNull(retryFunction)
        assertEquals(originalOperation, retryFunction)
    }
    
    @Test
    fun `createRetryFunction should return null for non-recoverable errors`() {
        val error = ErrorResult.Error(
            ErrorType.SecurityError.EncryptionFailed,
            "Security error",
            isRecoverable = false
        )
        
        val originalOperation: suspend () -> Unit = {}
        val retryFunction = errorUtils.createRetryFunction(originalOperation, error)
        
        assertNull(retryFunction)
    }
    
    @Test
    fun `safeExecute should return success for successful operation`() = runTest {
        val expectedResult = "Success"
        
        val result = safeExecute(errorUtils, "test context") {
            expectedResult
        }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(expectedResult, (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `safeExecute should return error for failed operation`() = runTest {
        val exception = IOException("Test error")
        
        val result = safeExecute(errorUtils, "test context") {
            throw exception
        }
        
        assertTrue(result is ErrorResult.Error)
        val error = result as ErrorResult.Error
        assertEquals(ErrorType.FileSystemError.WriteError, error.errorType)
        assertEquals(exception, error.cause)
    }
    
    @Test
    fun `logError should not throw exception`() {
        val error = ErrorResult.Error(
            ErrorType.SmsError.InvalidFormat,
            "Test error message",
            cause = RuntimeException("Test cause")
        )
        
        // Should not throw any exception
        assertDoesNotThrow {
            errorUtils.logError(error, "test context", mapOf("key" to "value"))
        }
    }
    
    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}