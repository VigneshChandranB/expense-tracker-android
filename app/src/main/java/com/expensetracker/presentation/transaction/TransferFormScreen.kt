package com.expensetracker.presentation.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter

/**
 * Screen for creating transfers between accounts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFromAccountDropdown by remember { mutableStateOf(false) }
    var showToAccountDropdown by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.onEvent(TransferFormEvent.LoadAccounts)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Transfer Money",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Amount field
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.onEvent(TransferFormEvent.UpdateAmount(it)) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.validationErrors.amount != null,
                supportingText = uiState.validationErrors.amount?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // From Account dropdown
            ExposedDropdownMenuBox(
                expanded = showFromAccountDropdown,
                onExpandedChange = { showFromAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = uiState.fromAccount?.nickname ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("From Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFromAccountDropdown) },
                    isError = uiState.validationErrors.fromAccount != null,
                    supportingText = uiState.validationErrors.fromAccount?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = showFromAccountDropdown,
                    onDismissRequest = { showFromAccountDropdown = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(account.nickname)
                                    Text(
                                        text = "${account.bankName} - ${account.accountType.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.onEvent(TransferFormEvent.UpdateFromAccount(account))
                                showFromAccountDropdown = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer direction indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = "Transfer direction",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // To Account dropdown
            ExposedDropdownMenuBox(
                expanded = showToAccountDropdown,
                onExpandedChange = { showToAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = uiState.toAccount?.nickname ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("To Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToAccountDropdown) },
                    isError = uiState.validationErrors.toAccount != null || uiState.validationErrors.sameAccount != null,
                    supportingText = (uiState.validationErrors.toAccount ?: uiState.validationErrors.sameAccount)?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = showToAccountDropdown,
                    onDismissRequest = { showToAccountDropdown = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(account.nickname)
                                    Text(
                                        text = "${account.bankName} - ${account.accountType.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.onEvent(TransferFormEvent.UpdateToAccount(account))
                                showToAccountDropdown = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description field
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onEvent(TransferFormEvent.UpdateDescription(it)) },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date field (simplified - in a real app you'd use a date picker)
            OutlinedTextField(
                value = uiState.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                onValueChange = { },
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { /* Open date picker */ }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select date")
                    }
                },
                isError = uiState.validationErrors.date != null,
                supportingText = uiState.validationErrors.date?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Transfer summary card
            if (uiState.fromAccount != null && uiState.toAccount != null && uiState.amount.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Transfer Summary",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Amount: ${uiState.amount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "From: ${uiState.fromAccount!!.nickname}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "To: ${uiState.toAccount!!.nickname}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Save button
            Button(
                onClick = { viewModel.onEvent(TransferFormEvent.SaveTransfer) },
                enabled = uiState.isValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Transfer")
                }
            }
        }
    }
    
    // Success handling
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.error == null && 
            uiState.amount.isBlank() && uiState.fromAccount == null && uiState.toAccount == null) {
            // Form was reset, indicating successful transfer
            onNavigateBack()
        }
    }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            viewModel.clearError()
        }
    }
}