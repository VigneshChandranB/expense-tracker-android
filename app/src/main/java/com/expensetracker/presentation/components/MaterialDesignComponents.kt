package com.expensetracker.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.presentation.theme.*
import java.math.BigDecimal

/**
 * Material Design 3 compliant financial amount card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAmountCard(
    title: String,
    amount: BigDecimal,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null
) {
    val containerColor = if (isPositive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    
    val contentColor = if (isPositive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    
    val formattedAmount = remember(amount) {
        java.text.NumberFormat.getCurrencyInstance().format(amount)
    }
    
    val contentDescription = remember(title, amount, isPositive) {
        val typeText = if (isPositive) "positive" else "negative"
        "$title: $typeText amount of $formattedAmount"
    }
    
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
                if (onClick != null) {
                    role = Role.Button
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
                
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Material Design 3 compliant category chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    FilterChip(
        onClick = onClick,
        label = { Text(label) },
        selected = selected,
        enabled = enabled,
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier = modifier.semantics {
            contentDescription = "$label category filter, ${if (selected) "selected" else "not selected"}"
            role = Role.Checkbox
        }
    )
}

/**
 * Material Design 3 compliant progress indicator with label
 */
@Composable
fun LabeledProgressIndicator(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )
    
    val percentageText = remember(progress) {
        "${(progress * 100).toInt()}%"
    }
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "$label: $percentageText complete"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = percentageText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = trackColor
        )
    }
}

/**
 * Material Design 3 compliant info banner
 */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    severity: BannerSeverity = BannerSeverity.INFO
) {
    val containerColor = when (severity) {
        BannerSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer
        BannerSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        BannerSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer
        BannerSeverity.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
    }
    
    val contentColor = when (severity) {
        BannerSeverity.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
        BannerSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        BannerSeverity.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        BannerSeverity.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${severity.name.lowercase()} banner: $message"
                liveRegion = when (severity) {
                    BannerSeverity.ERROR -> LiveRegionMode.Assertive
                    else -> LiveRegionMode.Polite
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                
                TextButton(
                    onClick = onActionClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = actionText
                        role = Role.Button
                    }
                ) {
                    Text(actionText)
                }
            }
            
            if (onDismiss != null) {
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "Dismiss banner"
                        role = Role.Button
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

enum class BannerSeverity {
    INFO, WARNING, ERROR, SUCCESS
}

/**
 * Material Design 3 compliant segmented button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedButton(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.semantics {
            contentDescription = "Segmented button group"
            selectableGroup()
        }
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selectedOption
            val label = optionLabel(option)
            
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { onOptionSelected(option) },
                selected = isSelected,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = "$label, ${if (isSelected) "selected" else "not selected"}"
                    role = Role.RadioButton
                }
            ) {
                Text(label)
            }
        }
    }
}

/**
 * Material Design 3 compliant status chip
 */
@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    AssistChip(
        onClick = { },
        label = { Text(status) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color
        ),
        modifier = modifier.semantics {
            contentDescription = "Status: $status"
        }
    )
}

/**
 * Material Design 3 compliant empty state
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .semantics {
                contentDescription = "Empty state: $title. $description"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onActionClick,
                modifier = Modifier.semantics {
                    contentDescription = actionText
                    role = Role.Button
                }
            ) {
                Text(actionText)
            }
        }
    }
}