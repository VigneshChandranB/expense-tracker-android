package com.expensetracker.presentation.transaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Transaction detail screen with edit capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top app bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(
                    onClick = { onNavigateToEdit(transactionId) },
                    enabled = uiState.transaction != null
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = uiState.transaction != null
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.transaction == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Transaction not found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            else -> {
                TransactionDetailContent(
                    transaction = uiState.transaction!!,
                    account = uiState.account,
                    transferAccount = uiState.transferAccount
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction()
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            viewModel.clearError()
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
    account: Account?,
    transferAccount: Account?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Amount card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (transaction.type) {
                    TransactionType.INCOME, TransactionType.TRANSFER_IN -> 
                        MaterialTheme.colorScheme.primaryContainer
                    TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> 
                        MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatAmount(transaction.amount.toDouble(), transaction.type),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        TransactionType.INCOME, TransactionType.TRANSFER_IN -> 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> 
                            MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                
                Text(
                    text = transaction.type.name.replace("_", " "),
                    style = MaterialTheme.typography.labelLarge,
                    color = when (transaction.type) {
                        TransactionType.INCOME, TransactionType.TRANSFER_IN -> 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> 
                            MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Transaction details
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                DetailRow(
                    label = "Merchant",
                    value = transaction.merchant,
                    icon = Icons.Default.Store
                )
                
                if (!transaction.description.isNullOrBlank()) {
                    DetailRow(
                        label = "Description",
                        value = transaction.description,
                        icon = Icons.Default.Description
                    )
                }
                
                DetailRow(
                    label = "Category",
                    value = transaction.category.name,
                    icon = Icons.Default.Category
                )
                
                DetailRow(
                    label = "Date",
                    value = transaction.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a")),
                    icon = Icons.Default.CalendarToday
                )
                
                account?.let {
                    DetailRow(
                        label = "Account",
                        value = "${it.bankName} - ${it.nickname}",
                        icon = Icons.Default.AccountBalance
                    )
                }
                
                if (transaction.type == TransactionType.TRANSFER_IN || transaction.type == TransactionType.TRANSFER_OUT) {
                    transferAccount?.let {
                        DetailRow(
                            label = if (transaction.type == TransactionType.TRANSFER_OUT) "To Account" else "From Account",
                            value = "${it.bankName} - ${it.nickname}",
                            icon = Icons.Default.SwapHoriz
                        )
                    }
                }
                
                DetailRow(
                    label = "Source",
                    value = when (transaction.source) {
                        com.expensetracker.domain.model.TransactionSource.SMS_AUTO -> "SMS (Automatic)"
                        com.expensetracker.domain.model.TransactionSource.MANUAL -> "Manual Entry"
                        com.expensetracker.domain.model.TransactionSource.IMPORTED -> "Imported"
                    },
                    icon = Icons.Default.Source
                )
                
                if (transaction.isRecurring) {
                    DetailRow(
                        label = "Recurring",
                        value = "Yes",
                        icon = Icons.Default.Repeat
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatAmount(amount: Double, type: TransactionType): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val formattedAmount = formatter.format(amount)
    
    return when (type) {
        TransactionType.INCOME, TransactionType.TRANSFER_IN -> "+$formattedAmount"
        TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> "-$formattedAmount"
    }
}