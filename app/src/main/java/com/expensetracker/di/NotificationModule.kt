package com.expensetracker.di

import android.content.Context
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.NotificationDao
import com.expensetracker.data.notification.AndroidNotificationService
import com.expensetracker.data.repository.NotificationRepositoryImpl
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.NotificationRepository
import com.expensetracker.domain.repository.TransactionRepository
import com.expensetracker.domain.service.NotificationService
import com.expensetracker.domain.usecase.analytics.DetectSpendingAnomaliesUseCase
import com.expensetracker.domain.usecase.notification.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dependency injection module for notification-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    
    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context
    ): NotificationService {
        return AndroidNotificationService(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao,
        categoryDao: CategoryDao
    ): NotificationRepository {
        return NotificationRepositoryImpl(notificationDao, categoryDao)
    }
    
    @Provides
    @Singleton
    fun provideSendBillReminderUseCase(
        notificationRepository: NotificationRepository,
        notificationService: NotificationService
    ): SendBillReminderUseCase {
        return SendBillReminderUseCase(notificationRepository, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideSendSpendingLimitAlertUseCase(
        notificationRepository: NotificationRepository,
        notificationService: NotificationService
    ): SendSpendingLimitAlertUseCase {
        return SendSpendingLimitAlertUseCase(notificationRepository, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideSendLowBalanceWarningUseCase(
        notificationRepository: NotificationRepository,
        notificationService: NotificationService
    ): SendLowBalanceWarningUseCase {
        return SendLowBalanceWarningUseCase(notificationRepository, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideSendUnusualSpendingAlertUseCase(
        notificationRepository: NotificationRepository,
        notificationService: NotificationService
    ): SendUnusualSpendingAlertUseCase {
        return SendUnusualSpendingAlertUseCase(notificationRepository, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideNotificationManagerUseCase(
        notificationRepository: NotificationRepository,
        notificationService: NotificationService,
        accountRepository: AccountRepository,
        transactionRepository: TransactionRepository,
        detectSpendingAnomaliesUseCase: DetectSpendingAnomaliesUseCase,
        sendBillReminderUseCase: SendBillReminderUseCase,
        sendSpendingLimitAlertUseCase: SendSpendingLimitAlertUseCase,
        sendLowBalanceWarningUseCase: SendLowBalanceWarningUseCase,
        sendUnusualSpendingAlertUseCase: SendUnusualSpendingAlertUseCase
    ): NotificationManagerUseCase {
        return NotificationManagerUseCase(
            notificationRepository,
            notificationService,
            accountRepository,
            transactionRepository,
            detectSpendingAnomaliesUseCase,
            sendBillReminderUseCase,
            sendSpendingLimitAlertUseCase,
            sendLowBalanceWarningUseCase,
            sendUnusualSpendingAlertUseCase
        )
    }
    
    @Provides
    @Singleton
    @NotificationScope
    fun provideNotificationCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NotificationScope