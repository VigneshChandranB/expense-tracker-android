package com.expensetracker.di

import com.expensetracker.data.categorization.KeywordCategorizerImpl
import com.expensetracker.data.categorization.MerchantCategorizerImpl
import com.expensetracker.data.categorization.SmartTransactionCategorizer
import com.expensetracker.data.repository.CategoryRepositoryImpl
import com.expensetracker.data.repository.CategorizationRepositoryImpl
import com.expensetracker.domain.categorization.KeywordCategorizer
import com.expensetracker.domain.categorization.MerchantCategorizer
import com.expensetracker.domain.categorization.TransactionCategorizer
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.CategorizationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for categorization components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CategorizationModule {
    
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository
    
    @Binds
    @Singleton
    abstract fun bindCategorizationRepository(
        categorizationRepositoryImpl: CategorizationRepositoryImpl
    ): CategorizationRepository
    
    @Binds
    @Singleton
    abstract fun bindKeywordCategorizer(
        keywordCategorizerImpl: KeywordCategorizerImpl
    ): KeywordCategorizer
    
    @Binds
    @Singleton
    abstract fun bindMerchantCategorizer(
        merchantCategorizerImpl: MerchantCategorizerImpl
    ): MerchantCategorizer
    
    @Binds
    @Singleton
    abstract fun bindTransactionCategorizer(
        smartTransactionCategorizer: SmartTransactionCategorizer
    ): TransactionCategorizer
}