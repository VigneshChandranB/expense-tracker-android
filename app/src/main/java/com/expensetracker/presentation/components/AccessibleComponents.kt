package com.expensetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.expensetracker.presentation.accessibility.accessibleClickable
import com.expensetracker.presentation.accessibility.accessibleSemantics
import java.math.BigDecimal

/**
 * Accessible text field with proper semantics and error handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    supportingText: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = trailingIcon,
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onAny = { onImeAction?.invoke() }
            ),
            visualTransformation = visualTransformation,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            supportingText = {
                when {
                    isError && errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics {
                                contentDescription = "Error: $errorMessage"
                                liveRegion = LiveRegionMode.Assertive
                            }
                        )
                    }
                    supportingText != null -> {
                        Text(supportingText)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                    if (!enabled) {
                        disabled()
                    }
                }
        )
    }
}

/**
 * Accessible button with proper semantics
 */
@Composable
fun AccessibleButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription ?: text
            role = Role.Button
            if (!enabled) {
                disabled()
            }
            if (loading) {
                stateDescription = "Loading"
            }
        }
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
    }
}

/**
 * Accessible card with proper semantics and click handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            if (onClick != null) {
                role = Role.Button
            }
            if (!enabled) {
                disabled()
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Accessible switch with proper semantics
 */
@Composable
fun AccessibleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .accessibleClickable(
                onClick = { onCheckedChange(!checked) },
                contentDescription = "$label switch, ${if (checked) "enabled" else "disabled"}",
                enabled = enabled
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = "$label switch"
                stateDescription = if (checked) "On" else "Off"
            }
        )
    }
}

/**
 * Accessible radio button group
 */
@Composable
fun <T> AccessibleRadioButtonGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    groupLabel: String? = null
) {
    Column(
        modifier = modifier.semantics {
            if (groupLabel != null) {
                contentDescription = groupLabel
            }
            selectableGroup()
        }
    ) {
        if (groupLabel != null) {
            Text(
                text = groupLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() }
            )
        }
        
        options.forEach { option ->
            val isSelected = option == selectedOption
            val label = optionLabel(option)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleClickable(
                        onClick = { onOptionSelected(option) },
                        contentDescription = "$label, ${if (isSelected) "selected" else "not selected"}",
                        enabled = enabled
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onOptionSelected(option) },
                    enabled = enabled,
                    modifier = Modifier.semantics {
                        contentDescription = label
                        role = Role.RadioButton
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Accessible amount display with proper currency formatting
 */
@Composable
fun AccessibleAmountDisplay(
    amount: BigDecimal,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge
) {
    val formattedAmount = remember(amount) {
        java.text.NumberFormat.getCurrencyInstance().format(amount)
    }
    
    val color = when {
        isIncome -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    
    val contentDescription = remember(amount, isIncome) {
        val typeText = if (isIncome) "income" else "expense"
        "$typeText of $formattedAmount"
    }
    
    Text(
        text = formattedAmount,
        style = style,
        color = color,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        }
    )
}

/**
 * Accessible loading indicator with proper announcements
 */
@Composable
fun AccessibleLoadingIndicator(
    message: String = "Loading",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics {
                contentDescription = message
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Accessible error display with retry option
 */
@Composable
fun AccessibleErrorDisplay(
    error: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics {
                contentDescription = "Error: $error"
                liveRegion = LiveRegionMode.Assertive
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            AccessibleButton(
                onClick = onRetry,
                text = "Retry",
                icon = Icons.Default.Refresh,
                contentDescription = "Retry loading"
            )
        }
    }
}