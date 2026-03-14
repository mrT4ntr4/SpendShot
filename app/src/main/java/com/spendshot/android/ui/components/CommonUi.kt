package com.spendshot.android.ui.components

import android.icu.text.NumberFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Enum removed
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.data.TransactionType
import com.spendshot.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import coil3.request.crossfade

@Composable
fun MonthYearToggler(
    selectedYear: Int,
    selectedMonth: Int,
    onDateChange: (Int, Int) -> Unit,
    salaryCreditDate: Int,
    modifier: Modifier = Modifier
) {
    val (startCalendar, endCalendar) = remember(selectedYear, selectedMonth, salaryCreditDate) {
        val start = Calendar.getInstance()
        start.set(selectedYear, selectedMonth, salaryCreditDate)
        start.add(Calendar.MONTH, -1)

        val end = Calendar.getInstance()
        end.time = start.time
        end.add(Calendar.MONTH, 1)
        end.add(Calendar.DAY_OF_YEAR, -1)

        Pair(start, end)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            val cal = Calendar.getInstance()
            cal.set(selectedYear, selectedMonth, 1)
            cal.add(Calendar.MONTH, -1)
            onDateChange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
        }

        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dateRange = "${dateFormat.format(startCalendar.time)} - ${dateFormat.format(endCalendar.time)}"
        Text(
            text = dateRange,
            style = MaterialTheme.typography.titleLarge,
        )

        val nextMonthCal = Calendar.getInstance()
        nextMonthCal.set(selectedYear, selectedMonth, 1)
        nextMonthCal.add(Calendar.MONTH, 1)
        val isFutureMonth = nextMonthCal.after(Calendar.getInstance())

        IconButton(
            onClick = {
                val cal = Calendar.getInstance()
                cal.set(selectedYear, selectedMonth, 1)
                cal.add(Calendar.MONTH, 1)
                onDateChange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            },
            enabled = !isFutureMonth
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    categoryIconUrl: String? = null
) {
    // For goal transactions, use sourceApp which contains the goal's icon URL
    val effectiveIconUrl = if (transaction.category.startsWith("Goal:", ignoreCase = true) && transaction.sourceApp != null) {
        transaction.sourceApp
    } else {
        categoryIconUrl
    }
    
    val cardColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    if (effectiveIconUrl != null) {
                         coil3.compose.SubcomposeAsyncImage(
                            model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(effectiveIconUrl)
                                .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                .crossfade(true)
                                .build(),
                            contentDescription = transaction.category,
                            modifier = Modifier.size(28.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(getCategoryColor(category = transaction.category))
                        )
                    } else {
                        Icon(
                            imageVector = getCategoryIcon(category = transaction.category),
                            contentDescription = transaction.category,
                            modifier = Modifier.size(28.dp),
                            tint = getCategoryColor(category = transaction.category)
                        )
                    }
                }

                if (transaction.detectedApp != null && !isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                    ) {
                        AppIcon(
                            label = transaction.detectedApp ?: "Unknown",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            val timeFormat =
                remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) } // Add this
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = timeFormat.format(transaction.timestamp), // Display time
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))


            Text(
                text = formatCurrency(transaction.amount, transaction.type), // Pass type for +/-
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                // Use theme-aware colors for better accessibility and UX
                color = if (transaction.type == TransactionType.INCOME) {
                    MaterialTheme.colorScheme.primary // A good, positive color for income
                } else {
                    MaterialTheme.colorScheme.error // The standard, accessible color for expenses/errors
                }
            )
        }
    }
}

// Update formatCurrency to handle income/expense signs
fun formatCurrency(amount: Double, type: TransactionType): String {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val formattedAmount = currencyFormat.format(amount)
    return if (type == TransactionType.INCOME) "+ $formattedAmount" else "- $formattedAmount"
}

@Composable
fun DonutChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    val proportions = data.mapValues { it.value / total }
    val colors = data.keys.associateWith { getCategoryColor(it) }

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Donut Chart
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f, fill = false) // Center the chart
        ) {
            Canvas(modifier = Modifier.size(120.dp)) { // Reduced size
                var startAngle = -90f
                val strokeWidth = 25.dp.toPx() // Reduced stroke width

                if (total == 0.0) {
                    drawCircle(color = surfaceVariantColor, style = Stroke(width = strokeWidth))
                } else {
                    proportions.forEach { (category, fraction) ->
                        val sweepAngle = (fraction * 360).toFloat()
                        drawArc(
                            color = colors[category] ?: onSurfaceVariantColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) // Smaller font
                Text(
                    String.format("%.0f", total),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp, // Smaller font
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend
        Column(
            modifier = Modifier.weight(1f, fill = false), // Center the legend
            verticalArrangement = Arrangement.Center
        ) {
            // Sort data for consistent legend order
            data.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[category] ?: onSurfaceVariantColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Text with category and amount
                    Text(
                        text = "${category.lowercase().replaceFirstChar { it.uppercase() }} - ${String.format("%.0f", amount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// Add this to your SettingsScreen.kt file

@Composable
fun TitledDivider(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp), // Added horizontal padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge, // A good style for settings headers
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Use primary color to draw attention
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}


// Helpers
@Composable
fun getCategoryColor(category: String): Color {
    return when (category.uppercase()) {
        "FOOD" -> AccentAmber
        "TRAVEL" -> AccentBlue
        "BILLS" -> AccentRed
        "SHOPPING" -> AccentTeal
        "ENTERTAINMENT" -> AccentGreen
        "HEALTH" -> Color(0xFF8E8E93) // iOS-style neutral gray
        "GROCERIES" -> AccentGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.uppercase()) {
        "FOOD" -> Icons.Default.Restaurant
        "TRAVEL" -> Icons.Default.DirectionsCar
        "BILLS" -> Icons.Default.Receipt
        "SHOPPING" -> Icons.Default.ShoppingCart
        "ENTERTAINMENT" -> Icons.Default.Movie
        "HEALTH" -> Icons.Default.MedicalServices
        "GROCERIES" -> Icons.Default.ShoppingBasket
        else -> Icons.Default.Category
    }
}
