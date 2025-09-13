package com.expensetracker.di

import android.content.Context
import com.expensetracker.data.local.optimization.DatabaseOptimizer
import com.expensetracker.data.performance.MemoryManager
import com.expensetracker.data.performance.PerformanceMonitor
import com.expensetracker.data.sms.OptimizedSmsProcessor
import com.expensetracker.data.sms.SmsProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Dependency injection module for performance optimization components
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    
    @Provides
    @Singleton
    fun provideMemoryManager(
        @ApplicationContext context: Context
    ): MemoryManager {
        return MemoryManager(context)
    }
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(
        @ApplicationContext context: Context,
        memoryManager: MemoryManager
    ): PerformanceMonitor {
        return PerformanceMonitor(context, memoryManager)
    }
    
    @Provides
    @Singleton
    fun provideDatabaseOptimizer(
        database: com.expensetracker.data.local.database.ExpenseDatabase
    ): DatabaseOptimizer {
        return DatabaseOptimizer(database)
    }
    
    @Provides
    @Singleton
    fun provideOptimizedSmsProcessor(
        smsProcessor: SmsProcessor
    ): OptimizedSmsProcessor {
        return OptimizedSmsProcessor(smsProcessor, Dispatchers.Default)
    }
}