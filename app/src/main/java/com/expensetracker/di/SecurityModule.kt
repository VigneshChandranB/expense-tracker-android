package com.expensetracker.di

import android.content.Context
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.security.DatabaseEncryptionManager
import com.expensetracker.data.security.DataIntegrityValidator
import com.expensetracker.data.security.KeystoreManager
import com.expensetracker.data.security.SecurePreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for security-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager {
        return KeystoreManager()
    }
    
    @Provides
    @Singleton
    fun provideDatabaseEncryptionManager(
        keystoreManager: KeystoreManager
    ): DatabaseEncryptionManager {
        return DatabaseEncryptionManager(keystoreManager)
    }
    
    @Provides
    @Singleton
    fun provideSecurePreferencesManager(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): SecurePreferencesManager {
        return SecurePreferencesManager(context, keystoreManager)
    }
    
    @Provides
    @Singleton
    fun provideDataIntegrityValidator(
        database: ExpenseDatabase
    ): DataIntegrityValidator {
        return DataIntegrityValidator(database)
    }
}