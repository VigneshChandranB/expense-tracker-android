package com.expensetracker.domain.error

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.expensetracker.data.file.FileSystemErrorHandler
import com.expensetracker.data.local.error.DatabaseErrorHandler
import com.expensetracker.data.sms.SmsErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error recovery manager that coordinates recovery actions across the app
 */
@Singleton
class ErrorRecoveryManager @Inject constructor(
    private val context: Context,
    private val errorHandler: ErrorHandler,
    private val smsErrorHandler: SmsErrorHandler,
    private val databaseErrorHandler: DatabaseErrorHandler,
    private val fileSystemErrorHandler: FileSystemErrorHandler
) {
    
    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()
    
    /**
     * Attempts to recover from an error automatically
     */
    suspend fun attemptRecovery(error: ErrorResult.Error): RecoveryResult {
        _recoveryState.value = RecoveryState.InProgress(error.errorType)
        
        val result = when (error.errorType) {
            // SMS Errors
            is ErrorType.SmsError -> recoverFromSmsError(error)
            
            // Database Errors
            is ErrorType.DatabaseError -> recoverFromDatabaseError(error)
            
            // File System Errors
            is ErrorType.FileSystemError -> recoverFromFileSystemError(error)
            
            // Security Errors
            is ErrorType.SecurityError -> recoverFromSecurityError(error)
            
            // Export Errors
            is ErrorType.ExportError -> recoverFromExportError(error)
            
            // General Errors
            is ErrorType.GeneralError -> recoverFromGeneralError(error)
            
            else -> RecoveryResult.Failed("No recovery strategy available")
        }
        
        _recoveryState.value = when (result) {
            is RecoveryResult.Success -> RecoveryState.Success(error.errorType)
            is RecoveryResult.Failed -> RecoveryState.Failed(error.errorType, result.reason)
            is RecoveryResult.RequiresUserAction -> RecoveryState.RequiresUserAction(
                error.errorType, 
                result.action, 
                result.instructions
            )
        }
        
        return result
    }
    
    /**
     * Provides user-friendly recovery instructions
     */
    fun getRecoveryInstructions(errorType: ErrorType): RecoveryInstructions {
        val recoveryAction = errorHandler.getRecoveryAction(errorType)
        val severity = errorHandler.getErrorSeverity(errorType)
        val message = errorHandler.getErrorMessage(errorType)
        
        return RecoveryInstructions(
            errorType = errorType,
            message = message,
            severity = severity,
            recoveryAction = recoveryAction,
            steps = getRecoverySteps(recoveryAction),
            canAutoRecover = canAutoRecover(errorType),
            estimatedRecoveryTime = getEstimatedRecoveryTime(recoveryAction)
        )
    }
    
    /**
     * Executes user-initiated recovery action
     */
    suspend fun executeUserRecovery(
        errorType: ErrorType,
        action: RecoveryAction
    ): RecoveryResult {
        return when (action) {
            RecoveryAction.RETRY -> RecoveryResult.Success("Ready to retry")
            
            RecoveryAction.REQUEST_PERMISSION -> {
                requestPermissions(errorType)
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.REQUEST_PERMISSION,
                    "Please grant the required permissions in the settings that opened"
                )
            }
            
            RecoveryAction.MANUAL_ENTRY -> RecoveryResult.Success("Switch to manual entry mode")
            
            RecoveryAction.REPORT_ISSUE -> {
                openIssueReporting(errorType)
                RecoveryResult.Success("Issue reporting opened")
            }
            
            RecoveryAction.RESTORE_BACKUP -> RecoveryResult.RequiresUserAction(
                RecoveryAction.RESTORE_BACKUP,
                "Please select a backup file to restore from"
            )
            
            RecoveryAction.FREE_SPACE -> {
                openStorageSettings()
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.FREE_SPACE,
                    "Please free up storage space and try again"
                )
            }
            
            RecoveryAction.REINSTALL_APP -> RecoveryResult.RequiresUserAction(
                RecoveryAction.REINSTALL_APP,
                "Please reinstall the app to fix this issue"
            )
            
            RecoveryAction.CHECK_CONNECTION -> RecoveryResult.RequiresUserAction(
                RecoveryAction.CHECK_CONNECTION,
                "Please check your internet connection and try again"
            )
            
            RecoveryAction.RETRY_LATER -> RecoveryResult.Success("Please try again later")
            
            RecoveryAction.REDUCE_DATA_SIZE -> RecoveryResult.RequiresUserAction(
                RecoveryAction.REDUCE_DATA_SIZE,
                "Please select a smaller date range or fewer transactions"
            )
            
            RecoveryAction.CORRECT_INPUT -> RecoveryResult.Success("Please correct the input and try again")
        }
    }
    
    private suspend fun recoverFromSmsError(error: ErrorResult.Error): RecoveryResult {
        return when (error.errorType) {
            is ErrorType.SmsError.ProcessingTimeout -> {
                // Retry with longer timeout
                RecoveryResult.Success("Retrying with extended timeout")
            }
            
            is ErrorType.SmsError.InvalidFormat,
            is ErrorType.SmsError.AmountParsingFailed -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.MANUAL_ENTRY,
                    "SMS format not recognized. Please add transaction manually."
                )
            }
            
            is ErrorType.SmsError.UnknownBankFormat -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.REPORT_ISSUE,
                    "Unknown bank SMS format. Please report this to help us improve."
                )
            }
            
            else -> RecoveryResult.Failed("Cannot auto-recover from SMS error")
        }
    }
    
    private suspend fun recoverFromDatabaseError(error: ErrorResult.Error): RecoveryResult {
        return when (error.errorType) {
            is ErrorType.DatabaseError.ConnectionFailed,
            is ErrorType.DatabaseError.TransactionFailed -> {
                // Retry after brief delay
                RecoveryResult.Success("Retrying database operation")
            }
            
            is ErrorType.DatabaseError.DiskSpaceFull -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.FREE_SPACE,
                    "Storage full. Please free up space to continue."
                )
            }
            
            is ErrorType.DatabaseError.DataCorruption -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.RESTORE_BACKUP,
                    "Database corrupted. Restore from backup or reinstall app."
                )
            }
            
            else -> RecoveryResult.Failed("Cannot auto-recover from database error")
        }
    }
    
    private suspend fun recoverFromFileSystemError(error: ErrorResult.Error): RecoveryResult {
        return when (error.errorType) {
            is ErrorType.FileSystemError.InsufficientStorage -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.FREE_SPACE,
                    "Not enough storage space. Please free up space."
                )
            }
            
            is ErrorType.FileSystemError.PermissionDenied -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.REQUEST_PERMISSION,
                    "Storage permission required. Please grant permission."
                )
            }
            
            is ErrorType.FileSystemError.WriteError,
            is ErrorType.FileSystemError.ReadError -> {
                RecoveryResult.Success("Retrying file operation")
            }
            
            else -> RecoveryResult.Failed("Cannot auto-recover from file system error")
        }
    }
    
    private suspend fun recoverFromSecurityError(error: ErrorResult.Error): RecoveryResult {
        return RecoveryResult.RequiresUserAction(
            RecoveryAction.REINSTALL_APP,
            "Security error detected. Please reinstall the app for safety."
        )
    }
    
    private suspend fun recoverFromExportError(error: ErrorResult.Error): RecoveryResult {
        return when (error.errorType) {
            is ErrorType.ExportError.DataTooLarge -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.REDUCE_DATA_SIZE,
                    "Export data too large. Please select a smaller date range."
                )
            }
            
            is ErrorType.ExportError.GenerationFailed -> {
                RecoveryResult.Success("Retrying export generation")
            }
            
            else -> RecoveryResult.Failed("Cannot auto-recover from export error")
        }
    }
    
    private suspend fun recoverFromGeneralError(error: ErrorResult.Error): RecoveryResult {
        return when (error.errorType) {
            is ErrorType.GeneralError.ValidationFailed -> {
                RecoveryResult.RequiresUserAction(
                    RecoveryAction.CORRECT_INPUT,
                    "Please correct the input: ${error.errorType.reason}"
                )
            }
            
            is ErrorType.GeneralError.Unknown -> {
                RecoveryResult.Success("Retrying operation")
            }
            
            else -> RecoveryResult.Failed("Cannot auto-recover from general error")
        }
    }
    
    private fun canAutoRecover(errorType: ErrorType): Boolean {
        return when (errorType) {
            is ErrorType.SmsError.ProcessingTimeout,
            is ErrorType.DatabaseError.ConnectionFailed,
            is ErrorType.DatabaseError.TransactionFailed,
            is ErrorType.FileSystemError.WriteError,
            is ErrorType.FileSystemError.ReadError,
            is ErrorType.ExportError.GenerationFailed,
            is ErrorType.GeneralError.Unknown -> true
            
            else -> false
        }
    }
    
    private fun getRecoverySteps(action: RecoveryAction?): List<String> {
        return when (action) {
            RecoveryAction.REQUEST_PERMISSION -> listOf(
                "Tap 'Grant Permission' button",
                "Allow the requested permission in system settings",
                "Return to the app and try again"
            )
            
            RecoveryAction.FREE_SPACE -> listOf(
                "Open device storage settings",
                "Delete unnecessary files or apps",
                "Clear app caches if needed",
                "Return to the app and try again"
            )
            
            RecoveryAction.RESTORE_BACKUP -> listOf(
                "Go to app settings",
                "Select 'Restore from Backup'",
                "Choose a recent backup file",
                "Wait for restore to complete"
            )
            
            RecoveryAction.REINSTALL_APP -> listOf(
                "Uninstall the app from device settings",
                "Download and install the latest version",
                "Set up the app again with your preferences"
            )
            
            else -> listOf("Follow the instructions provided")
        }
    }
    
    private fun getEstimatedRecoveryTime(action: RecoveryAction?): String {
        return when (action) {
            RecoveryAction.RETRY -> "A few seconds"
            RecoveryAction.REQUEST_PERMISSION -> "1-2 minutes"
            RecoveryAction.FREE_SPACE -> "5-10 minutes"
            RecoveryAction.RESTORE_BACKUP -> "2-5 minutes"
            RecoveryAction.REINSTALL_APP -> "5-10 minutes"
            else -> "Varies"
        }
    }
    
    private fun requestPermissions(errorType: ErrorType) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    private fun openStorageSettings() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    private fun openIssueReporting(errorType: ErrorType) {
        // In a real app, this would open a bug reporting system
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@expensetracker.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Error Report: ${errorType::class.simpleName}")
            putExtra(Intent.EXTRA_TEXT, "Error details: ${errorType}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val chooser = Intent.createChooser(intent, "Report Issue")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }
}

sealed class RecoveryState {
    object Idle : RecoveryState()
    data class InProgress(val errorType: ErrorType) : RecoveryState()
    data class Success(val errorType: ErrorType) : RecoveryState()
    data class Failed(val errorType: ErrorType, val reason: String) : RecoveryState()
    data class RequiresUserAction(
        val errorType: ErrorType,
        val action: RecoveryAction,
        val instructions: String
    ) : RecoveryState()
}

sealed class RecoveryResult {
    data class Success(val message: String) : RecoveryResult()
    data class Failed(val reason: String) : RecoveryResult()
    data class RequiresUserAction(
        val action: RecoveryAction,
        val instructions: String
    ) : RecoveryResult()
}

data class RecoveryInstructions(
    val errorType: ErrorType,
    val message: String,
    val severity: ErrorSeverity,
    val recoveryAction: RecoveryAction?,
    val steps: List<String>,
    val canAutoRecover: Boolean,
    val estimatedRecoveryTime: String
)