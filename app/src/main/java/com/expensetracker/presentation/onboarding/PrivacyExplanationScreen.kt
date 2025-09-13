package com.expensetracker.presentation.onboarding

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
 * Privacy explanation screen - explains data handling and privacy policies
 */
@Composable
fun PrivacyExplanationScreen(
    onAccept: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Your Privacy Matters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "We take your financial privacy seriously. Here's how we protect your data:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Privacy features
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.weight(1f)
        ) {
            PrivacyFeatureCard(
                icon = Icons.Default.PhoneAndroid,
                title = "Local Data Only",
                description = "All your financial data stays on your device. We never upload your transactions, SMS messages, or personal information to any server.",
                isHighlighted = true
            )
            
            PrivacyFeatureCard(
                icon = Icons.Default.Lock,
                title = "Encrypted Storage",
                description = "Your data is encrypted using Android's secure keystore. Even if someone gains access to your device, your financial data remains protected."
            )
            
            PrivacyFeatureCard(
                icon = Icons.Default.Sms,
                title = "SMS Processing",
                description = "SMS messages are processed locally on your device to extract transaction details. The original messages are never stored or transmitted."
            )
            
            PrivacyFeatureCard(
                icon = Icons.Default.CloudOff,
                title = "No Cloud Sync",
                description = "We don't use cloud services for data storage. Your information never leaves your device unless you explicitly export it."
            )
            
            PrivacyFeatureCard(
                icon = Icons.Default.Delete,
                title = "Complete Control",
                description = "You can delete all your data at any time. Uninstalling the app removes all stored information permanently."
            )
        }
        
        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = "I Understand & Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = "Back")
            }
        }
    }
}

@Composable
private fun PrivacyFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isHighlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isHighlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isHighlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyExplanationScreenPreview() {
    ExpenseTrackerTheme {
        PrivacyExplanationScreen(
            onAccept = {},
            onBack = {}
        )
    }
}