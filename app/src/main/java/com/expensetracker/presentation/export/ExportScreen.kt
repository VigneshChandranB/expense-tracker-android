package com.expensetracker.presentation.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.ExportFormat
import com.expensetracker.domain.model.ShareOption
import java.time.format.DateTimeFormatter

/**
 * Export screen for exporting transaction data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Handle export result
    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { result ->
            when (result) {
                is ExportResult.Success -> {
                    // Show success message or automatically open share options
                }
                is ExportResult.Error -> {
                    // Error is handled in UI state
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Export Data",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Format Selection
            item {
                ExportFormatSection(
                    selectedFormat = uiState.selectedFormat,
                    onFormatChanged = { viewModel.onEvent(ExportEvent.FormatChanged(it)) }
                )
            }
            
            // Date Range Selection
            item {
                DateRangeSection(
                    selectedDateRange = uiState.selectedDateRange,
                    customStartDate = uiState.customStartDate,
                    customEndDate = uiState.customEndDate,
                    showDatePicker = uiState.showDatePicker,
                    onDateRangeChanged = { viewModel.onEvent(ExportEvent.DateRangeChanged(it)) },
                    onCustomDateChanged = { start, end -> 
                        viewModel.onEvent(ExportEvent.CustomDateChanged(start, end)) 
                    },
                    onToggleDatePicker = { viewModel.onEvent(ExportEvent.ToggleDatePicker) }
                )
            }
            
            // PDF Options
            if (uiState.selectedFormat == ExportFormat.PDF) {
                item {
                    PdfOptionsSection(
                        includeCharts = uiState.includeCharts,
                        onIncludeChartsChanged = { 
                            viewModel.onEvent(ExportEvent.IncludeChartsChanged(it)) 
                        }
                    )
                }
            }
            
            // Account Filter
            item {
                AccountFilterSection(
                    availableAccounts = uiState.availableAccounts,
                    selectedAccounts = uiState.selectedAccounts,
                    showAccountFilter = uiState.showAccountFilter,
                    onAccountSelectionChanged = { 
                        viewModel.onEvent(ExportEvent.AccountSelectionChanged(it)) 
                    },
                    onToggleAccountFilter = { viewModel.onEvent(ExportEvent.ToggleAccountFilter) }
                )
            }
            
            // Category Filter
            item {
                CategoryFilterSection(
                    availableCategories = uiState.availableCategories,
                    selectedCategories = uiState.selectedCategories,
                    showCategoryFilter = uiState.showCategoryFilter,
                    onCategorySelectionChanged = { 
                        viewModel.onEvent(ExportEvent.CategorySelectionChanged(it)) 
                    },
                    onToggleCategoryFilter = { viewModel.onEvent(ExportEvent.ToggleCategoryFilter) }
                )
            }
            
            // File Name
            item {
                FileNameSection(
                    fileName = uiState.fileName,
                    onFileNameChanged = { viewModel.onEvent(ExportEvent.FileNameChanged(it)) }
                )
            }
            
            // Export Button
            item {
                Button(
                    onClick = { viewModel.onEvent(ExportEvent.StartExport) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (uiState.isLoading) "Exporting..." else "Export Data")
                }
            }
        }
    }
    
    // Error Dialog
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ExportEvent.DismissError) },
            title = { Text("Export Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ExportEvent.DismissError) }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Success Dialog with Share Options
    if (uiState.showShareOptions && uiState.exportResult is ExportResult.Success) {
        ShareOptionsDialog(
            result = uiState.exportResult,
            onShareOption = { viewModel.onEvent(ExportEvent.ShareFile(it)) },
            onDismiss = { viewModel.onEvent(ExportEvent.DismissResult) }
        )
    }
}

@Composable
private fun ExportFormatSection(
    selectedFormat: ExportFormat,
    onFormatChanged: (ExportFormat) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(modifier = Modifier.selectableGroup()) {
                ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedFormat == format,
                                onClick = { onFormatChanged(format) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = format.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (format) {
                                    ExportFormat.CSV -> "Spreadsheet format, compatible with Excel"
                                    ExportFormat.PDF -> "Document format with charts and tables"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateRangeSection(
    selectedDateRange: com.expensetracker.domain.model.DateRange,
    customStartDate: java.time.LocalDate?,
    customEndDate: java.time.LocalDate?,
    showDatePicker: Boolean,
    onDateRangeChanged: (DateRangeOption) -> Unit,
    onCustomDateChanged: (java.time.LocalDate?, java.time.LocalDate?) -> Unit,
    onToggleDatePicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            
            Text(
                text = "Current: ${selectedDateRange.startDate.format(dateFormatter)} - ${selectedDateRange.endDate.format(dateFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onDateRangeChanged(DateRangeOption.CURRENT_MONTH) },
                    label = { Text("This Month") },
                    selected = false
                )
                FilterChip(
                    onClick = { onDateRangeChanged(DateRangeOption.LAST_MONTH) },
                    label = { Text("Last Month") },
                    selected = false
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onDateRangeChanged(DateRangeOption.CURRENT_YEAR) },
                    label = { Text("This Year") },
                    selected = false
                )
                FilterChip(
                    onClick = { onDateRangeChanged(DateRangeOption.LAST_YEAR) },
                    label = { Text("Last Year") },
                    selected = false
                )
                FilterChip(
                    onClick = { onToggleDatePicker() },
                    label = { Text("Custom") },
                    selected = showDatePicker
                )
            }
        }
    }
}

@Composable
private fun PdfOptionsSection(
    includeCharts: Boolean,
    onIncludeChartsChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "PDF Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includeCharts,
                    onCheckedChange = onIncludeChartsChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Include Charts",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Add category breakdown and summary charts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountFilterSection(
    availableAccounts: List<com.expensetracker.domain.model.Account>,
    selectedAccounts: Set<Long>,
    showAccountFilter: Boolean,
    onAccountSelectionChanged: (Set<Long>) -> Unit,
    onToggleAccountFilter: () -> Unit
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
                    text = "Account Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onToggleAccountFilter) {
                    Text(if (showAccountFilter) "Hide" else "Show")
                }
            }
            
            if (selectedAccounts.isNotEmpty()) {
                Text(
                    text = "${selectedAccounts.size} accounts selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "All accounts included",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showAccountFilter) {
                Spacer(modifier = Modifier.height(8.dp))
                
                availableAccounts.forEach { account ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedAccounts.contains(account.id),
                            onCheckedChange = { checked ->
                                val newSelection = if (checked) {
                                    selectedAccounts + account.id
                                } else {
                                    selectedAccounts - account.id
                                }
                                onAccountSelectionChanged(newSelection)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = account.nickname,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${account.bankName} • ${account.accountType.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterSection(
    availableCategories: List<com.expensetracker.domain.model.Category>,
    selectedCategories: Set<Long>,
    showCategoryFilter: Boolean,
    onCategorySelectionChanged: (Set<Long>) -> Unit,
    onToggleCategoryFilter: () -> Unit
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
                    text = "Category Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onToggleCategoryFilter) {
                    Text(if (showCategoryFilter) "Hide" else "Show")
                }
            }
            
            if (selectedCategories.isNotEmpty()) {
                Text(
                    text = "${selectedCategories.size} categories selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "All categories included",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showCategoryFilter) {
                Spacer(modifier = Modifier.height(8.dp))
                
                availableCategories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedCategories.contains(category.id),
                            onCheckedChange = { checked ->
                                val newSelection = if (checked) {
                                    selectedCategories + category.id
                                } else {
                                    selectedCategories - category.id
                                }
                                onCategorySelectionChanged(newSelection)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileNameSection(
    fileName: String,
    onFileNameChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "File Name (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = fileName,
                onValueChange = onFileNameChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Auto-generated if empty") },
                singleLine = true
            )
        }
    }
}

@Composable
private fun ShareOptionsDialog(
    result: ExportResult.Success,
    onShareOption: (ShareOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Successful") },
        text = {
            Column {
                Text("File exported successfully!")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Size: ${result.fileSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column {
                TextButton(
                    onClick = {
                        onShareOption(ShareOption.EMAIL)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email")
                }
                
                TextButton(
                    onClick = {
                        onShareOption(ShareOption.CLOUD_STORAGE)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cloud Storage")
                }
                
                TextButton(
                    onClick = {
                        onShareOption(ShareOption.SHARE_INTENT)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}