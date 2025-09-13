package com.expensetracker.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountNotificationSettings
import java.math.BigDecimal

/**
 * Screen for managing notification settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.onEvent(NotificationSettingsEvent.LoadSettings)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(NotificationSettingsEvent.SaveSettings) }
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission status
                item {
                    PermissionStatusCard(
                        isGranted = uiState.isNotificationPermissionGranted,
                        onRequestPermission = {
                            viewModel.onEvent(NotificationSettingsEvent.RequestNotificationPermission)
                        }
                    )
                }
                
                // General Settings
                item {
                    GeneralNotificationSettings(
                        preferences = uiState.preferences,
                        onEvent = viewModel::onEvent
                    )
                }
                
                // Account-specific settings
                if (uiState.accounts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Account-Specific Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(uiState.accounts) { account ->
                        AccountNotificationSettingsCard(
                            account = account,
                            settings = uiState.accountSettings[account.id] ?: AccountNotificationSettings(account.id),
                            onEvent = viewModel::onEvent
                        )
                    }
                }
            }
        }
        
        // Error handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar or dialog
                viewModel.onEvent(NotificationSettingsEvent.ClearError)
            }
        }
        
        // Success message
        uiState.successMessage?.let { message ->
            LaunchedEffect(message) {
                // Show success snackbar
                viewModel.onEvent(NotificationSettingsEvent.ClearSuccessMessage)
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Notification Permission",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isGranted) {
                    "Notifications are enabled. You'll receive alerts based on your preferences below."
                } else {
                    "Notifications are disabled. Enable them to receive important financial alerts."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Notifications")
                }
            }
        }
    }
}

@Composable
private fun GeneralNotificationSettings(
    preferences: com.expensetracker.domain.model.NotificationPreferences,
    onEvent: (NotificationSettingsEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "General Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bill Reminders
            SettingRow(
                title = "Bill Reminders",
                subtitle = "Get notified before bills are due",
                checked = preferences.billRemindersEnabled,
                onCheckedChange = { onEvent(NotificationSettingsEvent.UpdateBillRemindersEnabled(it)) }
            )
            
            if (preferences.billRemindersEnabled) {
                NumberSettingRow(
                    title = "Remind me",
                    value = preferences.billReminderDaysBefore,
                    suffix = "days before",
                    onValueChange = { onEvent(NotificationSettingsEvent.UpdateBillReminderDays(it)) }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Spending Limit Alerts
            SettingRow(
                title = "Spending Limit Alerts",
                subtitle = "Alert when approaching spending limits",
                checked = preferences.spendingLimitAlertsEnabled,
                onCheckedChange = { onEvent(NotificationSettingsEvent.UpdateSpendingLimitAlertsEnabled(it)) }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Low Balance Warnings
            SettingRow(
                title = "Low Balance Warnings",
                subtitle = "Alert when account balance is low",
                checked = preferences.lowBalanceWarningsEnabled,
                onCheckedChange = { onEvent(NotificationSettingsEvent.UpdateLowBalanceWarningsEnabled(it)) }
            )
            
            if (preferences.lowBalanceWarningsEnabled) {
                AmountSettingRow(
                    title = "Low balance threshold",
                    value = preferences.lowBalanceThreshold,
                    onValueChange = { onEvent(NotificationSettingsEvent.UpdateLowBalanceThreshold(it)) }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Unusual Spending Alerts
            SettingRow(
                title = "Unusual Spending Alerts",
                subtitle = "Detect and alert on unusual spending patterns",
                checked = preferences.unusualSpendingAlertsEnabled,
                onCheckedChange = { onEvent(NotificationSettingsEvent.UpdateUnusualSpendingAlertsEnabled(it)) }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Large Transaction Alerts
            SettingRow(
                title = "Large Transaction Alerts",
                subtitle = "Alert for transactions above threshold",
                checked = preferences.largeTransactionAlertsEnabled,
                onCheckedChange = { onEvent(NotificationSettingsEvent.UpdateLargeTransactionAlertsEnabled(it)) }
            )
            
            if (preferences.largeTransactionAlertsEnabled) {
                AmountSettingRow(
                    title = "Large transaction threshold",
                    value = preferences.largeTransactionThreshold,
                    onValueChange = { onEvent(NotificationSettingsEvent.UpdateLargeTransactionThreshold(it)) }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Quiet Hours
            SettingRow(
                title = "Quiet Hours",
                subtitle = "Delay notifications during specified hours",
                checked = preferences.quietHoursEnabled,
                onCheckedChange = { onEvent(NotificationSettingsEvent.UpdateQuietHoursEnabled(it)) }
            )
            
            if (preferences.quietHoursEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeSettingRow(
                        title = "Start",
                        hour = preferences.quietHoursStart,
                        onHourChange = { onEvent(NotificationSettingsEvent.UpdateQuietHoursStart(it)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TimeSettingRow(
                        title = "End",
                        hour = preferences.quietHoursEnd,
                        onHourChange = { onEvent(NotificationSettingsEvent.UpdateQuietHoursEnd(it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountNotificationSettingsCard(
    account: Account,
    settings: AccountNotificationSettings,
    onEvent: (NotificationSettingsEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = account.nickname,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${account.bankName} • ${account.accountType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Spending Limit
            SettingRow(
                title = "Spending Limit Alerts",
                subtitle = "Account-specific spending limit alerts",
                checked = settings.spendingLimitEnabled,
                onCheckedChange = { 
                    onEvent(NotificationSettingsEvent.UpdateAccountSpendingLimitEnabled(account.id, it))
                }
            )
            
            if (settings.spendingLimitEnabled) {
                AmountSettingRow(
                    title = "Monthly spending limit",
                    value = settings.spendingLimit ?: BigDecimal.ZERO,
                    onValueChange = { 
                        onEvent(NotificationSettingsEvent.UpdateAccountSpendingLimit(account.id, it))
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Low Balance
            SettingRow(
                title = "Low Balance Warnings",
                subtitle = "Account-specific low balance threshold",
                checked = settings.lowBalanceEnabled,
                onCheckedChange = { 
                    onEvent(NotificationSettingsEvent.UpdateAccountLowBalanceEnabled(account.id, it))
                }
            )
            
            if (settings.lowBalanceEnabled) {
                AmountSettingRow(
                    title = "Low balance threshold",
                    value = settings.lowBalanceThreshold ?: BigDecimal.ZERO,
                    onValueChange = { 
                        onEvent(NotificationSettingsEvent.UpdateAccountLowBalanceThreshold(account.id, it))
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Unusual Spending
            SettingRow(
                title = "Unusual Spending Detection",
                subtitle = "Detect unusual patterns for this account",
                checked = settings.unusualSpendingEnabled,
                onCheckedChange = { 
                    onEvent(NotificationSettingsEvent.UpdateAccountUnusualSpendingEnabled(account.id, it))
                }
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NumberSettingRow(
    title: String,
    value: Int,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$value $suffix")
            // In a real implementation, you'd add increment/decrement buttons or a slider
        }
    }
}

@Composable
private fun AmountSettingRow(
    title: String,
    value: BigDecimal,
    onValueChange: (BigDecimal) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Text("₹${value}")
        // In a real implementation, you'd add an input field or dialog
    }
}

@Composable
private fun TimeSettingRow(
    title: String,
    hour: Int,
    onHourChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = String.format("%02d:00", hour),
            style = MaterialTheme.typography.bodyLarge
        )
        // In a real implementation, you'd add a time picker
    }
}