package com.expensetracker.domain.model

/**
 * Supported export formats for transaction data
 */
enum class ExportFormat {
    CSV,
    PDF
}

/**
 * Date range for export operations
 */
data class DateRange(
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate
) {
    companion object {
        fun currentMonth(): DateRange {
            val now = java.time.LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
            return DateRange(startOfMonth, endOfMonth)
        }
        
        fun lastMonth(): DateRange {
            val now = java.time.LocalDate.now()
            val lastMonth = now.minusMonths(1)
            val startOfMonth = lastMonth.withDayOfMonth(1)
            val endOfMonth = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
            return DateRange(startOfMonth, endOfMonth)
        }
        
        fun currentYear(): DateRange {
            val now = java.time.LocalDate.now()
            val startOfYear = now.withDayOfYear(1)
            val endOfYear = now.withDayOfYear(now.lengthOfYear())
            return DateRange(startOfYear, endOfYear)
        }
        
        fun lastYear(): DateRange {
            val now = java.time.LocalDate.now()
            val lastYear = now.minusYears(1)
            val startOfYear = lastYear.withDayOfYear(1)
            val endOfYear = lastYear.withDayOfYear(lastYear.lengthOfYear())
            return DateRange(startOfYear, endOfYear)
        }
        
        fun custom(startDate: java.time.LocalDate, endDate: java.time.LocalDate): DateRange {
            return DateRange(startDate, endDate)
        }
    }
}

/**
 * Export configuration for transaction data
 */
data class ExportConfig(
    val format: ExportFormat,
    val dateRange: DateRange,
    val includeCharts: Boolean = false,
    val accountIds: List<Long>? = null,
    val categoryIds: List<Long>? = null,
    val fileName: String? = null
)

/**
 * Result of export operation
 */
sealed class ExportResult {
    data class Success(val filePath: String, val fileSize: Long) : ExportResult()
    data class Error(val message: String, val cause: Throwable? = null) : ExportResult()
}

/**
 * Export sharing options
 */
enum class ShareOption {
    EMAIL,
    CLOUD_STORAGE,
    LOCAL_SAVE,
    SHARE_INTENT
}