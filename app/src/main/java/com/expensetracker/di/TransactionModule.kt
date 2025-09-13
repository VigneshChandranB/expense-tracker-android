package com.expensetracker.di

import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.usecase.ManageTransactionsUseCase
import com.expensetracker.domain.usecase.TransactionUndoRedoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for transaction-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object TransactionModule {
    
    @Provides
    @Singleton
    fun provideManageTransactionsUseCase(
        transactionRepository: TransactionRepository,
        accountRepository: AccountRepository
    ): ManageTransactionsUseCase {
        return ManageTransactionsUseCase(transactionRepository, accountRepository)
    }
    
    @Provides
    @Singleton
    fun provideTransactionUndoRedoUseCase(
        transactionRepository: TransactionRepository
    ): TransactionUndoRedoUseCase {
        return TransactionUndoRedoUseCase(transactionRepository)
    }
}