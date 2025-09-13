package com.expensetracker.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.presentation.dashboard.AlertSeverity
import com.expensetracker.presentation.dashboard.FinancialAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAlertsCard(
    alerts: List<FinancialAlert>,
    onAlertDismiss: (FinancialAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(alerts) { alert ->
                    AlertItem(
                        alert = alert,
                        onDismiss = { onAlertDismiss(alert) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertItem(
    alert: FinancialAlert,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, backgroundColor) = when (alert.severity) {
        AlertSeverity.INFO -> Triple(
            Icons.Default.Info,
            Color(0xFF2196F3),
            Color(0xFF2196F3).copy(alpha = 0.1f)
        )
        AlertSeverity.WARNING -> Triple(
            Icons.Default.Warning,
            Color(0xFFFF9800),
            Color(0xFFFF9800).copy(alpha = 0.1f)
        )
        AlertSeverity.ERROR -> Triple(
            Icons.Default.Error,
            Color(0xFFF44336),
            Color(0xFFF44336).copy(alpha = 0.1f)
        )
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}