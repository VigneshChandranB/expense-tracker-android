package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.security.DatabaseEncryptionManager
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.dao.AccountDao
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.CategoryRuleDao
import com.expensetracker.data.local.dao.MerchantInfoDao
import com.expensetracker.data.local.dao.KeywordMappingDao
import com.expensetracker.data.local.dao.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideExpenseDatabase(
        @ApplicationContext context: Context,
        databaseEncryptionManager: DatabaseEncryptionManager
    ): ExpenseDatabase {
        return databaseEncryptionManager.createEncryptedDatabase(context)
    }
    
    @Provides
    fun provideTransactionDao(database: ExpenseDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideAccountDao(database: ExpenseDatabase): AccountDao {
        return database.accountDao()
    }
    
    @Provides
    fun provideCategoryDao(database: ExpenseDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    fun provideCategoryRuleDao(database: ExpenseDatabase): CategoryRuleDao {
        return database.categoryRuleDao()
    }
    
    @Provides
    fun provideMerchantInfoDao(database: ExpenseDatabase): MerchantInfoDao {
        return database.merchantInfoDao()
    }
    
    @Provides
    fun provideKeywordMappingDao(database: ExpenseDatabase): KeywordMappingDao {
        return database.keywordMappingDao()
    }
    
    @Provides
    fun provideNotificationDao(database: ExpenseDatabase): NotificationDao {
        return database.notificationDao()
    }
}