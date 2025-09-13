package com.expensetracker.presentation.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.model.ExportConfig
import com.expensetracker.domain.model.ExportResult
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.usecase.ExportTransactionsUseCase
import com.expensetracker.domain.usecase.ShareExportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for export functionality
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    private val shareExportUseCase: ShareExportUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    fun onEvent(event: ExportEvent) {
        when (event) {
            is ExportEvent.StartExport -> startExport()
            is ExportEvent.FormatChanged -> updateFormat(event.format)
            is ExportEvent.DateRangeChanged -> updateDateRange(event.option)
            is ExportEvent.CustomDateChanged -> updateCustomDate(event.startDate, event.endDate)
            is ExportEvent.IncludeChartsChanged -> updateIncludeCharts(event.include)
            is ExportEvent.AccountSelectionChanged -> updateAccountSelection(event.accountIds)
            is ExportEvent.CategorySelectionChanged -> updateCategorySelection(event.categoryIds)
            is ExportEvent.FileNameChanged -> updateFileName(event.fileName)
            is ExportEvent.ShareFile -> shareFile(event.shareOption)
            is ExportEvent.DismissError -> dismissError()
            is ExportEvent.DismissResult -> dismissResult()
            is ExportEvent.ToggleDatePicker -> toggleDatePicker()
            is ExportEvent.ToggleAccountFilter -> toggleAccountFilter()
            is ExportEvent.ToggleCategoryFilter -> toggleCategoryFilter()
            is ExportEvent.ToggleShareOptions -> toggleShareOptions()
        }
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val accounts = accountRepository.getAllAccounts()
                val categories = categoryRepository.getAllCategories()
                
                _uiState.value = _uiState.value.copy(
                    availableAccounts = accounts,
                    availableCategories = categories
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }
    
    private fun startExport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val config = createExportConfig()
                val result = exportTransactionsUseCase(config)
                
                when (result) {
                    is ExportResult.Success -> {
                        val fileSize = formatFileSize(result.fileSize)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            exportResult = com.expensetracker.presentation.export.ExportResult.Success(
                                result.filePath,
                                fileSize
                            ),
                            showShareOptions = true
                        )
                    }
                    is ExportResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }
    
    private fun shareFile(shareOption: com.expensetracker.domain.model.ShareOption) {
        viewModelScope.launch {
            val result = _uiState.value.exportResult
            if (result is com.expensetracker.presentation.export.ExportResult.Success) {
                val success = shareExportUseCase(result.filePath, shareOption)
                if (!success) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to share file"
                    )
                }
            }
        }
    }
    
    private fun createExportConfig(): ExportConfig {
        val state = _uiState.value
        val dateRange = if (state.customStartDate != null && state.customEndDate != null) {
            DateRange.custom(state.customStartDate, state.customEndDate)
        } else {
            state.selectedDateRange
        }
        
        return ExportConfig(
            format = state.selectedFormat,
            dateRange = dateRange,
            includeCharts = state.includeCharts,
            accountIds = if (state.selectedAccounts.isNotEmpty()) state.selectedAccounts.toList() else null,
            categoryIds = if (state.selectedCategories.isNotEmpty()) state.selectedCategories.toList() else null,
            fileName = if (state.fileName.isNotBlank()) state.fileName else null
        )
    }
    
    private fun updateFormat(format: com.expensetracker.domain.model.ExportFormat) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }
    
    private fun updateDateRange(option: DateRangeOption) {
        val dateRange = when (option) {
            DateRangeOption.CURRENT_MONTH -> DateRange.currentMonth()
            DateRangeOption.LAST_MONTH -> DateRange.lastMonth()
            DateRangeOption.CURRENT_YEAR -> DateRange.currentYear()
            DateRangeOption.LAST_YEAR -> DateRange.lastYear()
            DateRangeOption.CUSTOM -> _uiState.value.selectedDateRange
        }
        
        _uiState.value = _uiState.value.copy(
            selectedDateRange = dateRange,
            showDatePicker = option == DateRangeOption.CUSTOM
        )
    }
    
    private fun updateCustomDate(startDate: java.time.LocalDate?, endDate: java.time.LocalDate?) {
        _uiState.value = _uiState.value.copy(
            customStartDate = startDate,
            customEndDate = endDate
        )
    }
    
    private fun updateIncludeCharts(include: Boolean) {
        _uiState.value = _uiState.value.copy(includeCharts = include)
    }
    
    private fun updateAccountSelection(accountIds: Set<Long>) {
        _uiState.value = _uiState.value.copy(selectedAccounts = accountIds)
    }
    
    private fun updateCategorySelection(categoryIds: Set<Long>) {
        _uiState.value = _uiState.value.copy(selectedCategories = categoryIds)
    }
    
    private fun updateFileName(fileName: String) {
        _uiState.value = _uiState.value.copy(fileName = fileName)
    }
    
    private fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun dismissResult() {
        _uiState.value = _uiState.value.copy(
            exportResult = null,
            showShareOptions = false
        )
    }
    
    private fun toggleDatePicker() {
        _uiState.value = _uiState.value.copy(
            showDatePicker = !_uiState.value.showDatePicker
        )
    }
    
    private fun toggleAccountFilter() {
        _uiState.value = _uiState.value.copy(
            showAccountFilter = !_uiState.value.showAccountFilter
        )
    }
    
    private fun toggleCategoryFilter() {
        _uiState.value = _uiState.value.copy(
            showCategoryFilter = !_uiState.value.showCategoryFilter
        )
    }
    
    private fun toggleShareOptions() {
        _uiState.value = _uiState.value.copy(
            showShareOptions = !_uiState.value.showShareOptions
        )
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}