package com.expensetracker.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry

/**
 * Material Design 3 compliant navigation transitions
 */
object NavigationTransitions {
    
    private const val TRANSITION_DURATION = 300
    private const val FADE_DURATION = 150
    
    /**
     * Standard enter transition for screens
     */
    fun enterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_DURATION,
                delayMillis = FADE_DURATION,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Standard exit transition for screens
     */
    fun exitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FADE_DURATION,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Pop enter transition (when navigating back)
     */
    fun popEnterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_DURATION,
                delayMillis = FADE_DURATION,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Pop exit transition (when navigating back)
     */
    fun popExitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FADE_DURATION,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Fade transition for modal screens
     */
    fun fadeEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Fade exit transition for modal screens
     */
    fun fadeExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Vertical slide transition for bottom sheets or dialogs
     */
    fun slideUpEnterTransition(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_DURATION,
                delayMillis = FADE_DURATION,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Vertical slide exit transition for bottom sheets or dialogs
     */
    fun slideDownExitTransition(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FADE_DURATION,
                easing = LinearEasing
            )
        )
    }
    
    /**
     * Shared element transition for detailed views
     */
    fun sharedElementEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 4 },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    /**
     * Shared element exit transition for detailed views
     */
    fun sharedElementExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight / 4 },
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }
}

/**
 * Extension function to get appropriate transitions based on navigation direction
 */
@Composable
fun getTransitionsForRoute(
    route: String,
    previousRoute: String?
): Pair<EnterTransition, ExitTransition> {
    return when {
        // Modal routes (settings, forms, etc.)
        route.contains("settings") || route.contains("form") || route.contains("add") -> {
            NavigationTransitions.fadeEnterTransition() to NavigationTransitions.fadeExitTransition()
        }
        
        // Detail routes (transaction details, account details)
        route.contains("detail") -> {
            NavigationTransitions.sharedElementEnterTransition() to NavigationTransitions.sharedElementExitTransition()
        }
        
        // Bottom sheet routes
        route.contains("sheet") -> {
            NavigationTransitions.slideUpEnterTransition() to NavigationTransitions.slideDownExitTransition()
        }
        
        // Standard navigation
        else -> {
            NavigationTransitions.enterTransition() to NavigationTransitions.exitTransition()
        }
    }
}

/**
 * Extension function to get pop transitions based on navigation direction
 */
@Composable
fun getPopTransitionsForRoute(
    route: String,
    targetRoute: String?
): Pair<EnterTransition, ExitTransition> {
    return when {
        // Modal routes
        route.contains("settings") || route.contains("form") || route.contains("add") -> {
            NavigationTransitions.fadeEnterTransition() to NavigationTransitions.fadeExitTransition()
        }
        
        // Detail routes
        route.contains("detail") -> {
            NavigationTransitions.sharedElementEnterTransition() to NavigationTransitions.sharedElementExitTransition()
        }
        
        // Bottom sheet routes
        route.contains("sheet") -> {
            NavigationTransitions.slideUpEnterTransition() to NavigationTransitions.slideDownExitTransition()
        }
        
        // Standard back navigation
        else -> {
            NavigationTransitions.popEnterTransition() to NavigationTransitions.popExitTransition()
        }
    }
}