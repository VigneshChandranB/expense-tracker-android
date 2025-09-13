package com.expensetracker.domain.error

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for error handling operations
 */
@Singleton
class ErrorUtils @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ErrorUtils"
        private const val MAX_ERROR_LOG_LENGTH = 1000
    }
    
    /**
     * Converts any throwable to an appropriate ErrorType
     */
    fun mapThrowableToErrorType(throwable: Throwable): ErrorType {
        return when (throwable) {
            is CancellationException -> ErrorType.GeneralError.OperationCancelled
            is TimeoutCancellationException -> ErrorType.GeneralError.OperationCancelled
            
            // Network errors
            is UnknownHostException -> ErrorType.NetworkError.NoConnection
            is ConnectException -> ErrorType.NetworkError.NoConnection
            is SocketTimeoutException -> ErrorType.NetworkError.Timeout
            
            // IO errors
            is IOException -> mapIOException(throwable)
            
            // Security errors
            is GeneralSecurityException -> ErrorType.SecurityError.EncryptionFailed
            is SecurityException -> ErrorType.SecurityError.EncryptionFailed
            
            // Database errors
            is SQLException -> ErrorType.DatabaseError.TransactionFailed
            
            // Validation errors
            is IllegalArgumentException -> ErrorType.GeneralError.ValidationFailed(
                field = "input",
                reason = throwable.message ?: "Invalid argument"
            )
            is IllegalStateException -> ErrorType.GeneralError.ValidationFailed(
                field = "state",
                reason = throwable.message ?: "Invalid state"
            )
            
            // Default to unknown error
            else -> ErrorType.GeneralError.Unknown(throwable)
        }
    }
    
    private fun mapIOException(exception: IOException): ErrorType {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            message.contains("no space left") || message.contains("disk full") -> 
                ErrorType.FileSystemError.InsufficientStorage
            message.contains("permission denied") || message.contains("access denied") -> 
                ErrorType.FileSystemError.PermissionDenied
            message.contains("file not found") || message.contains("no such file") -> 
                ErrorType.FileSystemError.FileNotFound
            message.contains("read") -> 
                ErrorType.FileSystemError.ReadError
            message.contains("write") -> 
                ErrorType.FileSystemError.WriteError
            else -> 
                ErrorType.FileSystemError.WriteError
        }
    }
    
    /**
     * Creates an ErrorResult from a throwable with appropriate error type and message
     */
    fun createErrorResult(
        throwable: Throwable,
        customMessage: String? = null,
        isRecoverable: Boolean? = null
    ): ErrorResult.Error {
        val errorType = mapThrowableToErrorType(throwable)
        val message = customMessage ?: getDefaultErrorMessage(errorType)
        val recoverable = isRecoverable ?: isErrorRecoverable(errorType)
        
        return ErrorResult.Error(
            errorType = errorType,
            message = message,
            isRecoverable = recoverable,
            cause = throwable
        )
    }
    
    /**
     * Determines if an error type is generally recoverable
     */
    fun isErrorRecoverable(errorType: ErrorType): Boolean {
        return when (errorType) {
            // Recoverable errors
            is ErrorType.SmsError.ProcessingTimeout,
            is ErrorType.SmsError.InvalidFormat,
            is ErrorType.SmsError.AmountParsingFailed -> true
            
            is ErrorType.DatabaseError.ConnectionFailed,
            is ErrorType.DatabaseError.TransactionFailed,
            is ErrorType.DatabaseError.ConstraintViolation -> true
            
            is ErrorType.FileSystemError.WriteError,
            is ErrorType.FileSystemError.ReadError,
            is ErrorType.FileSystemError.PermissionDenied -> true
            
            is ErrorType.NetworkError.Timeout,
            is ErrorType.NetworkError.NoConnection -> true
            
            is ErrorType.ExportError.GenerationFailed -> true
            
            is ErrorType.GeneralError.ValidationFailed,
            is ErrorType.GeneralError.Unknown -> true
            
            // Non-recoverable errors
            is ErrorType.SmsError.UnknownBankFormat -> false
            
            is ErrorType.DatabaseError.DataCorruption,
            is ErrorType.DatabaseError.DiskSpaceFull,
            is ErrorType.DatabaseError.MigrationFailed -> false
            
            is ErrorType.FileSystemError.InsufficientStorage,
            is ErrorType.FileSystemError.FileNotFound,
            is ErrorType.FileSystemError.InvalidPath -> false
            
            is ErrorType.NetworkError.ServerError,
            is ErrorType.NetworkError.Unauthorized -> false
            
            is ErrorType.ExportError.DataTooLarge,
            is ErrorType.ExportError.FormatNotSupported -> false
            
            is ErrorType.SecurityError -> false
            
            is ErrorType.GeneralError.OperationCancelled -> false
        }
    }
    
    /**
     * Gets a default error message for an error type
     */
    private fun getDefaultErrorMessage(errorType: ErrorType): String {
        return when (errorType) {
            is ErrorType.SmsError -> "SMS processing error occurred"
            is ErrorType.DatabaseError -> "Database operation failed"
            is ErrorType.FileSystemError -> "File operation failed"
            is ErrorType.NetworkError -> "Network error occurred"
            is ErrorType.ExportError -> "Export operation failed"
            is ErrorType.SecurityError -> "Security error occurred"
            is ErrorType.GeneralError -> "An error occurred"
        }
    }
    
    /**
     * Logs an error with appropriate level based on severity
     */
    fun logError(
        error: ErrorResult.Error,
        context: String = "",
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val severity = getErrorSeverity(error.errorType)
        val logMessage = buildErrorLogMessage(error, context, additionalData)
        
        when (severity) {
            ErrorSeverity.CRITICAL -> Log.e(TAG, logMessage, error.cause)
            ErrorSeverity.HIGH -> Log.e(TAG, logMessage, error.cause)
            ErrorSeverity.WARNING -> Log.w(TAG, logMessage, error.cause)
            ErrorSeverity.MEDIUM -> Log.i(TAG, logMessage)
            ErrorSeverity.LOW -> Log.d(TAG, logMessage)
        }
        
        // In a production app, you might also send to crash reporting service
        if (severity == ErrorSeverity.CRITICAL) {
            // Example: Crashlytics.recordException(error.cause ?: RuntimeException(error.message))
        }
    }
    
    private fun buildErrorLogMessage(
        error: ErrorResult.Error,
        context: String,
        additionalData: Map<String, Any>
    ): String {
        return buildString {
            append("Error: ${error.errorType::class.simpleName}")
            append(" | Message: ${error.message}")
            if (context.isNotEmpty()) {
                append(" | Context: $context")
            }
            append(" | Recoverable: ${error.isRecoverable}")
            
            if (additionalData.isNotEmpty()) {
                append(" | Data: ")
                additionalData.entries.joinToString(", ") { "${it.key}=${it.value}" }
                    .take(MAX_ERROR_LOG_LENGTH)
                    .let { append(it) }
            }
        }
    }
    
    private fun getErrorSeverity(errorType: ErrorType): ErrorSeverity {
        return when (errorType) {
            is ErrorType.SecurityError,
            is ErrorType.DatabaseError.DataCorruption -> ErrorSeverity.CRITICAL
            
            is ErrorType.DatabaseError.DiskSpaceFull,
            is ErrorType.FileSystemError.InsufficientStorage -> ErrorSeverity.HIGH
            
            is ErrorType.SmsError.PermissionDenied,
            is ErrorType.SmsError.InvalidFormat -> ErrorSeverity.WARNING
            
            is ErrorType.NetworkError,
            is ErrorType.ExportError,
            is ErrorType.DatabaseError.ConnectionFailed -> ErrorSeverity.MEDIUM
            
            else -> ErrorSeverity.LOW
        }
    }
    
    /**
     * Creates a user-friendly error summary for display
     */
    fun createErrorSummary(error: ErrorResult.Error): ErrorSummary {
        val severity = getErrorSeverity(error.errorType)
        val category = getErrorCategory(error.errorType)
        val suggestions = getErrorSuggestions(error.errorType)
        
        return ErrorSummary(
            title = getErrorTitle(error.errorType),
            message = error.message,
            severity = severity,
            category = category,
            isRecoverable = error.isRecoverable,
            suggestions = suggestions,
            technicalDetails = error.cause?.message
        )
    }
    
    private fun getErrorCategory(errorType: ErrorType): ErrorCategory {
        return when (errorType) {
            is ErrorType.SmsError -> ErrorCategory.SMS_PROCESSING
            is ErrorType.DatabaseError -> ErrorCategory.DATA_STORAGE
            is ErrorType.FileSystemError -> ErrorCategory.FILE_OPERATIONS
            is ErrorType.NetworkError -> ErrorCategory.NETWORK
            is ErrorType.ExportError -> ErrorCategory.DATA_EXPORT
            is ErrorType.SecurityError -> ErrorCategory.SECURITY
            is ErrorType.GeneralError -> ErrorCategory.GENERAL
        }
    }
    
    private fun getErrorTitle(errorType: ErrorType): String {
        return when (errorType) {
            is ErrorType.SmsError -> "SMS Processing Issue"
            is ErrorType.DatabaseError -> "Data Storage Issue"
            is ErrorType.FileSystemError -> "File Operation Issue"
            is ErrorType.NetworkError -> "Network Issue"
            is ErrorType.ExportError -> "Export Issue"
            is ErrorType.SecurityError -> "Security Issue"
            is ErrorType.GeneralError -> "Application Issue"
        }
    }
    
    private fun getErrorSuggestions(errorType: ErrorType): List<String> {
        return when (errorType) {
            is ErrorType.SmsError.PermissionDenied -> listOf(
                "Grant SMS permission in app settings",
                "Restart the app after granting permission"
            )
            
            is ErrorType.SmsError.InvalidFormat -> listOf(
                "Add the transaction manually",
                "Report the SMS format to help improve detection"
            )
            
            is ErrorType.DatabaseError.DiskSpaceFull -> listOf(
                "Free up storage space on your device",
                "Delete old photos or unused apps",
                "Move files to cloud storage"
            )
            
            is ErrorType.FileSystemError.InsufficientStorage -> listOf(
                "Free up storage space",
                "Try exporting a smaller date range"
            )
            
            is ErrorType.NetworkError.NoConnection -> listOf(
                "Check your internet connection",
                "Try again when connected to WiFi"
            )
            
            is ErrorType.SecurityError -> listOf(
                "Restart the app",
                "If problem persists, reinstall the app"
            )
            
            else -> listOf("Try the operation again")
        }
    }
    
    /**
     * Checks if two errors are of the same type (for deduplication)
     */
    fun areErrorsSimilar(error1: ErrorResult.Error, error2: ErrorResult.Error): Boolean {
        return error1.errorType::class == error2.errorType::class
    }
    
    /**
     * Creates a retry function for an error if applicable
     */
    fun createRetryFunction(
        originalOperation: suspend () -> Unit,
        error: ErrorResult.Error
    ): (suspend () -> Unit)? {
        return if (error.isRecoverable) {
            originalOperation
        } else {
            null
        }
    }
}

/**
 * Data class representing a user-friendly error summary
 */
data class ErrorSummary(
    val title: String,
    val message: String,
    val severity: ErrorSeverity,
    val category: ErrorCategory,
    val isRecoverable: Boolean,
    val suggestions: List<String>,
    val technicalDetails: String?
)

/**
 * Enum representing different error categories
 */
enum class ErrorCategory {
    SMS_PROCESSING,
    DATA_STORAGE,
    FILE_OPERATIONS,
    NETWORK,
    DATA_EXPORT,
    SECURITY,
    GENERAL
}

/**
 * Extension function to safely execute operations with error handling
 */
suspend inline fun <T> safeExecute(
    errorUtils: ErrorUtils,
    context: String = "",
    crossinline operation: suspend () -> T
): ErrorResult<T> {
    return try {
        ErrorResult.Success(operation())
    } catch (e: Exception) {
        val error = errorUtils.createErrorResult(e)
        errorUtils.logError(error, context)
        error
    }
}

/**
 * Extension function for ErrorResult to add logging
 */
fun ErrorResult.Error.logError(
    errorUtils: ErrorUtils,
    context: String = "",
    additionalData: Map<String, Any> = emptyMap()
): ErrorResult.Error {
    errorUtils.logError(this, context, additionalData)
    return this
}