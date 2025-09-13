package com.expensetracker.presentation.error

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expensetracker.R
import com.expensetracker.domain.error.*

/**
 * Composable dialog for displaying errors with recovery options
 */
@Composable
fun ErrorDialog(
    error: ErrorResult.Error,
    recoveryInstructions: RecoveryInstructions,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    onRecoveryAction: (RecoveryAction) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = getErrorIcon(recoveryInstructions.severity),
                contentDescription = null,
                tint = getErrorColor(recoveryInstructions.severity)
            )
        },
        title = {
            Text(
                text = getErrorTitle(recoveryInstructions.severity),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                Text(
                    text = recoveryInstructions.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Recovery instructions if available
                if (recoveryInstructions.recoveryAction != null) {
                    RecoveryInstructionsSection(
                        instructions = recoveryInstructions,
                        onRecoveryAction = onRecoveryAction
                    )
                }
                
                // Auto-recovery indicator
                if (recoveryInstructions.canAutoRecover) {
                    AutoRecoveryIndicator(
                        estimatedTime = recoveryInstructions.estimatedRecoveryTime
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recovery action button
                recoveryInstructions.recoveryAction?.let { action ->
                    Button(
                        onClick = { onRecoveryAction(action) }
                    ) {
                        Text(getRecoveryActionText(action))
                    }
                }
                
                // Retry button for recoverable errors
                if (error.isRecoverable) {
                    OutlinedButton(
                        onClick = onRetry
                    ) {
                        Text(stringResource(R.string.recovery_retry))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun RecoveryInstructionsSection(
    instructions: RecoveryInstructions,
    onRecoveryAction: (RecoveryAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recovery Steps:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            instructions.steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}. ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            if (instructions.estimatedRecoveryTime.isNotEmpty()) {
                Text(
                    text = "Estimated time: ${instructions.estimatedRecoveryTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AutoRecoveryIndicator(
    estimatedTime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Column {
                Text(
                    text = "Auto-recovery available",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This error can be automatically resolved in $estimatedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun getErrorIcon(severity: ErrorSeverity): ImageVector {
    return when (severity) {
        ErrorSeverity.CRITICAL -> Icons.Default.Error
        ErrorSeverity.HIGH -> Icons.Default.Warning
        ErrorSeverity.WARNING -> Icons.Default.Warning
        ErrorSeverity.MEDIUM -> Icons.Default.Info
        ErrorSeverity.LOW -> Icons.Default.Info
    }
}

@Composable
private fun getErrorColor(severity: ErrorSeverity): androidx.compose.ui.graphics.Color {
    return when (severity) {
        ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        ErrorSeverity.HIGH -> MaterialTheme.colorScheme.error
        ErrorSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
        ErrorSeverity.MEDIUM -> MaterialTheme.colorScheme.primary
        ErrorSeverity.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun getErrorTitle(severity: ErrorSeverity): String {
    return when (severity) {
        ErrorSeverity.CRITICAL -> "Critical Error"
        ErrorSeverity.HIGH -> "Error"
        ErrorSeverity.WARNING -> "Warning"
        ErrorSeverity.MEDIUM -> "Issue Detected"
        ErrorSeverity.LOW -> "Notice"
    }
}

@Composable
private fun getRecoveryActionText(action: RecoveryAction): String {
    return when (action) {
        RecoveryAction.RETRY -> stringResource(R.string.recovery_retry)
        RecoveryAction.REQUEST_PERMISSION -> stringResource(R.string.recovery_grant_permission)
        RecoveryAction.MANUAL_ENTRY -> stringResource(R.string.recovery_manual_entry)
        RecoveryAction.REPORT_ISSUE -> stringResource(R.string.recovery_report_issue)
        RecoveryAction.RESTORE_BACKUP -> stringResource(R.string.recovery_restore_backup)
        RecoveryAction.FREE_SPACE -> stringResource(R.string.recovery_free_space)
        RecoveryAction.REINSTALL_APP -> stringResource(R.string.recovery_reinstall)
        RecoveryAction.CHECK_CONNECTION -> stringResource(R.string.recovery_check_connection)
        RecoveryAction.RETRY_LATER -> stringResource(R.string.recovery_try_later)
        RecoveryAction.REDUCE_DATA_SIZE -> stringResource(R.string.recovery_reduce_data)
        RecoveryAction.CORRECT_INPUT -> stringResource(R.string.recovery_correct_input)
    }
}

/**
 * Composable for displaying error snackbars with recovery actions
 */
@Composable
fun ErrorSnackbar(
    error: ErrorResult.Error,
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(error) {
        val result = snackbarHostState.showSnackbar(
            message = error.message,
            actionLabel = if (error.isRecoverable) "Retry" else null,
            duration = SnackbarDuration.Long
        )
        
        when (result) {
            SnackbarResult.ActionPerformed -> onRetry()
            SnackbarResult.Dismissed -> onDismiss()
        }
    }
    
    SnackbarHost(hostState = snackbarHostState)
}