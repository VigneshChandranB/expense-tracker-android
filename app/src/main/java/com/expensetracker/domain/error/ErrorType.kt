package com.expensetracker.domain.error

/**
 * Sealed class representing different types of errors that can occur in the app
 */
sealed class ErrorType {
    // SMS Processing Errors
    sealed class SmsError : ErrorType() {
        object PermissionDenied : SmsError()
        object InvalidFormat : SmsError()
        object AmountParsingFailed : SmsError()
        object UnknownBankFormat : SmsError()
        data class ProcessingTimeout(val timeoutMs: Long) : SmsError()
        data class PatternMatchFailed(val pattern: String) : SmsError()
    }
    
    // Database Errors
    sealed class DatabaseError : ErrorType() {
        object ConnectionFailed : DatabaseError()
        object ConstraintViolation : DatabaseError()
        object DataCorruption : DatabaseError()
        data class MigrationFailed(val fromVersion: Int, val toVersion: Int) : DatabaseError()
        object TransactionFailed : DatabaseError()
        object DiskSpaceFull : DatabaseError()
    }
    
    // File System Errors
    sealed class FileSystemError : ErrorType() {
        object InsufficientStorage : FileSystemError()
        object PermissionDenied : FileSystemError()
        object FileNotFound : FileSystemError()
        object WriteError : FileSystemError()
        object ReadError : FileSystemError()
        data class InvalidPath(val path: String) : FileSystemError()
    }
    
    // Network Errors (for future use)
    sealed class NetworkError : ErrorType() {
        object NoConnection : NetworkError()
        object Timeout : NetworkError()
        object ServerError : NetworkError()
        object Unauthorized : NetworkError()
    }
    
    // Export Errors
    sealed class ExportError : ErrorType() {
        object DataTooLarge : ExportError()
        data class FormatNotSupported(val format: String) : ExportError()
        object GenerationFailed : ExportError()
    }
    
    // Security Errors
    sealed class SecurityError : ErrorType() {
        object EncryptionFailed : SecurityError()
        object DecryptionFailed : SecurityError()
        object KeyGenerationFailed : SecurityError()
        object IntegrityCheckFailed : SecurityError()
    }
    
    // General Errors
    sealed class GeneralError : ErrorType() {
        data class Unknown(val throwable: Throwable) : GeneralError()
        data class ValidationFailed(val field: String, val reason: String) : GeneralError()
        object OperationCancelled : GeneralError()
    }
}