package com.expensetracker.presentation.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.presentation.theme.ExpenseTrackerTheme

/**
 * Onboarding completion screen - congratulates user and provides next steps
 */
@Composable
fun OnboardingCompletionScreen(
    onGetStarted: () -> Unit,
    sampleDataCreated: Boolean = false,
    accountsCreated: Int = 0,
    smsPermissionGranted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Success animation/icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "You're All Set!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Your expense tracker is ready to help you manage your finances.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Setup summary
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Setup Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            SetupSummaryItem(
                icon = Icons.Default.AccountBalance,
                title = "Accounts",
                description = if (accountsCreated > 0) {
                    "$accountsCreated account${if (accountsCreated > 1) "s" else ""} created"
                } else if (sampleDataCreated) {
                    "3 sample accounts created"
                } else {
                    "Ready to add your first account"
                },
                isCompleted = accountsCreated > 0 || sampleDataCreated
            )
            
            SetupSummaryItem(
                icon = Icons.Default.Sms,
                title = "SMS Permission",
                description = if (smsPermissionGranted) {
                    "Automatic transaction detection enabled"
                } else {
                    "Manual transaction entry (can enable later)"
                },
                isCompleted = smsPermissionGranted
            )
            
            SetupSummaryItem(
                icon = Icons.Default.DataUsage,
                title = "Sample Data",
                description = if (sampleDataCreated) {
                    "Sample transactions and categories created"
                } else {
                    "Starting with clean slate"
                },
                isCompleted = sampleDataCreated
            )
            
            // Next steps card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "What's Next?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    val nextSteps = buildList {
                        if (accountsCreated == 0 && !sampleDataCreated) {
                            add("• Add your first bank account")
                        }
                        if (!smsPermissionGranted) {
                            add("• Enable SMS permission for automatic tracking")
                        }
                        add("• Start adding transactions manually or via SMS")
                        add("• Explore analytics and spending insights")
                        add("• Customize categories to fit your needs")
                    }
                    
                    Text(
                        text = nextSteps.joinToString("\n"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Get started button
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Go to Dashboard",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Text(
                text = "You can always change these settings later in the app preferences.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SetupSummaryItem(
    icon: ImageVector,
    title: String,
    description: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (!isCompleted) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingCompletionScreenPreview() {
    ExpenseTrackerTheme {
        OnboardingCompletionScreen(
            onGetStarted = {},
            sampleDataCreated = true,
            accountsCreated = 2,
            smsPermissionGranted = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingCompletionScreenMinimalPreview() {
    ExpenseTrackerTheme {
        OnboardingCompletionScreen(
            onGetStarted = {},
            sampleDataCreated = false,
            accountsCreated = 0,
            smsPermissionGranted = false
        )
    }
}