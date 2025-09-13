package com.expensetracker.data.repository

import android.content.Context
import com.expensetracker.data.export.CsvExporter
import com.expensetracker.data.export.FileShareService
import com.expensetracker.data.export.PdfExporter
import com.expensetracker.domain.model.ExportConfig
import com.expensetracker.domain.model.ExportFormat
import com.expensetracker.domain.model.ExportResult
import com.expensetracker.domain.model.ShareOption
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.ExportRepository
import com.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Implementation of ExportRepository for handling data export operations
 */
class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val csvExporter: CsvExporter,
    private val pdfExporter: PdfExporter,
    private val fileShareService: FileShareService
) : ExportRepository {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    override suspend fun exportTransactions(config: ExportConfig): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Get transactions for the specified date range
            val transactions = if (config.accountIds != null) {
                transactionRepository.searchTransactions(
                    accountIds = config.accountIds,
                    categoryIds = config.categoryIds,
                    startDate = config.dateRange.startDate,
                    endDate = config.dateRange.endDate
                )
            } else {
                transactionRepository.getTransactionsByDateRange(
                    config.dateRange.startDate,
                    config.dateRange.endDate
                )
            }
            
            if (transactions.isEmpty()) {
                return@withContext ExportResult.Error("No transactions found for the specified criteria")
            }
            
            // Get account information
            val accounts = accountRepository.getAllAccounts().associateBy { it.id }
            
            // Generate filename
            val fileName = config.fileName ?: generateFileName(config)
            val outputFile = File(getExportDirectory(), fileName)
            
            // Ensure export directory exists
            outputFile.parentFile?.mkdirs()
            
            // Perform export based on format
            val success = when (config.format) {
                ExportFormat.CSV -> csvExporter.exportTransactions(transactions, accounts, outputFile)
                ExportFormat.PDF -> pdfExporter.exportTransactions(
                    transactions, 
                    accounts, 
                    config.dateRange, 
                    config.includeCharts, 
                    outputFile
                )
            }
            
            if (success) {
                ExportResult.Success(outputFile.absolutePath, outputFile.length())
            } else {
                ExportResult.Error("Failed to generate export file")
            }
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}", e)
        }
    }
    
    override suspend fun shareExportedFile(filePath: String, shareOption: ShareOption): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return false
            }
            fileShareService.shareFile(file, shareOption)
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getSupportedFormats(): List<String> {
        return ExportFormat.values().map { it.name }
    }
    
    override fun validateExportConfig(config: ExportConfig): Boolean {
        return try {
            // Check date range validity
            if (config.dateRange.startDate.isAfter(config.dateRange.endDate)) {
                return false
            }
            
            // Check if date range is not too far in the future
            val now = java.time.LocalDate.now()
            if (config.dateRange.startDate.isAfter(now.plusDays(1))) {
                return false
            }
            
            // Check if date range is reasonable (not more than 5 years)
            val maxRange = now.minusYears(5)
            if (config.dateRange.startDate.isBefore(maxRange)) {
                return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getExportSizeEstimate(config: ExportConfig): Long = withContext(Dispatchers.IO) {
        try {
            val transactions = transactionRepository.getTransactionsByDateRange(
                config.dateRange.startDate,
                config.dateRange.endDate
            )
            
            // Rough estimate: CSV ~200 bytes per transaction, PDF ~300 bytes per transaction
            val bytesPerTransaction = when (config.format) {
                ExportFormat.CSV -> 200L
                ExportFormat.PDF -> if (config.includeCharts) 400L else 300L
            }
            
            transactions.size * bytesPerTransaction
        } catch (e: Exception) {
            0L
        }
    }
    
    override suspend fun cleanupOldExports(): Int = withContext(Dispatchers.IO) {
        try {
            val exportDir = getExportDirectory()
            if (!exportDir.exists()) return@withContext 0
            
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days ago
            var deletedCount = 0
            
            exportDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            deletedCount
        } catch (e: Exception) {
            0
        }
    }
    
    override fun getExportDirectory(): File {
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }
    
    private fun generateFileName(config: ExportConfig): String {
        val dateRange = "${config.dateRange.startDate.format(dateFormatter)}_to_${config.dateRange.endDate.format(dateFormatter)}"
        val extension = when (config.format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.PDF -> "pdf"
        }
        return "expense_tracker_${dateRange}.${extension}"
    }
}