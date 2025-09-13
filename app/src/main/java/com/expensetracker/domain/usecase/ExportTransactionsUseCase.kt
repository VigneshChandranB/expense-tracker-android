package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.ExportConfig
import com.expensetracker.domain.model.ExportResult
import com.expensetracker.domain.repository.ExportRepository
import javax.inject.Inject

/**
 * Use case for exporting transaction data
 */
class ExportTransactionsUseCase @Inject constructor(
    private val exportRepository: ExportRepository
) {
    
    suspend operator fun invoke(config: ExportConfig): ExportResult {
        return try {
            // Validate configuration
            if (!exportRepository.validateExportConfig(config)) {
                return ExportResult.Error("Invalid export configuration")
            }
            
            // Check estimated file size
            val estimatedSize = exportRepository.getExportSizeEstimate(config)
            if (estimatedSize > MAX_FILE_SIZE_BYTES) {
                return ExportResult.Error("Export file would be too large. Please reduce date range or filter data.")
            }
            
            // Perform export
            exportRepository.exportTransactions(config)
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}", e)
        }
    }
    
    companion object {
        private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
    }
}