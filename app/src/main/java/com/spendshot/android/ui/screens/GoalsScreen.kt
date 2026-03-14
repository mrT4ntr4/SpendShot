package com.spendshot.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import coil3.request.crossfade
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.spendshot.android.data.GoalEntity
import com.spendshot.android.utils.toTitleCase
import com.spendshot.android.viewmodels.GoalsViewModel
import com.spendshot.android.ui.components.AddFundsDialog
import com.spendshot.android.ui.components.EditGoalDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    goalsViewModel: GoalsViewModel,
    selectedGoalIds: Set<Int> = emptySet(),
    onSelectionChange: (Set<Int>) -> Unit = {}
) {
    GoalsContent(goalsViewModel, selectedGoalIds, onSelectionChange)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GoalsContent(
    viewModel: GoalsViewModel,
    selectedGoalIds: Set<Int> = emptySet(),
    onSelectionChange: (Set<Int>) -> Unit = {}
) {
    val goals by viewModel.goals.collectAsState()
    val monthlySavings by viewModel.monthlySavings.collectAsState()
    var goalToAddFunds by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var savingsHistory by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    
    val scope = rememberCoroutineScope()
    val isInSelectionMode = selectedGoalIds.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No goals set yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to start saving",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    val isSelected = selectedGoalIds.contains(goal.id)
                    GoalItem(
                        goal = goal,
                        savedThisMonth = monthlySavings[goal.id] ?: 0.0,
                        isSelected = isSelected,
                        onAddFundsClick = { 
                            if (!isInSelectionMode) goalToAddFunds = goal 
                        },
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    if (isInSelectionMode) {
                                        onSelectionChange(
                                            if (isSelected) selectedGoalIds - goal.id
                                            else selectedGoalIds + goal.id
                                        )
                                    } else {
                                        scope.launch {
                                            savingsHistory = viewModel.getGoalSavingsHistory(goal.name)
                                            goalToEdit = goal
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isInSelectionMode) {
                                        onSelectionChange(selectedGoalIds + goal.id)
                                    }
                                }
                            )
                    )
                }
            }
        }
    }

    if (goalToAddFunds != null) {
        val goal = goalToAddFunds!!
        val remainingAmount = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
        AddFundsDialog(
            goalName = goal.name,
            maxAmount = remainingAmount,
            onDismiss = { goalToAddFunds = null },
            onSave = { amount ->
                viewModel.addSavings(goal.id, amount)
                goalToAddFunds = null
            }
        )
    }

    if (goalToEdit != null) {
        val goal = goalToEdit!!
        EditGoalDialog(
            goal = goal,
            savingsHistory = savingsHistory,
            onDismiss = { goalToEdit = null },
            onSave = { updatedGoal ->
                viewModel.updateGoal(updatedGoal)
                goalToEdit = null
            }
        )
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GoalItem(
    goal: GoalEntity,
    savedThisMonth: Double = 0.0,
    isSelected: Boolean = false,
    onAddFundsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()
    val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
    val isCompleted = progress >= 1f
    
    // Animated scale for selection feedback
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        ),
        label = "scale"
    )
    
    // Colors
    val accentColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val cardBackground = MaterialTheme.colorScheme.surface
    val selectedBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) selectedBackground else cardBackground,
        border = if (isSelected) BorderStroke(2.dp, accentColor) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Top Row: Icon + Name + Circular Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon + Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Modern Icon Container
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = accentColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Text(
                                    text = "🎉",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            } else if (goal.icon != null) {
                                coil3.compose.SubcomposeAsyncImage(
                                    model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(goal.icon)
                                        .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accentColor),
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = accentColor
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = goal.name.toTitleCase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currencyFormat.format(goal.targetAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Circular Progress with percentage
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(56.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 5.dp,
                            color = accentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                modifier = Modifier.size(24.dp),
                                tint = accentColor
                            )
                        } else {
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Row: Saved amount + Add Funds button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currencyFormat.format(goal.savedAmount),
                                style = MaterialTheme.typography.titleLarge,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " saved",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCompleted) {
                            Text(
                                text = "🎊 Goal achieved!",
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor
                            )
                        } else if (savedThisMonth > 0) {
                            Text(
                                text = "+${currencyFormat.format(savedThisMonth)} this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    // Add Funds button (hide for completed goals)
                    if (!isCompleted) {
                        FilledTonalButton(
                            onClick = onAddFundsClick,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = accentColor.copy(alpha = 0.15f),
                                contentColor = accentColor
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Funds", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
