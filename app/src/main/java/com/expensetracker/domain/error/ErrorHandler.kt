package com.expensetracker.domain.error

import android.content.Context
import com.expensetracker.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central error handler that provides user-friendly error messages and recovery actions
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {
    
    /**
     * Converts an ErrorType to a user-friendly error message
     */
    fun getErrorMessage(errorType: ErrorType): String {
        return when (errorType) {
            // SMS Errors
            is ErrorType.SmsError.PermissionDenied -> 
                context.getString(R.string.error_sms_permission_denied)
            is ErrorType.SmsError.InvalidFormat -> 
                context.getString(R.string.error_sms_invalid_format)
            is ErrorType.SmsError.AmountParsingFailed -> 
                context.getString(R.string.error_sms_amount_parsing)
            is ErrorType.SmsError.UnknownBankFormat -> 
                context.getString(R.string.error_sms_unknown_bank)
            is ErrorType.SmsError.ProcessingTimeout -> 
                context.getString(R.string.error_sms_timeout)
            is ErrorType.SmsError.PatternMatchFailed -> 
                context.getString(R.string.error_sms_pattern_match)
                
            // Database Errors
            is ErrorType.DatabaseError.ConnectionFailed -> 
                context.getString(R.string.error_database_connection)
            is ErrorType.DatabaseError.ConstraintViolation -> 
                context.getString(R.string.error_database_constraint)
            is ErrorType.DatabaseError.DataCorruption -> 
                context.getString(R.string.error_database_corruption)
            is ErrorType.DatabaseError.MigrationFailed -> 
                context.getString(R.string.error_database_migration)
            is ErrorType.DatabaseError.TransactionFailed -> 
                context.getString(R.string.error_database_transaction)
            is ErrorType.DatabaseError.DiskSpaceFull -> 
                context.getString(R.string.error_database_disk_space)
                
            // File System Errors
            is ErrorType.FileSystemError.InsufficientStorage -> 
                context.getString(R.string.error_file_insufficient_storage)
            is ErrorType.FileSystemError.PermissionDenied -> 
                context.getString(R.string.error_file_permission_denied)
            is ErrorType.FileSystemError.FileNotFound -> 
                context.getString(R.string.error_file_not_found)
            is ErrorType.FileSystemError.WriteError -> 
                context.getString(R.string.error_file_write)
            is ErrorType.FileSystemError.ReadError -> 
                context.getString(R.string.error_file_read)
            is ErrorType.FileSystemError.InvalidPath -> 
                context.getString(R.string.error_file_invalid_path)
                
            // Network Errors
            is ErrorType.NetworkError.NoConnection -> 
                context.getString(R.string.error_network_no_connection)
            is ErrorType.NetworkError.Timeout -> 
                context.getString(R.string.error_network_timeout)
            is ErrorType.NetworkError.ServerError -> 
                context.getString(R.string.error_network_server)
            is ErrorType.NetworkError.Unauthorized -> 
                context.getString(R.string.error_network_unauthorized)
                
            // Export Errors
            is ErrorType.ExportError.DataTooLarge -> 
                context.getString(R.string.error_export_data_too_large)
            is ErrorType.ExportError.FormatNotSupported -> 
                context.getString(R.string.error_export_format_not_supported, errorType.format)
            is ErrorType.ExportError.GenerationFailed -> 
                context.getString(R.string.error_export_generation_failed)
                
            // Security Errors
            is ErrorType.SecurityError.EncryptionFailed -> 
                context.getString(R.string.error_security_encryption)
            is ErrorType.SecurityError.DecryptionFailed -> 
                context.getString(R.string.error_security_decryption)
            is ErrorType.SecurityError.KeyGenerationFailed -> 
                context.getString(R.string.error_security_key_generation)
            is ErrorType.SecurityError.IntegrityCheckFailed -> 
                context.getString(R.string.error_security_integrity)
                
            // General Errors
            is ErrorType.GeneralError.Unknown -> 
                context.getString(R.string.error_general_unknown)
            is ErrorType.GeneralError.ValidationFailed -> 
                context.getString(R.string.error_general_validation, errorType.field)
            is ErrorType.GeneralError.OperationCancelled -> 
                context.getString(R.string.error_general_cancelled)
        }
    }
    
    /**
     * Determines if an error is recoverable and provides recovery suggestions
     */
    fun getRecoveryAction(errorType: ErrorType): RecoveryAction? {
        return when (errorType) {
            is ErrorType.SmsError.PermissionDenied -> 
                RecoveryAction.RequestPermission
            is ErrorType.SmsError.InvalidFormat,
            is ErrorType.SmsError.AmountParsingFailed,
            is ErrorType.SmsError.PatternMatchFailed -> 
                RecoveryAction.ManualEntry
            is ErrorType.SmsError.UnknownBankFormat -> 
                RecoveryAction.ReportIssue
            is ErrorType.SmsError.ProcessingTimeout -> 
                RecoveryAction.Retry
                
            is ErrorType.DatabaseError.ConnectionFailed,
            is ErrorType.DatabaseError.TransactionFailed -> 
                RecoveryAction.Retry
            is ErrorType.DatabaseError.DataCorruption -> 
                RecoveryAction.RestoreBackup
            is ErrorType.DatabaseError.DiskSpaceFull -> 
                RecoveryAction.FreeSpace
            is ErrorType.DatabaseError.MigrationFailed -> 
                RecoveryAction.ReinstallApp
                
            is ErrorType.FileSystemError.InsufficientStorage -> 
                RecoveryAction.FreeSpace
            is ErrorType.FileSystemError.PermissionDenied -> 
                RecoveryAction.RequestPermission
            is ErrorType.FileSystemError.WriteError,
            is ErrorType.FileSystemError.ReadError -> 
                RecoveryAction.Retry
                
            is ErrorType.NetworkError.NoConnection,
            is ErrorType.NetworkError.Timeout -> 
                RecoveryAction.CheckConnection
            is ErrorType.NetworkError.ServerError -> 
                RecoveryAction.RetryLater
                
            is ErrorType.ExportError.DataTooLarge -> 
                RecoveryAction.ReduceDataSize
            is ErrorType.ExportError.GenerationFailed -> 
                RecoveryAction.Retry
                
            is ErrorType.SecurityError -> 
                RecoveryAction.ReinstallApp
                
            is ErrorType.GeneralError.ValidationFailed -> 
                RecoveryAction.CorrectInput
            is ErrorType.GeneralError.Unknown -> 
                RecoveryAction.Retry
                
            else -> null
        }
    }
    
    /**
     * Determines the severity level of an error
     */
    fun getErrorSeverity(errorType: ErrorType): ErrorSeverity {
        return when (errorType) {
            is ErrorType.SmsError.PermissionDenied,
            is ErrorType.SmsError.InvalidFormat,
            is ErrorType.SmsError.AmountParsingFailed -> ErrorSeverity.WARNING
            
            is ErrorType.DatabaseError.DataCorruption,
            is ErrorType.SecurityError -> ErrorSeverity.CRITICAL
            
            is ErrorType.DatabaseError.DiskSpaceFull,
            is ErrorType.FileSystemError.InsufficientStorage -> ErrorSeverity.HIGH
            
            is ErrorType.NetworkError,
            is ErrorType.ExportError -> ErrorSeverity.MEDIUM
            
            else -> ErrorSeverity.LOW
        }
    }
}

enum class RecoveryAction {
    RETRY,
    REQUEST_PERMISSION,
    MANUAL_ENTRY,
    REPORT_ISSUE,
    RESTORE_BACKUP,
    FREE_SPACE,
    REINSTALL_APP,
    CHECK_CONNECTION,
    RETRY_LATER,
    REDUCE_DATA_SIZE,
    CORRECT_INPUT
}

enum class ErrorSeverity {
    LOW,
    MEDIUM,
    HIGH,
    WARNING,
    CRITICAL
}