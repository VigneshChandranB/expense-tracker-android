package com.expensetracker.presentation.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Enhanced transaction list screen with filtering, sorting, and search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToTransfer: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSelectingStartDate by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        viewModel.onEvent(TransactionEvent.LoadTransactions)
        viewModel.onEvent(TransactionEvent.LoadAccounts)
        viewModel.onEvent(TransactionEvent.LoadCategories)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with search and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                // Undo/Redo buttons
                IconButton(
                    onClick = { viewModel.onEvent(TransactionEvent.Undo) },
                    enabled = uiState.canUndo
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                
                IconButton(
                    onClick = { viewModel.onEvent(TransactionEvent.Redo) },
                    enabled = uiState.canRedo
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
                
                // Sort button
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                
                // Filter toggle button
                IconButton(onClick = { viewModel.onEvent(TransactionEvent.ToggleFilters) }) {
                    Icon(
                        Icons.Default.FilterList, 
                        contentDescription = "Toggle Filters",
                        tint = if (uiState.showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onEvent(TransactionEvent.UpdateSearchQuery(it)) },
            label = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onEvent(TransactionEvent.UpdateSearchQuery("")) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filters section
        if (uiState.showFilters) {
            FiltersSection(
                uiState = uiState,
                onAccountToggle = { viewModel.onEvent(TransactionEvent.ToggleAccountFilter(it)) },
                onCategoryToggle = { viewModel.onEvent(TransactionEvent.ToggleCategoryFilter(it)) },
                onTypeToggle = { viewModel.onEvent(TransactionEvent.ToggleTypeFilter(it)) },
                onDateRangeClick = { isStart ->
                    isSelectingStartDate = isStart
                    showDatePicker = true
                },
                onClearFilters = { viewModel.onEvent(TransactionEvent.ClearAllFilters) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToAddTransaction,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Transaction")
            }
            
            OutlinedButton(
                onClick = onNavigateToTransfer,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transfer")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Active filters display
        ActiveFiltersRow(
            uiState = uiState,
            onRemoveAccountFilter = { viewModel.onEvent(TransactionEvent.ToggleAccountFilter(it)) },
            onRemoveCategoryFilter = { viewModel.onEvent(TransactionEvent.ToggleCategoryFilter(it)) },
            onRemoveTypeFilter = { viewModel.onEvent(TransactionEvent.ToggleTypeFilter(it)) },
            onRemoveDateFilter = { viewModel.onEvent(TransactionEvent.UpdateDateRange(null, null)) }
        )
        
        // Transaction list
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.transactions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (hasActiveFilters(uiState)) "No transactions match your filters" else "No transactions found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (hasActiveFilters(uiState)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.onEvent(TransactionEvent.ClearAllFilters) }) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.transactions) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            account = uiState.accounts.find { it.id == transaction.accountId },
                            onClick = { onNavigateToEditTransaction(transaction.id) },
                            onDelete = { viewModel.onEvent(TransactionEvent.DeleteTransaction(transaction.id)) }
                        )
                    }
                }
            }
        }
    }
    
    // Sort dialog
    if (showSortDialog) {
        SortDialog(
            currentSortOrder = uiState.sortOrder,
            onSortOrderSelected = { 
                viewModel.onEvent(TransactionEvent.UpdateSortOrder(it))
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                if (isSelectingStartDate) {
                    viewModel.onEvent(TransactionEvent.UpdateDateRange(date, uiState.endDate))
                } else {
                    viewModel.onEvent(TransactionEvent.UpdateDateRange(uiState.startDate, date))
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionItem(
    transaction: Transaction,
    account: Account?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!transaction.description.isNullOrBlank()) {
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.category.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        account?.let {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = it.nickname,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatAmount(transaction.amount.toDouble(), transaction.type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (transaction.type) {
                            TransactionType.INCOME, TransactionType.TRANSFER_IN -> Color.Green
                            TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> Color.Red
                        }
                    )
                    
                    Text(
                        text = transaction.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FiltersSection(
    uiState: TransactionUiState,
    onAccountToggle: (Long) -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onTypeToggle: (TransactionType) -> Unit,
    onDateRangeClick: (Boolean) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Account filters
            if (uiState.accounts.isNotEmpty()) {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.accounts) { account ->
                        FilterChip(
                            onClick = { onAccountToggle(account.id) },
                            label = { Text(account.nickname) },
                            selected = uiState.selectedAccountIds.contains(account.id)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Category filters
            if (uiState.categories.isNotEmpty()) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.categories) { category ->
                        FilterChip(
                            onClick = { onCategoryToggle(category.id) },
                            label = { Text(category.name) },
                            selected = uiState.selectedCategoryIds.contains(category.id)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Transaction type filters
            Text(
                text = "Transaction Types",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TransactionType.values()) { type ->
                    FilterChip(
                        onClick = { onTypeToggle(type) },
                        label = { Text(type.name.replace("_", " ")) },
                        selected = uiState.selectedTypes.contains(type)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Date range filters
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDateRangeClick(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.startDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "Start Date"
                    )
                }
                OutlinedButton(
                    onClick = { onDateRangeClick(false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.endDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "End Date"
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    uiState: TransactionUiState,
    onRemoveAccountFilter: (Long) -> Unit,
    onRemoveCategoryFilter: (Long) -> Unit,
    onRemoveTypeFilter: (TransactionType) -> Unit,
    onRemoveDateFilter: () -> Unit
) {
    val hasFilters = hasActiveFilters(uiState)
    
    if (hasFilters) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // Account filters
            items(uiState.selectedAccountIds) { accountId ->
                val account = uiState.accounts.find { it.id == accountId }
                account?.let {
                    FilterChip(
                        onClick = { onRemoveAccountFilter(accountId) },
                        label = { Text("${it.nickname} ✕") },
                        selected = true
                    )
                }
            }
            
            // Category filters
            items(uiState.selectedCategoryIds) { categoryId ->
                val category = uiState.categories.find { it.id == categoryId }
                category?.let {
                    FilterChip(
                        onClick = { onRemoveCategoryFilter(categoryId) },
                        label = { Text("${it.name} ✕") },
                        selected = true
                    )
                }
            }
            
            // Type filters
            items(uiState.selectedTypes) { type ->
                FilterChip(
                    onClick = { onRemoveTypeFilter(type) },
                    label = { Text("${type.name.replace("_", " ")} ✕") },
                    selected = true
                )
            }
            
            // Date range filter
            if (uiState.startDate != null || uiState.endDate != null) {
                item {
                    FilterChip(
                        onClick = onRemoveDateFilter,
                        label = { Text("Date Range ✕") },
                        selected = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SortDialog(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Transactions") },
        text = {
            Column {
                SortOrder.values().forEach { sortOrder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderSelected(sortOrder) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOrder == sortOrder,
                            onClick = { onSortOrderSelected(sortOrder) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (sortOrder) {
                                SortOrder.DATE_ASC -> "Date (Oldest First)"
                                SortOrder.DATE_DESC -> "Date (Newest First)"
                                SortOrder.AMOUNT_ASC -> "Amount (Low to High)"
                                SortOrder.AMOUNT_DESC -> "Amount (High to Low)"
                                SortOrder.MERCHANT_ASC -> "Merchant (A to Z)"
                                SortOrder.MERCHANT_DESC -> "Merchant (Z to A)"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
                        onDateSelected(localDate)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun hasActiveFilters(uiState: TransactionUiState): Boolean {
    return uiState.searchQuery.isNotEmpty() ||
            uiState.selectedAccountIds.isNotEmpty() ||
            uiState.selectedCategoryIds.isNotEmpty() ||
            uiState.selectedTypes.isNotEmpty() ||
            uiState.startDate != null ||
            uiState.endDate != null
}

private fun formatAmount(amount: Double, type: TransactionType): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val formattedAmount = formatter.format(amount)
    
    return when (type) {
        TransactionType.INCOME, TransactionType.TRANSFER_IN -> "+$formattedAmount"
        TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> "-$formattedAmount"
    }
}