package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.MonthlyReport
import com.expensetracker.domain.repository.AnalyticsRepository
import java.time.YearMonth
import javax.inject.Inject

/**
 * Use case for generating comprehensive monthly financial reports
 * Implements requirement 5.1: Monthly spending summary and income vs expenses comparison
 */
class GenerateMonthlyReportUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    
    /**
     * Generate a detailed monthly report for the specified month
     * 
     * @param month The month to generate the report for
     * @return MonthlyReport containing comprehensive financial analysis
     */
    suspend operator fun invoke(month: YearMonth): Result<MonthlyReport> {
        return try {
            val report = analyticsRepository.generateMonthlyReport(month)
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate monthly report for the current month
     */
    suspend fun generateCurrentMonthReport(): Result<MonthlyReport> {
        return invoke(YearMonth.now())
    }
    
    /**
     * Generate monthly report for the previous month
     */
    suspend fun generatePreviousMonthReport(): Result<MonthlyReport> {
        return invoke(YearMonth.now().minusMonths(1))
    }
}