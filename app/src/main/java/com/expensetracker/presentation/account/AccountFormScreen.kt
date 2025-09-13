package com.expensetracker.presentation.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.domain.model.AccountType

/**
 * Screen for creating and editing accounts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val formUiState by viewModel.formUiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Handle successful save
    LaunchedEffect(formUiState.isLoading) {
        if (!formUiState.isLoading && formUiState.error == null && 
            formUiState.bankName.isEmpty() && formUiState.accountNumber.isEmpty()) {
            // Form was reset after successful save
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = if (formUiState.isEditing) "Edit Account" else "Add Account",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error handling
            formUiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.onFormEvent(AccountFormEvent.ClearError) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Bank Name Field
            OutlinedTextField(
                value = formUiState.bankName,
                onValueChange = { viewModel.onFormEvent(AccountFormEvent.BankNameChanged(it)) },
                label = { Text("Bank Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = formUiState.validationErrors.bankNameError != null,
                supportingText = formUiState.validationErrors.bankNameError?.let { { Text(it) } },
                enabled = !formUiState.isLoading
            )

            // Account Type Dropdown
            var accountTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = accountTypeExpanded,
                onExpandedChange = { accountTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = formUiState.accountType.name.replace("_", " "),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Account Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !formUiState.isLoading
                )
                ExposedDropdownMenu(
                    expanded = accountTypeExpanded,
                    onDismissRequest = { accountTypeExpanded = false }
                ) {
                    AccountType.values().forEach { accountType ->
                        DropdownMenuItem(
                            text = { Text(accountType.name.replace("_", " ")) },
                            onClick = {
                                viewModel.onFormEvent(AccountFormEvent.AccountTypeChanged(accountType))
                                accountTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Account Number Field
            OutlinedTextField(
                value = formUiState.accountNumber,
                onValueChange = { viewModel.onFormEvent(AccountFormEvent.AccountNumberChanged(it)) },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                isError = formUiState.validationErrors.accountNumberError != null,
                supportingText = formUiState.validationErrors.accountNumberError?.let { { Text(it) } },
                enabled = !formUiState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Nickname Field
            OutlinedTextField(
                value = formUiState.nickname,
                onValueChange = { viewModel.onFormEvent(AccountFormEvent.NicknameChanged(it)) },
                label = { Text("Account Nickname") },
                modifier = Modifier.fillMaxWidth(),
                isError = formUiState.validationErrors.nicknameError != null,
                supportingText = formUiState.validationErrors.nicknameError?.let { { Text(it) } },
                enabled = !formUiState.isLoading
            )

            // Current Balance Field
            OutlinedTextField(
                value = formUiState.currentBalance,
                onValueChange = { viewModel.onFormEvent(AccountFormEvent.BalanceChanged(it)) },
                label = { Text("Current Balance") },
                modifier = Modifier.fillMaxWidth(),
                isError = formUiState.validationErrors.balanceError != null,
                supportingText = formUiState.validationErrors.balanceError?.let { { Text(it) } },
                enabled = !formUiState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.onFormEvent(AccountFormEvent.CancelEdit) },
                    modifier = Modifier.weight(1f),
                    enabled = !formUiState.isLoading
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { viewModel.onFormEvent(AccountFormEvent.SaveAccount) },
                    modifier = Modifier.weight(1f),
                    enabled = formUiState.isSaveEnabled && !formUiState.isLoading
                ) {
                    if (formUiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (formUiState.isEditing) "Update" else "Create")
                    }
                }
            }
        }
    }
}