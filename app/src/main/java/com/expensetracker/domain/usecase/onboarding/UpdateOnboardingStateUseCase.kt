package com.expensetracker.domain.usecase.onboarding

import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.model.OnboardingStep
import com.expensetracker.domain.repository.OnboardingRepository
import javax.inject.Inject

/**
 * Use case to update onboarding state and progress
 */
class UpdateOnboardingStateUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) {
    suspend fun completeStep(step: OnboardingStep) {
        onboardingRepository.completeStep(step)
    }
    
    suspend fun updateState(state: OnboardingState) {
        onboardingRepository.updateOnboardingState(state)
    }
    
    suspend fun markOnboardingComplete() {
        onboardingRepository.markOnboardingComplete()
    }
}