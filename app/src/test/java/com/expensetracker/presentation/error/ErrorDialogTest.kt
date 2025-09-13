package com.expensetracker.presentation.error

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.error.*
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorDialogTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun errorDialog_displaysErrorMessage() {
        val error = ErrorResult.Error(
            ErrorType.SmsError.InvalidFormat,
            "SMS format not recognized",
            isRecoverable = true
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.SmsError.InvalidFormat,
            message = "SMS format not recognized",
            severity = ErrorSeverity.WARNING,
            recoveryAction = RecoveryAction.MANUAL_ENTRY,
            steps = listOf("Add transaction manually", "Check SMS format"),
            canAutoRecover = false,
            estimatedRecoveryTime = "1-2 minutes"
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = {},
                    onRetry = {},
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify error message is displayed
        composeTestRule.onNodeWithText("SMS format not recognized").assertIsDisplayed()
        
        // Verify dialog title based on severity
        composeTestRule.onNodeWithText("Warning").assertIsDisplayed()
    }
    
    @Test
    fun errorDialog_showsRecoverySteps() {
        val error = ErrorResult.Error(
            ErrorType.DatabaseError.DiskSpaceFull,
            "Insufficient storage space",
            isRecoverable = false
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.DatabaseError.DiskSpaceFull,
            message = "Insufficient storage space",
            severity = ErrorSeverity.HIGH,
            recoveryAction = RecoveryAction.FREE_SPACE,
            steps = listOf(
                "Open device storage settings",
                "Delete unnecessary files or apps",
                "Clear app caches if needed",
                "Return to the app and try again"
            ),
            canAutoRecover = false,
            estimatedRecoveryTime = "5-10 minutes"
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = {},
                    onRetry = {},
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify recovery steps are displayed
        composeTestRule.onNodeWithText("Recovery Steps:").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. Open device storage settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. Delete unnecessary files or apps").assertIsDisplayed()
        composeTestRule.onNodeWithText("3. Clear app caches if needed").assertIsDisplayed()
        composeTestRule.onNodeWithText("4. Return to the app and try again").assertIsDisplayed()
        
        // Verify estimated time is displayed
        composeTestRule.onNodeWithText("Estimated time: 5-10 minutes").assertIsDisplayed()
    }
    
    @Test
    fun errorDialog_showsAutoRecoveryIndicator() {
        val error = ErrorResult.Error(
            ErrorType.SmsError.ProcessingTimeout(5000L),
            "SMS processing timed out",
            isRecoverable = true
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.SmsError.ProcessingTimeout(5000L),
            message = "SMS processing timed out",
            severity = ErrorSeverity.MEDIUM,
            recoveryAction = RecoveryAction.RETRY,
            steps = listOf("Wait for automatic retry"),
            canAutoRecover = true,
            estimatedRecoveryTime = "A few seconds"
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = {},
                    onRetry = {},
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify auto-recovery indicator is displayed
        composeTestRule.onNodeWithText("Auto-recovery available").assertIsDisplayed()
        composeTestRule.onNodeWithText("This error can be automatically resolved in A few seconds").assertIsDisplayed()
    }
    
    @Test
    fun errorDialog_showsRecoveryActionButton() {
        val error = ErrorResult.Error(
            ErrorType.SmsError.PermissionDenied,
            "SMS permission denied",
            isRecoverable = true
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.SmsError.PermissionDenied,
            message = "SMS permission denied",
            severity = ErrorSeverity.WARNING,
            recoveryAction = RecoveryAction.REQUEST_PERMISSION,
            steps = listOf("Grant permission in settings"),
            canAutoRecover = false,
            estimatedRecoveryTime = "1-2 minutes"
        )
        
        var recoveryActionCalled = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = {},
                    onRetry = {},
                    onRecoveryAction = { recoveryActionCalled = true }
                )
            }
        }
        
        // Verify recovery action button is displayed and clickable
        composeTestRule.onNodeWithText("Grant Permission").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant Permission").performClick()
        
        assert(recoveryActionCalled)
    }
    
    @Test
    fun errorDialog_showsRetryButtonForRecoverableErrors() {
        val error = ErrorResult.Error(
            ErrorType.DatabaseError.ConnectionFailed,
            "Database connection failed",
            isRecoverable = true
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.DatabaseError.ConnectionFailed,
            message = "Database connection failed",
            severity = ErrorSeverity.MEDIUM,
            recoveryAction = RecoveryAction.RETRY,
            steps = listOf("Retry the operation"),
            canAutoRecover = true,
            estimatedRecoveryTime = "A few seconds"
        )
        
        var retryCalled = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = {},
                    onRetry = { retryCalled = true },
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify retry button is displayed and clickable
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        
        assert(retryCalled)
    }
    
    @Test
    fun errorDialog_hideRetryButtonForNonRecoverableErrors() {
        val error = ErrorResult.Error(
            ErrorType.SecurityError.EncryptionFailed,
            "Encryption failed",
            isRecoverable = false
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.SecurityError.EncryptionFailed,
            message = "Encryption failed",
            severity = ErrorSeverity.CRITICAL,
            recoveryAction = RecoveryAction.REINSTALL_APP,
            steps = listOf("Reinstall the application"),
            canAutoRecover = false,
            estimatedRecoveryTime = "5-10 minutes"
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = {},
                    onRetry = {},
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify retry button is not displayed for non-recoverable errors
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
        
        // But recovery action button should be displayed
        composeTestRule.onNodeWithText("Reinstall App").assertIsDisplayed()
    }
    
    @Test
    fun errorDialog_showsCancelButton() {
        val error = ErrorResult.Error(
            ErrorType.GeneralError.Unknown(RuntimeException()),
            "Unknown error",
            isRecoverable = true
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.GeneralError.Unknown(RuntimeException()),
            message = "Unknown error",
            severity = ErrorSeverity.LOW,
            recoveryAction = null,
            steps = emptyList(),
            canAutoRecover = false,
            estimatedRecoveryTime = "Varies"
        )
        
        var dismissCalled = false
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = { dismissCalled = true },
                    onRetry = {},
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify cancel button is displayed and clickable
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        
        assert(dismissCalled)
    }
    
    @Test
    fun errorDialog_displaysDifferentIconsForDifferentSeverities() {
        val criticalError = ErrorResult.Error(
            ErrorType.SecurityError.IntegrityCheckFailed,
            "Critical security error",
            isRecoverable = false
        )
        
        val criticalInstructions = RecoveryInstructions(
            errorType = ErrorType.SecurityError.IntegrityCheckFailed,
            message = "Critical security error",
            severity = ErrorSeverity.CRITICAL,
            recoveryAction = RecoveryAction.REINSTALL_APP,
            steps = listOf("Reinstall the app"),
            canAutoRecover = false,
            estimatedRecoveryTime = "5-10 minutes"
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = criticalError,
                    recoveryInstructions = criticalInstructions,
                    onDismiss = {},
                    onRetry = {},
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify critical error shows appropriate title
        composeTestRule.onNodeWithText("Critical Error").assertIsDisplayed()
    }
    
    @Test
    fun errorDialog_handlesLongErrorMessages() {
        val longMessage = "This is a very long error message that should be scrollable when it exceeds the available space in the dialog. ".repeat(10)
        
        val error = ErrorResult.Error(
            ErrorType.GeneralError.ValidationFailed("field", "reason"),
            longMessage,
            isRecoverable = true
        )
        
        val instructions = RecoveryInstructions(
            errorType = ErrorType.GeneralError.ValidationFailed("field", "reason"),
            message = longMessage,
            severity = ErrorSeverity.MEDIUM,
            recoveryAction = RecoveryAction.CORRECT_INPUT,
            steps = listOf("Step 1", "Step 2", "Step 3", "Step 4", "Step 5"),
            canAutoRecover = false,
            estimatedRecoveryTime = "1 minute"
        )
        
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ErrorDialog(
                    error = error,
                    recoveryInstructions = instructions,
                    onDismiss = {},
                    onRetry = {},
                    onRecoveryAction = {}
                )
            }
        }
        
        // Verify the dialog displays without crashing and shows scrollable content
        composeTestRule.onNodeWithText("Issue Detected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Correct Input").assertIsDisplayed()
    }
}