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
 * SMS permission setup screen - explains SMS usage and requests permission
 */
@Composable
fun SmsPermissionSetupScreen(
    onGrantPermission: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
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
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = if (smsPermissionGranted) "SMS Permission Granted!" else "SMS Permission Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = if (smsPermissionGranted) {
                    "Great! We can now automatically track your transactions from bank SMS messages."
                } else {
                    "Allow SMS access to automatically track transactions from your bank messages."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        if (smsPermissionGranted) {
            // Permission granted state
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "SMS Permission Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Text(
                            text = "The app will now monitor incoming SMS messages from banks and automatically extract transaction details. You can disable this anytime in settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        } else {
            // Permission request state
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                SmsFeatureExplanation(
                    icon = Icons.Default.AutoAwesome,
                    title = "Automatic Transaction Detection",
                    description = "When you receive SMS messages from banks about transactions, we'll automatically extract the amount, merchant, and date."
                )
                
                SmsFeatureExplanation(
                    icon = Icons.Default.Speed,
                    title = "Save Time",
                    description = "No more manual entry of transactions. Just receive the SMS and see it appear in your expense tracker instantly."
                )
                
                SmsFeatureExplanation(
                    icon = Icons.Default.Security,
                    title = "Secure Processing",
                    description = "SMS messages are processed locally on your device. We never store or transmit your SMS content."
                )
                
                SmsFeatureExplanation(
                    icon = Icons.Default.Settings,
                    title = "Full Control",
                    description = "You can revoke SMS permission anytime and continue using the app with manual transaction entry."
                )
            }
        }
        
        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            if (smsPermissionGranted) {
                Button(
                    onClick = onGrantPermission, // This will proceed to next step
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Grant SMS Permission",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(text = "Skip for Now")
                }
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
private fun SmsFeatureExplanation(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
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

@Preview(showBackground = true)
@Composable
fun SmsPermissionSetupScreenPreview() {
    ExpenseTrackerTheme {
        SmsPermissionSetupScreen(
            onGrantPermission = {},
            onSkip = {},
            onBack = {},
            smsPermissionGranted = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SmsPermissionSetupScreenGrantedPreview() {
    ExpenseTrackerTheme {
        SmsPermissionSetupScreen(
            onGrantPermission = {},
            onSkip = {},
            onBack = {},
            smsPermissionGranted = true
        )
    }
}