package com.expensetracker.domain.repository

import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.model.OnboardingStep
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for onboarding data management
 */
interface OnboardingRepository {
    fun getOnboardingState(): Flow<OnboardingState>
    suspend fun updateOnboardingState(state: OnboardingState)
    suspend fun completeStep(step: OnboardingStep)
    suspend fun markOnboardingComplete()
    suspend fun isOnboardingCompleted(): Boolean
}