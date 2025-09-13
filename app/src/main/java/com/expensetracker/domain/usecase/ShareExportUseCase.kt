package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.ShareOption
import com.expensetracker.domain.repository.ExportRepository
import javax.inject.Inject

/**
 * Use case for sharing exported files
 */
class ShareExportUseCase @Inject constructor(
    private val exportRepository: ExportRepository
) {
    
    suspend operator fun invoke(filePath: String, shareOption: ShareOption): Boolean {
        return try {
            exportRepository.shareExportedFile(filePath, shareOption)
        } catch (e: Exception) {
            false
        }
    }
}