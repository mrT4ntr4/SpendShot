package com.spendshot.android.ui.screens

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.icu.util.Calendar
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.copy
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendshot.android.ui.components.getCategoryIcon
import com.spendshot.android.data.SettingsEntity
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.data.TransactionType
import com.spendshot.android.ui.components.MonthYearToggler
import com.spendshot.android.ui.components.SummaryCard
import com.spendshot.android.ui.components.TitledDivider
import com.spendshot.android.utils.formatNumber
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>?, // This is now a pre-filtered list
    settings: SettingsEntity?,
    selectedYear: Int,
    selectedMonth: Int
) {
    if (transactions == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val scrollState = rememberScrollState()
    val monthlySalary = settings?.monthlySalary ?: 0.0

    // --- MODIFICATION: The transaction list is already filtered. We just need to sum the amounts. ---
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    // --- MODIFICATION: Budget-aware daily average calculation ---
    val salaryCreditDate = settings?.salaryCreditDate ?: 1
    
    val daysDivisor = remember(selectedYear, selectedMonth, salaryCreditDate) {
        val today = Calendar.getInstance()
        
        // Construct the Budget Period Start Date based on selectedYear/Month ("Visual" Month)
        // In MainApp, logic is: if current day < salaryDate, visual month matches current calendar month, but actually refers to PREVIOUS period.
        // But here we receive the explicit selectedYear/Month which represents the "Start of the budget bucket" conceptually? 
        // No, in MainApp:
        /*
          set(selectedYear, selectedMonth, 1) -> This is the "Anchor"
          set(DAY, salaryCreditDate)
          if (today < salaryDate) add(MONTH, -1) -> This was determining the INIT (default) period.
        */
        
        // When user CLICKS arrows, they change `selectedYear/Month`.
        // MainApp uses that to build `budgetStartCal`.
        // We should replicate that logic to find the start of the period for THIS `selectedYear/Month`.
        
        val budgetStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, salaryCreditDate)
            
            // Adjust for month wrapping if salaryDate > daysInMonth?
            // Calendar handles this (e.g. Feb 30 -> Mar 2), but budget logic usually caps it. 
            // For now rely on Calendar's lenient behavior or assume valid input.
            
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Special Case: If using "Salary Credit Date" logic where a month "starts" in the previous calendar month?
        // Let's stick to MainApp's convention:
        // `filteredTransactions` in MainApp uses logic:
        /*
           budgetStartCal = Calendar instance set to (selectedYear, selectedMonth, 1)
           set(DAY, salaryCreditDate)
           if (today.day < salaryDate && today.month == selectedMonth) -> Back 1 month.
        */
        // Actually that logic in MainApp was `remember(transactions, selectedYear, selectedMonth, settings)`...
        // Wait, notice MainApp line 661: `if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < ...)`
        // result in an unstable filter if `selectedYear/Month` are just pushed in.
        // Actually, MainApp's filter logic tries to "Automatic adjust" the anchor if we are in the "early part" of a month.
        // But `selectedYear/Month` passed to us ARE the anchors derived from that logic (or user navigation).
        // So we can trust `selectedYear/Month` IS the target month index.
        // EXCEPT: MainApp's filter block RE-RUNS that logic?
        
        // Let's look at MainApp again.
        /*
        val budgetStartCal = Calendar.getInstance().apply {
             time = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }.time
             set(Calendar.DAY_OF_MONTH, settings?.salaryCreditDate ?: 1)
             // The Weird Logic:
             if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < (settings?.salaryCreditDate ?: 1) &&
                 get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                 add(Calendar.MONTH, -1)
             }
        }
        */
        // This "Weird Logic" inside the `remember` block in MainApp means `filteredTransactions` MIGHT be for (Month - 1) 
        // even if `selectedMonth` is (Month), IF we are physically in the early days of (Month).
        // This is confusing. Ideally `selectedYear/Month` should BE the start.
        // However, assuming consistency with `filteredTransactions` calculation:
        
        val robustStart = Calendar.getInstance().apply {
             set(Calendar.YEAR, selectedYear)
             set(Calendar.MONTH, selectedMonth)
             set(Calendar.DAY_OF_MONTH, 1)
             // Replicate MainApp logic exactly for consistency
             set(Calendar.DAY_OF_MONTH, salaryCreditDate)
             
             val now = Calendar.getInstance()
             if (now.get(Calendar.DAY_OF_MONTH) < salaryCreditDate && 
                 get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                 get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                 add(Calendar.MONTH, -1)
             }
             
             set(Calendar.HOUR_OF_DAY, 0)
             set(Calendar.MINUTE, 0)
             set(Calendar.SECOND, 0)
             set(Calendar.MILLISECOND, 0)
        }
        
        val robustEnd = (robustStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1) // End of period
        }
        
        val now = Calendar.getInstance()
        
        when {
            now.before(robustStart) -> 1.0 // Future period?
            now.after(robustEnd) -> {
                // Past Period: Total days in range
                val diffFn = robustEnd.timeInMillis - robustStart.timeInMillis
                (diffFn / (1000 * 60 * 60 * 24)).toDouble().coerceAtLeast(1.0)
            }
            else -> {
                // Current Period: Days elapsed SO FAR
                val diffFn = now.timeInMillis - robustStart.timeInMillis
                val days = (diffFn / (1000 * 60 * 60 * 24)).toDouble()
                (days + 1).coerceAtLeast(1.0) // +1 because day 1 is "1 day elapsed" effectively for avg
            }
        }
    }

    var dailyAvg: Double = 0.0
    if (totalExpense > 0)
        dailyAvg = ((totalExpense / daysDivisor) * 100).toLong() / 100.0

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- REMOVED: MonthYearToggler is no longer needed here ---

        if (monthlySalary > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SalaryProgress(salary = monthlySalary, expense = totalExpense)
                Spacer(modifier = Modifier.height(16.dp))
                SalaryBreakdown(salary = monthlySalary, expense = totalExpense)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Add spacer to give some top margin
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryCard(
                    title = "Salary + Income",
                    amount = formatNumber(monthlySalary + totalIncome),
                    icon = Icons.Default.ArrowUpward,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Expense",
                    amount = formatNumber(totalExpense),
                    icon = Icons.Default.ArrowDownward,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Daily Avg",
                    amount = formatNumber(dailyAvg),
                    icon = Icons.Default.DateRange,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (totalExpense > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TitledDivider("Budget Analysis")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // --- MODIFICATION: Pass the already filtered list directly ---
                BudgetInsightsRow(transactions)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun BudgetInsightsRow(transactions: List<TransactionEntity>) {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    if (expenses.isEmpty()) return

    val maxTxn = expenses.maxByOrNull { it.amount }

    val expensesByCategory = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val maxCategoryTotal = expensesByCategory.values.maxOrNull() ?: 0.0
    val topCategories = expensesByCategory.filter { it.value == maxCategoryTotal }.keys.toList()

    val topCategoryName = if (topCategories.isNotEmpty()) {
        topCategories.joinToString(", ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
    } else {
        "-"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    "Highest Spend",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    maxTxn?.merchant ?: "-",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formatNumber(maxTxn?.amount ?: 0.0),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    "Top Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (topCategories.isNotEmpty()) {
                        Icon(
                            imageVector = getCategoryIcon(topCategories.first()),
                            contentDescription = topCategoryName,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        topCategoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


@Composable
fun SalaryProgress(salary: Double, expense: Double) {
    val percentageUsed = (expense / salary).coerceIn(0.0, 1.0).toFloat()
    
    // Animation for the liquid level rising
    val animatedPercentage by animateFloatAsState(
        targetValue = percentageUsed,
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "WaterLevelAnimation"
    )

    // Animation for the wave motion
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhaseAnimation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height * (1 - animatedPercentage) // Fill from bottom

            val wavePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height) // Start bottom-left
                lineTo(0f, centerY) // Go up to water level

                // Draw sine wave
                val waveAmplitude = 15.dp.toPx() // Height of wave crests
                val waveFrequency = 1.5f // Number of full waves across width

                for (x in 0..width.toInt()) {
                    val angle = (x.toFloat() / width) * (2 * Math.PI.toFloat()) * waveFrequency + wavePhase
                    val y = centerY + waveAmplitude * kotlin.math.sin(angle).toFloat()
                    lineTo(x.toFloat(), y)
                }

                lineTo(width, height) // Go to bottom-right
                close()
            }

            // Draw container circle outline (optional, can be removed if liquid should define shape)
            drawCircle(
                color = surfaceVariantColor.copy(alpha = 0.3f),
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Clip to circle and draw wave
            clipPath(androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, width, height)) }) {
                 // Background of circle (optional)
                 drawRect(surfaceVariantColor.copy(alpha = 0.1f))
                 
                 // Draw the liquid wave
                 drawPath(
                     path = wavePath,
                     color = primaryColor.copy(alpha = 0.8f) // Slight transparency for liquid look
                 )
            }
        }

        // Percentage Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text(
                text = String.format("%.0f%%", animatedPercentage * 100),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Used",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun SalaryBreakdown(salary: Double, expense: Double) {
    val remaining = (salary - expense).coerceAtLeast(0.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                "Total Salary",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatNumber(salary),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "Remaining",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatNumber(remaining),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
