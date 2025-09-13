package com.expensetracker.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.MonthlyReport
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingSummaryCard(
    monthlyReport: MonthlyReport?,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (monthlyReport != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Income Section
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormatter.format(monthlyReport.totalIncome),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        
                        // Income change indicator
                        if (monthlyReport.comparisonToPreviousMonth.incomeChangePercentage != 0f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (monthlyReport.comparisonToPreviousMonth.incomeChangePercentage > 0) 
                                        Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (monthlyReport.comparisonToPreviousMonth.incomeChangePercentage > 0) 
                                        Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${kotlin.math.abs(monthlyReport.comparisonToPreviousMonth.incomeChangePercentage).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (monthlyReport.comparisonToPreviousMonth.incomeChangePercentage > 0) 
                                        Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }
                    
                    // Expenses Section
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormatter.format(monthlyReport.totalExpenses),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                        
                        // Expense change indicator
                        if (monthlyReport.comparisonToPreviousMonth.expenseChangePercentage != 0f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (monthlyReport.comparisonToPreviousMonth.expenseChangePercentage > 0) 
                                        Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (monthlyReport.comparisonToPreviousMonth.expenseChangePercentage > 0) 
                                        Color(0xFFF44336) else Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${kotlin.math.abs(monthlyReport.comparisonToPreviousMonth.expenseChangePercentage).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (monthlyReport.comparisonToPreviousMonth.expenseChangePercentage > 0) 
                                        Color(0xFFF44336) else Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Net Amount
                val netAmount = monthlyReport.netAmount
                val isPositive = netAmount >= java.math.BigDecimal.ZERO
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPositive) 
                            Color(0xFF4CAF50).copy(alpha = 0.1f) 
                        else 
                            Color(0xFFF44336).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPositive) "Net Savings" else "Net Deficit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currencyFormatter.format(kotlin.math.abs(netAmount.toDouble())),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
                
            } else {
                // Loading or empty state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading spending data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}