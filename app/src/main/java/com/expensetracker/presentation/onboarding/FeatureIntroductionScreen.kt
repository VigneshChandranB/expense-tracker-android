package com.expensetracker.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.TutorialStep
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

/**
 * Feature introduction screen with tutorial steps
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeatureIntroductionScreen(
    tutorialSteps: List<TutorialStep>,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { tutorialSteps.size })
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "App Features",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Learn about the key features that will help you manage your finances",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Tutorial pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page < tutorialSteps.size) {
                TutorialStepCard(
                    step = tutorialSteps[page],
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        
        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            repeat(tutorialSteps.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                )
            }
        }
        
        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back/Previous button
            if (pagerState.currentPage == 0) {
                OutlinedButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Back")
                }
            } else {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Previous")
                }
            }
            
            // Skip button
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Skip")
            }
            
            // Next/Complete button
            Button(
                onClick = {
                    if (pagerState.currentPage == tutorialSteps.size - 1) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    if (pagerState.currentPage == tutorialSteps.size - 1) "Continue" else "Next"
                )
            }
        }
    }
}

@Composable
private fun TutorialStepCard(
    step: TutorialStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Feature icon
            Icon(
                imageVector = getTutorialStepIcon(step.iconResource),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Feature title
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Feature description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )
            
            // Feature highlights based on step
            when (step.id) {
                "dashboard" -> DashboardHighlights()
                "accounts" -> AccountsHighlights()
                "sms_parsing" -> SmsParsingHighlights()
                "categorization" -> CategorizationHighlights()
                "analytics" -> AnalyticsHighlights()
                "export" -> ExportHighlights()
            }
        }
    }
}

@Composable
private fun DashboardHighlights() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureHighlight(
            icon = Icons.Default.AccountBalance,
            text = "View all account balances at a glance"
        )
        FeatureHighlight(
            icon = Icons.Default.TrendingUp,
            text = "Track monthly income vs expenses"
        )
        FeatureHighlight(
            icon = Icons.Default.PieChart,
            text = "See spending breakdown by category"
        )
    }
}

@Composable
private fun AccountsHighlights() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureHighlight(
            icon = Icons.Default.Add,
            text = "Add multiple bank accounts and credit cards"
        )
        FeatureHighlight(
            icon = Icons.Default.SwapHoriz,
            text = "Track transfers between accounts"
        )
        FeatureHighlight(
            icon = Icons.Default.Visibility,
            text = "Switch between account views easily"
        )
    }
}

@Composable
private fun SmsParsingHighlights() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureHighlight(
            icon = Icons.Default.AutoAwesome,
            text = "Automatic transaction detection from SMS"
        )
        FeatureHighlight(
            icon = Icons.Default.Security,
            text = "All processing happens locally on your device"
        )
        FeatureHighlight(
            icon = Icons.Default.Speed,
            text = "No manual entry required for bank transactions"
        )
    }
}

@Composable
private fun CategorizationHighlights() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureHighlight(
            icon = Icons.Default.Category,
            text = "Smart automatic categorization"
        )
        FeatureHighlight(
            icon = Icons.Default.School,
            text = "System learns from your corrections"
        )
        FeatureHighlight(
            icon = Icons.Default.Edit,
            text = "Create custom categories for your needs"
        )
    }
}

@Composable
private fun AnalyticsHighlights() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureHighlight(
            icon = Icons.Default.BarChart,
            text = "Visual charts and spending trends"
        )
        FeatureHighlight(
            icon = Icons.Default.Compare,
            text = "Month-over-month comparisons"
        )
        FeatureHighlight(
            icon = Icons.Default.Warning,
            text = "Spending alerts and anomaly detection"
        )
    }
}

@Composable
private fun ExportHighlights() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeatureHighlight(
            icon = Icons.Default.FileDownload,
            text = "Export to CSV and PDF formats"
        )
        FeatureHighlight(
            icon = Icons.Default.Share,
            text = "Share via email or cloud storage"
        )
        FeatureHighlight(
            icon = Icons.Default.DateRange,
            text = "Custom date ranges for exports"
        )
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getTutorialStepIcon(iconResource: String): ImageVector {
    return when (iconResource) {
        "dashboard" -> Icons.Default.Dashboard
        "account_balance" -> Icons.Default.AccountBalance
        "sms" -> Icons.Default.Sms
        "category" -> Icons.Default.Category
        "analytics" -> Icons.Default.Analytics
        "file_download" -> Icons.Default.FileDownload
        else -> Icons.Default.Info
    }
}

@Preview(showBackground = true)
@Composable
fun FeatureIntroductionScreenPreview() {
    val sampleSteps = listOf(
        TutorialStep(
            id = "dashboard",
            title = "Dashboard Overview",
            description = "View your account balances, recent transactions, and spending insights all in one place.",
            targetFeature = "dashboard",
            iconResource = "dashboard"
        ),
        TutorialStep(
            id = "accounts",
            title = "Multi-Account Management",
            description = "Manage multiple bank accounts and credit cards. Switch between accounts and track transfers.",
            targetFeature = "accounts",
            iconResource = "account_balance"
        )
    )
    
    ExpenseTrackerTheme {
        FeatureIntroductionScreen(
            tutorialSteps = sampleSteps,
            onComplete = {},
            onSkip = {},
            onBack = {}
        )
    }
}