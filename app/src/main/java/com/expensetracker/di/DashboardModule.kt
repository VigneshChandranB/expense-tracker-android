package com.expensetracker.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Dagger Hilt module for Dashboard dependencies
 */
@Module
@InstallIn(ViewModelComponent::class)
object DashboardModule {
    // Dashboard-specific dependencies can be provided here if needed
    // Currently, the DashboardViewModel uses existing repositories and use cases
}