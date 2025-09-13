package com.expensetracker.di

import android.content.Context
import com.expensetracker.data.sms.*
import com.expensetracker.domain.permission.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for SMS-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object SmsModule {
    
    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context
    ): PermissionManager {
        return PermissionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSmsReader(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): SmsReader {
        return SmsReader(context, permissionManager)
    }
    
    @Provides
    @Singleton
    fun provideBankPatternRegistry(): BankPatternRegistry {
        return InMemoryBankPatternRegistry()
    }
    
    @Provides
    @Singleton
    fun provideAccountMappingService(): AccountMappingService {
        return InMemoryAccountMappingService()
    }
    
    @Provides
    @Singleton
    fun provideSmsTransactionExtractor(
        bankPatternRegistry: BankPatternRegistry,
        accountMappingService: AccountMappingService
    ): SmsTransactionExtractor {
        return AccountAwareSmsTransactionExtractor(bankPatternRegistry, accountMappingService)
    }
    
    @Provides
    @Singleton
    fun provideSmsProcessor(
        transactionExtractor: SmsTransactionExtractor
    ): SmsProcessor {
        return SmartSmsProcessor(transactionExtractor)
    }
    
    @Provides
    @Singleton
    fun provideSmsServiceManager(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): com.expensetracker.domain.sms.SmsServiceManager {
        return com.expensetracker.domain.sms.SmsServiceManager(context, permissionManager)
    }
    
    @Provides
    @Singleton
    fun provideGracefulDegradationHandler(
        permissionManager: PermissionManager
    ): com.expensetracker.domain.permission.GracefulDegradationHandler {
        return com.expensetracker.domain.permission.GracefulDegradationHandler(permissionManager)
    }
}