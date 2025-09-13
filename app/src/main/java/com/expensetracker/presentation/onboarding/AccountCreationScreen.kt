package com.expensetracker.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.AccountType
import com.expensetracker.presentation.theme.ExpenseTrackerTheme

/**
 * Account creation screen - allows users to create their first account or skip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCreationScreen(
    onCreateAccount: (String, AccountType, String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var bankName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(AccountType.CHECKING) }
    var nickname by remember { mutableStateOf("") }
    var showAccountTypeDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Add Your First Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Set up your first bank account to start tracking expenses. You can add more accounts later.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Account form
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Bank name field
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name") },
                placeholder = { Text("e.g., HDFC Bank, SBI, ICICI") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Account type selector
            OutlinedTextField(
                value = getAccountTypeDisplayName(accountType),
                onValueChange = { },
                label = { Text("Account Type") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showAccountTypeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select account type"
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = getAccountTypeIcon(accountType),
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Nickname field
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Account Nickname (Optional)") },
                placeholder = { Text("e.g., Primary Checking, Salary Account") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Info card
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Account Setup Tips",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "• You can add multiple accounts from the same bank\n• Account nicknames help you identify accounts easily\n• You can edit account details anytime in settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Button(
                onClick = {
                    val finalNickname = nickname.ifBlank { 
                        "${getAccountTypeDisplayName(accountType)} Account" 
                    }
                    onCreateAccount(bankName, accountType, finalNickname)
                },
                enabled = bankName.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = "Skip for Now")
            }
            
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = "Back")
            }
        }
    }
    
    // Account type selection dialog
    if (showAccountTypeDialog) {
        AccountTypeSelectionDialog(
            selectedType = accountType,
            onTypeSelected = { 
                accountType = it
                showAccountTypeDialog = false
            },
            onDismiss = { showAccountTypeDialog = false }
        )
    }
}

@Composable
private fun AccountTypeSelectionDialog(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Account Type",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                AccountType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = type == selectedType,
                                onClick = { onTypeSelected(type) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = type == selectedType,
                            onClick = { onTypeSelected(type) }
                        )
                        
                        Icon(
                            imageVector = getAccountTypeIcon(type),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Column {
                            Text(
                                text = getAccountTypeDisplayName(type),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getAccountTypeDescription(type),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private fun getAccountTypeDisplayName(type: AccountType): String {
    return when (type) {
        AccountType.CHECKING -> "Checking Account"
        AccountType.SAVINGS -> "Savings Account"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.INVESTMENT -> "Investment Account"
        AccountType.CASH -> "Cash Account"
    }
}

private fun getAccountTypeDescription(type: AccountType): String {
    return when (type) {
        AccountType.CHECKING -> "For daily transactions and expenses"
        AccountType.SAVINGS -> "For saving money and earning interest"
        AccountType.CREDIT_CARD -> "For credit purchases and payments"
        AccountType.INVESTMENT -> "For stocks, mutual funds, and investments"
        AccountType.CASH -> "For cash transactions and petty expenses"
    }
}

private fun getAccountTypeIcon(type: AccountType): ImageVector {
    return when (type) {
        AccountType.CHECKING -> Icons.Default.AccountBalance
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.INVESTMENT -> Icons.Default.TrendingUp
        AccountType.CASH -> Icons.Default.Money
    }
}

@Preview(showBackground = true)
@Composable
fun AccountCreationScreenPreview() {
    ExpenseTrackerTheme {
        AccountCreationScreen(
            onCreateAccount = { _, _, _ -> },
            onSkip = {},
            onBack = {},
            isLoading = false
        )
    }
}