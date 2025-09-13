package com.expensetracker.presentation.export

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.model.ExportFormat
import com.expensetracker.domain.model.ShareOption

/**
 * UI state for export screen
 */
data class ExportUiState(
    val isLoading: Boolean = false,
    val selectedFormat: ExportFormat = ExportFormat.CSV,
    val selectedDateRange: DateRange = DateRange.currentMonth(),
    val customStartDate: java.time.LocalDate? = null,
    val customEndDate: java.time.LocalDate? = null,
    val includeCharts: Boolean = false,
    val selectedAccounts: Set<Long> = emptySet(),
    val selectedCategories: Set<Long> = emptySet(),
    val availableAccounts: List<Account> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val fileName: String = "",
    val estimatedFileSize: String = "",
    val exportResult: ExportResult? = null,
    val error: String? = null,
    val showDatePicker: Boolean = false,
    val showAccountFilter: Boolean = false,
    val showCategoryFilter: Boolean = false,
    val showShareOptions: Boolean = false
)

/**
 * Export result for UI
 */
sealed class ExportResult {
    data class Success(val filePath: String, val fileSize: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

/**
 * Date range options for UI
 */
enum class DateRangeOption {
    CURRENT_MONTH,
    LAST_MONTH,
    CURRENT_YEAR,
    LAST_YEAR,
    CUSTOM
}

/**
 * Export events from UI
 */
sealed class ExportEvent {
    object StartExport : ExportEvent()
    data class FormatChanged(val format: ExportFormat) : ExportEvent()
    data class DateRangeChanged(val option: DateRangeOption) : ExportEvent()
    data class CustomDateChanged(val startDate: java.time.LocalDate?, val endDate: java.time.LocalDate?) : ExportEvent()
    data class IncludeChartsChanged(val include: Boolean) : ExportEvent()
    data class AccountSelectionChanged(val accountIds: Set<Long>) : ExportEvent()
    data class CategorySelectionChanged(val categoryIds: Set<Long>) : ExportEvent()
    data class FileNameChanged(val fileName: String) : ExportEvent()
    data class ShareFile(val shareOption: ShareOption) : ExportEvent()
    object DismissError : ExportEvent()
    object DismissResult : ExportEvent()
    object ToggleDatePicker : ExportEvent()
    object ToggleAccountFilter : ExportEvent()
    object ToggleCategoryFilter : ExportEvent()
    object ToggleShareOptions : ExportEvent()
}