package com.spendshot.android.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendshot.android.data.SettingsEntity
import com.spendshot.android.data.TransactionEntity
import com.spendshot.android.data.TransactionType
import com.spendshot.android.viewmodels.BudgetViewModel
import com.spendshot.android.viewmodels.BudgetUiState
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import com.spendshot.android.data.CategoryEntity // Ensure this import exists or use fully qualified if preferred, but simpler to import.
import java.util.AbstractMap
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.sp
import coil3.request.crossfade
// Actually, let's just use fully qualified in signature to be safe as per previous code, or just import it.
// The file imports are around lines 28-32. I'll just change the type signature for now.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportsScreen(
    transactions: List<TransactionEntity>?,
    settings: SettingsEntity?,
    pagerState: PagerState,
    budgetViewModel: BudgetViewModel,

    onCategoryClick: (com.spendshot.android.data.CategoryEntity) -> Unit
) {
    if (transactions == null) return
    val budgetStates by budgetViewModel.budgets.collectAsState()
    if (budgetStates.any { it.spentAmount > 0 }) {
        var selectedCategory by remember { mutableStateOf<String?>(null) }
        
        val activeBudgets = remember(budgetStates) {
            budgetStates.filter { it.spentAmount > 0 }
        }
        
        val chartData = remember(activeBudgets) {
            activeBudgets.associate { it.category.name to it.spentAmount }
        }
        
        val categoryColors = remember(activeBudgets) {
            activeBudgets.associate { it.category.name to it.color }
        }
        
        val categoryIcons = remember(activeBudgets) {
            activeBudgets.associate { it.category.name to it.category.icon }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            TreemapChart(
                data = chartData,
                colors = categoryColors,
                icons = categoryIcons,
                onCategoryClick = { selectedCategory = it }
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header for Category List
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // Display Category Items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                budgetStates.forEach { state ->
                    BudgetListItem(state = state, onClick = { onCategoryClick(state.category) })
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
             // Even if no expenses, we might want to show the budget list if budgets exist?
             // But the user requested "Show All Categories". If we show all categories, budgetStates is never empty.
             // We should check if ALL spentAmount are 0.
             if (budgetStates.isNotEmpty()) {
                 Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                     // Show just the list if no expenses to chart? Or show empty chart?
                     // Let's show the list primarily.
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        budgetStates.forEach { state ->
                            BudgetListItem(state = state, onClick = { onCategoryClick(state.category) })
                        }
                    }
                }
             } else {
                Text(
                    "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
             }
        }
    }
}

@Composable
fun TreemapChart(
    data: Map<String, Double>,
    colors: Map<String, Color>,
    icons: Map<String, String?>, // Pass icons
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    // 1. Sort data by value descending
    val sortedEntries = data.entries.sortedByDescending { it.value }

    // 2. Limit to top 5 categories, group rest into "Others"
    val topEntries = sortedEntries.take(5)
    val otherEntries = sortedEntries.drop(5)
    
    val finalData = if (otherEntries.isNotEmpty()) {
        topEntries + AbstractMap.SimpleEntry("Others", otherEntries.sumOf { it.value })
    } else {
        topEntries
    }

    // 3. Normalize values for weights
    val totalCheck = finalData.sumOf { it.value }
    val items = finalData.map { (cat, value) ->
        TreemapItemData(
            category = cat,
            value = value,
            weight = (value / totalCheck).toFloat(),
            color = if (cat == "Others") MaterialTheme.colorScheme.surfaceVariant else (colors[cat] ?: MaterialTheme.colorScheme.primary),
            iconUrl = if (cat == "Others") null else icons[cat]
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp) // Fixed height for the chart
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
         TreemapNode(
            items = items,
            isHorizontal = true, // Start splitting horizontally (left vs right)
            onItemClick = onCategoryClick
        )
    }
}

data class TreemapItemData(
    val category: String,
    val value: Double,
    val weight: Float,
    val color: Color,
    val iconUrl: String? = null
)

@Composable
fun TreemapNode(
    items: List<TreemapItemData>,
    isHorizontal: Boolean,
    onItemClick: (String) -> Unit
) {
    if (items.isEmpty()) return

    if (items.size == 1) {
        // Leaf Node
        val item = items[0]
        TreemapLeaf(item = item, onClick = { onItemClick(item.category) })
        return
    }

    // Recursive Split
    // Take the heaviest item (first one) vs the rest
    val first = items[0]
    val rest = items.drop(1)
    
    val firstWeight = first.weight
    val restWeight = rest.sumOf { it.weight.toDouble() }.toFloat()
    
    // Animate entry
    val animatedWeight by animateFloatAsState(
        targetValue = firstWeight / (firstWeight + restWeight),
         animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "SplitAnimation"
    )

    if (isHorizontal) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(animatedWeight).fillMaxHeight()) {
                TreemapLeaf(item = first, onClick = { onItemClick(first.category) })
            }
            Box(modifier = Modifier.weight(1f - animatedWeight).fillMaxHeight()) {
                TreemapNode(items = rest, isHorizontal = !isHorizontal, onItemClick = onItemClick)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
             Box(modifier = Modifier.weight(animatedWeight).fillMaxWidth()) {
                TreemapLeaf(item = first, onClick = { onItemClick(first.category) })
            }
            Box(modifier = Modifier.weight(1f - animatedWeight).fillMaxWidth()) {
                TreemapNode(items = rest, isHorizontal = !isHorizontal, onItemClick = onItemClick)
            }
        }
    }
}

@Composable
fun TreemapLeaf(
    item: TreemapItemData,
    onClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance() }
    
    // Strip "Goal: " prefix for display
    val displayName = remember(item.category) {
        item.category.removePrefix("Goal: ")
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp) // Gap between blocks
            .clip(RoundedCornerShape(8.dp))
            .background(item.color)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val width = maxWidth.value
            val height = maxHeight.value
            val minDimension = minOf(width, height)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    // Very small tiles (< 50dp): Just show color, no content
                    minDimension < 50 -> {
                        // Empty - just the colored background
                    }
                    
                    // Small tiles (50-80dp): Icon only
                    minDimension < 80 -> {
                        if (item.category != "Others") {
                            if (item.iconUrl != null) {
                                coil3.compose.SubcomposeAsyncImage(
                                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(item.iconUrl)
                                        .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.9f))
                                )
                            } else {
                                Icon(
                                    imageVector = com.spendshot.android.ui.components.getCategoryIcon(displayName),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    // Medium tiles (80-120dp): Icon + category name
                    minDimension < 120 -> {
                        if (item.category != "Others") {
                            if (item.iconUrl != null) {
                                coil3.compose.SubcomposeAsyncImage(
                                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(item.iconUrl)
                                        .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.9f))
                                )
                            } else {
                                Icon(
                                    imageVector = com.spendshot.android.ui.components.getCategoryIcon(displayName),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = if (item.category == "Others") "Others" else displayName.take(6),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    // Large tiles (120+dp): Icon + category name + amount
                    else -> {
                        if (item.category != "Others") {
                            if (item.iconUrl != null) {
                                coil3.compose.SubcomposeAsyncImage(
                                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(item.iconUrl)
                                        .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.9f))
                                )
                            } else {
                                Icon(
                                    imageVector = com.spendshot.android.ui.components.getCategoryIcon(displayName),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = if (item.category == "Others") "Others" else displayName.take(10),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currencyFormat.format(item.value),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun BudgetListItem(state: BudgetUiState, onClick: () -> Unit) {
    val progressColor = if (state.isExceeded) Color.Red else state.color
    val currencyFormat = remember { NumberFormat.getCurrencyInstance() }
    
    // Animate background color when touched/selected (optional, keeping simple for now)
    
    // Calculate percentage of limit used, or just 0 if no limit
    val percentage = if (state.limitAmount > 0) (state.spentAmount / state.limitAmount * 100) else 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(state.color.copy(alpha = 0.2f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (state.category.icon != null) {
                 coil3.compose.SubcomposeAsyncImage(
                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(state.category.icon)
                        .decoderFactory(coil3.svg.SvgDecoder.Factory())
                        .crossfade(true)
                        .build(),
                    contentDescription = state.category.name,
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(state.color)
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = com.spendshot.android.ui.components.getCategoryIcon(state.category.name),
                    contentDescription = state.category.name,
                    modifier = Modifier.size(24.dp),
                    tint = state.color
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.category.name.removePrefix("Goal: ").replace("_", " "),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (state.hasBudget) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${currencyFormat.format(state.spentAmount)} spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currencyFormat.format(state.limitAmount)} limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                 Text(
                    text = "${currencyFormat.format(state.spentAmount)} spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        if (!state.hasBudget) {
            // Action to add budget (This would ideally trigger the dialog)
            // For now, we'll just show the spent amount prominently or a generic indicator
            // Since triggering dialog requires a callback not yet piped through, 
            // a simple UI cleanup is the priority.
             Text(
                text = "No Limit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        } else {
             Text(
                text = String.format(Locale.getDefault(), "%.0f%%", percentage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isExceeded) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}