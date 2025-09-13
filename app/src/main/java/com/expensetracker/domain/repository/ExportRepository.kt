package com.expensetracker.domain.repository

import com.expensetracker.domain.model.ExportConfig
import com.expensetracker.domain.model.ExportResult
import com.expensetracker.domain.model.ShareOption
import java.io.File

/**
 * Repository interface for data export operations
 */
interface ExportRepository {
    
    /**
     * Export transaction data based on configuration
     */
    suspend fun exportTransactions(config: ExportConfig): ExportResult
    
    /**
     * Share exported file using specified option
     */
    suspend fun shareExportedFile(filePath: String, shareOption: ShareOption): Boolean
    
    /**
     * Get available export formats
     */
    fun getSupportedFormats(): List<String>
    
    /**
     * Validate export configuration
     */
    fun validateExportConfig(config: ExportConfig): Boolean
    
    /**
     * Get export file size estimate
     */
    suspend fun getExportSizeEstimate(config: ExportConfig): Long
    
    /**
     * Clean up old export files
     */
    suspend fun cleanupOldExports(): Int
    
    /**
     * Get export directory
     */
    fun getExportDirectory(): File
}