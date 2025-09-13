package com.expensetracker.di

import com.expensetracker.data.export.CsvExporter
import com.expensetracker.data.export.FileShareService
import com.expensetracker.data.export.PdfExporter
import com.expensetracker.data.repository.ExportRepositoryImpl
import com.expensetracker.domain.repository.ExportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for export functionality dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {
    
    @Binds
    @Singleton
    abstract fun bindExportRepository(
        exportRepositoryImpl: ExportRepositoryImpl
    ): ExportRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ExportProvidersModule {
    // CsvExporter, PdfExporter, and FileShareService are provided via @Inject constructor
}