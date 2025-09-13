package com.expensetracker.di

import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.repository.SettingsRepositoryImpl
import com.expensetracker.domain.repository.SettingsRepository
import com.expensetracker.domain.usecase.settings.GetAppSettingsUseCase
import com.expensetracker.domain.usecase.settings.UpdateAppSettingsUseCase
import com.expensetracker.domain.usecase.settings.ManageDataUseCase
import com.expensetracker.domain.usecase.settings.BackupRestoreUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for settings-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        database: ExpenseDatabase
    ): SettingsRepository {
        return SettingsRepositoryImpl(
            settingsDao = database.settingsDao(),
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            notificationDao = database.notificationDao()
        )
    }
    
    @Provides
    fun provideGetAppSettingsUseCase(
        settingsRepository: SettingsRepository
    ): GetAppSettingsUseCase {
        return GetAppSettingsUseCase(settingsRepository)
    }
    
    @Provides
    fun provideUpdateAppSettingsUseCase(
        settingsRepository: SettingsRepository
    ): UpdateAppSettingsUseCase {
        return UpdateAppSettingsUseCase(settingsRepository)
    }
    
    @Provides
    fun provideManageDataUseCase(
        settingsRepository: SettingsRepository
    ): ManageDataUseCase {
        return ManageDataUseCase(settingsRepository)
    }
    
    @Provides
    fun provideBackupRestoreUseCase(
        settingsRepository: SettingsRepository
    ): BackupRestoreUseCase {
        return BackupRestoreUseCase(settingsRepository)
    }
}