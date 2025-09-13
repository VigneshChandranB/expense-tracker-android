package com.expensetracker.domain.usecase.onboarding

import com.expensetracker.domain.model.OnboardingState
import com.expensetracker.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get the current onboarding state
 */
class GetOnboardingStateUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) {
    operator fun invoke(): Flow<OnboardingState> {
        return onboardingRepository.getOnboardingState()
    }
}