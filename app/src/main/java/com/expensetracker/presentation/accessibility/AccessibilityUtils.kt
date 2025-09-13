package com.expensetracker.presentation.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/**
 * Accessibility utilities for the Expense Tracker app
 */
object AccessibilityUtils {
    
    /**
     * Formats currency amounts for screen readers
     */
    fun formatCurrencyForAccessibility(amount: BigDecimal): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        return formatter.format(amount)
    }
    
    /**
     * Formats percentage values for screen readers
     */
    fun formatPercentageForAccessibility(percentage: Float): String {
        return "${percentage.toInt()} percent"
    }
    
    /**
     * Creates accessible content description for transaction items
     */
    fun createTransactionContentDescription(
        merchant: String,
        amount: BigDecimal,
        category: String,
        date: String,
        isIncome: Boolean
    ): String {
        val amountText = formatCurrencyForAccessibility(amount)
        val typeText = if (isIncome) "income" else "expense"
        return "$typeText of $amountText from $merchant in $category category on $date"
    }
    
    /**
     * Creates accessible content description for account balance cards
     */
    fun createAccountBalanceContentDescription(
        accountName: String,
        balance: BigDecimal,
        accountType: String
    ): String {
        val balanceText = formatCurrencyForAccessibility(balance)
        return "$accountType account $accountName with balance of $balanceText"
    }
    
    /**
     * Creates accessible content description for category breakdown
     */
    fun createCategoryBreakdownContentDescription(
        categoryName: String,
        amount: BigDecimal,
        percentage: Float
    ): String {
        val amountText = formatCurrencyForAccessibility(amount)
        val percentageText = formatPercentageForAccessibility(percentage)
        return "$categoryName category: $amountText, $percentageText of total spending"
    }
}

/**
 * Modifier for accessible clickable items with proper touch target size
 */
@Composable
fun Modifier.accessibleClickable(
    onClick: () -> Unit,
    contentDescription: String? = null,
    role: Role? = null,
    enabled: Boolean = true
): Modifier {
    return this
        .size(minWidth = 48.dp, minHeight = 48.dp)
        .clip(MaterialTheme.shapes.small)
        .clickable(
            enabled = enabled,
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = true)
        )
        .semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            if (role != null) {
                this.role = role
            }
            if (!enabled) {
                disabled()
            }
        }
}

/**
 * Modifier for accessible selectable items in groups (like radio buttons)
 */
@Composable
fun Modifier.accessibleSelectable(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String? = null,
    enabled: Boolean = true
): Modifier {
    return this
        .size(minWidth = 48.dp, minHeight = 48.dp)
        .clip(MaterialTheme.shapes.small)
        .selectable(
            selected = selected,
            enabled = enabled,
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = true)
        )
        .semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            this.role = Role.RadioButton
            if (!enabled) {
                disabled()
            }
        }
}

/**
 * Modifier for grouping selectable items
 */
fun Modifier.accessibleSelectableGroup(): Modifier {
    return this.selectableGroup()
}

/**
 * Modifier for adding semantic information to composables
 */
fun Modifier.accessibleSemantics(
    contentDescription: String? = null,
    role: Role? = null,
    stateDescription: String? = null,
    heading: Boolean = false,
    mergeDescendants: Boolean = false,
    clearAndSetSemantics: Boolean = false
): Modifier {
    return if (clearAndSetSemantics) {
        this.clearAndSetSemantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            if (role != null) {
                this.role = role
            }
            if (stateDescription != null) {
                this.stateDescription = stateDescription
            }
            if (heading) {
                this.heading()
            }
        }
    } else {
        this.semantics(mergeDescendants = mergeDescendants) {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            if (role != null) {
                this.role = role
            }
            if (stateDescription != null) {
                this.stateDescription = stateDescription
            }
            if (heading) {
                this.heading()
            }
        }
    }
}

/**
 * Modifier for live regions that announce changes to screen readers
 */
fun Modifier.accessibleLiveRegion(
    politeness: LiveRegionMode = LiveRegionMode.Polite
): Modifier {
    return this.semantics {
        liveRegion = politeness
    }
}