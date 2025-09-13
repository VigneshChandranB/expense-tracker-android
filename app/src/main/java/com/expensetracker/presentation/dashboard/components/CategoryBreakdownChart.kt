package com.expensetracker.presentation.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.CategoryBreakdown
import com.expensetracker.domain.model.CategorySpending
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBreakdownChart(
    categoryBreakdown: CategoryBreakdown?,
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
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (categoryBreakdown != null && categoryBreakdown.categorySpending.isNotEmpty()) {
                // Pie Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(
                        data = categoryBreakdown.categorySpending.take(6), // Show top 6 categories
                        modifier = Modifier.size(160.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category Legend
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(categoryBreakdown.categorySpending.take(6)) { categorySpending ->
                        CategoryLegendItem(
                            categorySpending = categorySpending,
                            color = getCategoryColor(categorySpending.category.name),
                            currencyFormatter = currencyFormatter
                        )
                    }
                }
                
                // Show "Others" if there are more than 6 categories
                if (categoryBreakdown.categorySpending.size > 6) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val othersAmount = categoryBreakdown.categorySpending
                        .drop(6)
                        .sumOf { it.amount }
                    
                    Text(
                        text = "Others: ${currencyFormatter.format(othersAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
            } else {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No spending data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start adding transactions to see breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.amount }
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.8f
        
        var startAngle = -90f
        
        data.forEach { categorySpending ->
            val sweepAngle = (categorySpending.amount.toFloat() / total.toFloat()) * 360f
            val color = getCategoryColor(categorySpending.category.name)
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategoryLegendItem(
    categorySpending: CategorySpending,
    color: Color,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = categorySpending.category.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = currencyFormatter.format(categorySpending.amount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${categorySpending.percentage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getCategoryColor(categoryName: String): Color {
    return when (categoryName.lowercase()) {
        "food & dining", "food" -> Color(0xFFFF9800)
        "shopping" -> Color(0xFF2196F3)
        "transportation" -> Color(0xFF4CAF50)
        "bills & utilities", "bills" -> Color(0xFFF44336)
        "entertainment" -> Color(0xFF9C27B0)
        "healthcare" -> Color(0xFFE91E63)
        "investment" -> Color(0xFF00BCD4)
        "income" -> Color(0xFF8BC34A)
        else -> Color(0xFF607D8B)
    }
}