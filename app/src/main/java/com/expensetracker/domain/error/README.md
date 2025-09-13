# Error Handling System

This document describes the comprehensive error handling and recovery system implemented in the Expense Tracker app.

## Overview

The error handling system provides:
- Centralized error type definitions
- Automatic error recovery mechanisms
- User-friendly error messages and recovery instructions
- Comprehensive logging and monitoring
- Retry logic with exponential backoff
- Graceful degradation for non-critical failures

## Architecture

### Core Components

1. **ErrorType** - Sealed class hierarchy defining all possible error types
2. **ErrorResult** - Wrapper for operation results that can succeed or fail
3. **ErrorHandler** - Central error handler for user-friendly messages
4. **ErrorRecoveryManager** - Coordinates automatic and manual error recovery
5. **ErrorUtils** - Utility functions for error handling operations
6. **Specific Error Handlers** - Domain-specific error handling (SMS, Database, FileSystem)

### Error Flow

```
Operation → Exception → ErrorType → ErrorResult → Recovery → User Action
```

## Error Types

### SMS Processing Errors
- `PermissionDenied` - SMS permission not granted
- `InvalidFormat` - SMS message format not recognized
- `AmountParsingFailed` - Unable to extract transaction amount
- `UnknownBankFormat` - Unknown bank SMS format
- `ProcessingTimeout` - SMS processing timed out
- `PatternMatchFailed` - SMS pattern matching failed

### Database Errors
- `ConnectionFailed` - Database connection failed
- `ConstraintViolation` - Database constraint violation
- `DataCorruption` - Database corruption detected
- `MigrationFailed` - Database migration failed
- `TransactionFailed` - Database transaction failed
- `DiskSpaceFull` - Insufficient disk space

### File System Errors
- `InsufficientStorage` - Not enough storage space
- `PermissionDenied` - File permission denied
- `FileNotFound` - File not found
- `WriteError` - File write error
- `ReadError` - File read error
- `InvalidPath` - Invalid file path

### Network Errors
- `NoConnection` - No internet connection
- `Timeout` - Network timeout
- `ServerError` - Server error
- `Unauthorized` - Unauthorized access

### Export Errors
- `DataTooLarge` - Export data too large
- `FormatNotSupported` - Export format not supported
- `GenerationFailed` - Export generation failed

### Security Errors
- `EncryptionFailed` - Data encryption failed
- `DecryptionFailed` - Data decryption failed
- `KeyGenerationFailed` - Security key generation failed
- `IntegrityCheckFailed` - Data integrity check failed

### General Errors
- `Unknown` - Unknown error occurred
- `ValidationFailed` - Input validation failed
- `OperationCancelled` - Operation was cancelled

## Error Severity Levels

- **CRITICAL** - Security errors, data corruption
- **HIGH** - Disk space full, major functionality broken
- **WARNING** - Permission denied, format issues
- **MEDIUM** - Network errors, export failures
- **LOW** - Minor validation errors, temporary issues

## Recovery Actions

### Automatic Recovery
- **RETRY** - Retry the operation automatically
- **RETRY_WITH_BACKOFF** - Retry with exponential backoff

### User-Initiated Recovery
- **REQUEST_PERMISSION** - Request required permissions
- **MANUAL_ENTRY** - Switch to manual data entry
- **REPORT_ISSUE** - Report the issue to support
- **RESTORE_BACKUP** - Restore from backup
- **FREE_SPACE** - Free up storage space
- **REINSTALL_APP** - Reinstall the application
- **CHECK_CONNECTION** - Check network connection
- **REDUCE_DATA_SIZE** - Reduce data size for operation
- **CORRECT_INPUT** - Correct user input

## Usage Examples

### Basic Error Handling

```kotlin
// Using safeCall for simple operations
val result = safeCall(
    errorType = ErrorType.SmsError.ProcessingTimeout(5000L),
    message = "SMS processing failed"
) {
    processSmsMessage(smsMessage)
}

when (result) {
    is ErrorResult.Success -> handleSuccess(result.data)
    is ErrorResult.Error -> handleError(result)
}
```

### Error Recovery

```kotlin
// Automatic error recovery
val recoveryResult = errorRecoveryManager.attemptRecovery(error)

when (recoveryResult) {
    is RecoveryResult.Success -> {
        // Error was automatically recovered
        retryOperation()
    }
    is RecoveryResult.RequiresUserAction -> {
        // Show user recovery instructions
        showRecoveryDialog(recoveryResult.action, recoveryResult.instructions)
    }
    is RecoveryResult.Failed -> {
        // Recovery failed, show error to user
        showErrorDialog(error)
    }
}
```

### SMS Error Handling with Retry

```kotlin
// SMS processing with automatic retry
val result = smsErrorHandler.processWithRetry(smsMessage) { message ->
    // Your SMS processing logic
    parseTransactionFromSms(message)
}

result.onSuccess { transaction ->
    // Handle successful transaction
    saveTransaction(transaction)
}.onError { error ->
    // Handle error with recovery
    handleSmsError(error)
}
```

### Database Operations with Error Handling

```kotlin
// Database operation with transaction and retry
val result = databaseErrorHandler.executeTransaction(database) {
    // Your database operations
    transactionDao.insert(transaction)
    accountDao.updateBalance(accountId, newBalance)
}

if (result is ErrorResult.Error) {
    when (result.errorType) {
        is ErrorType.DatabaseError.DiskSpaceFull -> {
            showStorageFullDialog()
        }
        is ErrorType.DatabaseError.ConstraintViolation -> {
            showValidationErrorDialog()
        }
        else -> {
            // Generic error handling
            showGenericErrorDialog(result)
        }
    }
}
```

### File Operations with Backup

```kotlin
// File operation with automatic backup
val backupResult = fileSystemErrorHandler.createFileBackup(filePath)
if (backupResult is ErrorResult.Success) {
    val backupPath = backupResult.data
    
    val writeResult = fileSystemErrorHandler.writeFile(filePath, content)
    if (writeResult is ErrorResult.Error) {
        // Restore from backup on failure
        fileSystemErrorHandler.restoreFromBackup(filePath, backupPath)
    }
}
```

### Error Logging

```kotlin
// Comprehensive error logging
errorUtils.logError(
    error = error,
    context = "SMS Processing",
    additionalData = mapOf(
        "smsId" to smsMessage.id,
        "sender" to smsMessage.sender,
        "bodyLength" to smsMessage.body.length
    )
)
```

### Error Dialog Display

```kotlin
// Show error dialog with recovery options
@Composable
fun HandleError(error: ErrorResult.Error) {
    val instructions = errorRecoveryManager.getRecoveryInstructions(error.errorType)
    
    ErrorDialog(
        error = error,
        recoveryInstructions = instructions,
        onDismiss = { /* Handle dismiss */ },
        onRetry = { /* Handle retry */ },
        onRecoveryAction = { action ->
            // Handle recovery action
            errorRecoveryManager.executeUserRecovery(error.errorType, action)
        }
    )
}
```

## Best Practices

### Error Handling
1. Always use `ErrorResult` for operations that can fail
2. Map exceptions to appropriate `ErrorType` instances
3. Provide meaningful error messages for users
4. Log errors with appropriate severity levels
5. Implement retry logic for transient failures

### Recovery Implementation
1. Distinguish between recoverable and non-recoverable errors
2. Provide clear recovery instructions to users
3. Implement automatic recovery for appropriate error types
4. Test recovery mechanisms thoroughly
5. Monitor recovery success rates

### User Experience
1. Show user-friendly error messages, not technical details
2. Provide actionable recovery steps
3. Indicate when errors can be automatically resolved
4. Allow users to retry operations when appropriate
5. Gracefully degrade functionality when possible

### Testing
1. Test all error scenarios and recovery paths
2. Verify error messages are user-friendly
3. Test retry logic and backoff strategies
4. Validate error logging and monitoring
5. Test error dialogs and user interactions

## Monitoring and Analytics

The error handling system supports:
- Error frequency tracking
- Recovery success rate monitoring
- Error severity distribution
- User action effectiveness
- Performance impact analysis

## Configuration

Error handling behavior can be configured through:
- Retry attempt limits
- Backoff delay intervals
- Error severity thresholds
- Recovery timeout values
- Logging verbosity levels

## Future Enhancements

Planned improvements include:
- Machine learning for error prediction
- Adaptive retry strategies
- Enhanced error analytics
- Automated error reporting
- Context-aware recovery suggestions