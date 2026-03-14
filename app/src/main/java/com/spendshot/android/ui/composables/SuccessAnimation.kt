package com.spendshot.android.ui.composables

import android.icu.text.NumberFormat
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import com.spendshot.android.R
import com.spendshot.android.data.TransactionType
import com.spendshot.android.ui.components.AppIcon
import com.spendshot.android.ui.components.AppIcon
import com.spendshot.android.utils.ParsedReceipt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import coil3.request.crossfade


@Composable
fun SuccessAnimation(
    receipt: ParsedReceipt?,
    onAnimationFinish: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success_animation))
    // Make isPlaying always true and let LaunchedEffect handle the lifecycle
    val progress by animateLottieCompositionAsState(composition, isPlaying = true, restartOnPlay = false)
    var showDetails by remember { mutableStateOf(false) }

    // Sound and Haptic Effect
    DisposableEffect(Unit) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.success_sound)
        mediaPlayer?.start()
        scope.launch {
            repeat(2) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(100)
            }
        }
        onDispose { mediaPlayer?.release() }
    }

    // Listens for the animation to finish, then waits before closing.
    LaunchedEffect(progress) {
        // Start showing details partway through the Lottie animation
        if (progress > 0.2f && !showDetails) {
            showDetails = true
        }
        // When Lottie is done, wait longer before dismissing
        if (progress == 1f) {
            delay(1200L) // Increased delay
            onAnimationFinish()
        }
    }

    Dialog(
        onDismissRequest = { /* Prevent dismissing */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lottie success animation checkmark
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )

            AnimatedVisibility(
                visible = showDetails,
                // Animate the content coming in
                enter = fadeIn(animationSpec = tween(500, delayMillis = 100)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(600, delayMillis = 100)
                        )
            ) {
                if (receipt != null) {
                    // Animate the amount counting up from 0
                    val animatedAmount by animateFloatAsState(
                        targetValue = if (showDetails) receipt.amount.toFloat() else 0f,
                        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
                        label = "AmountCountUp"
                    )

                    // Animate the color of the amount text
                    val animatedColor by animateColorAsState(
                        targetValue = if (receipt.transactionType == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
                        label = "AmountColor"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        receipt.detectedAppLabel?.let { appLabel ->
                            AppIcon(
                                label = appLabel,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        val prefix = when (receipt.transactionType) {
                            TransactionType.INCOME -> "Income"
                            TransactionType.EXPENSE -> "Expense"
                        }

                        Text(
                            text = "$prefix Logged!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        receipt.category?.let { category ->
                            val iconUrl = receipt.categoryIcon
                            if (iconUrl != null) {
                                coil3.compose.SubcomposeAsyncImage(
                                    model = coil3.request.ImageRequest.Builder(LocalContext.current)
                                        .data(iconUrl)
                                        .decoderFactory(coil3.svg.SvgDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = category,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(10.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                                    loading = {
                                        androidx.compose.material3.CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = com.spendshot.android.ui.components.getCategoryIcon(category), // Use helper
                                    contentDescription = category,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(10.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = receipt.merchant,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Use the animated amount and color
                        Text(
                            text = formatCurrency(animatedAmount.toDouble(), receipt.transactionType),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = animatedColor, // Use animated color
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format currency with sign
private fun formatCurrency(amount: Double, type: TransactionType): String {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")) // Using INR
    val formattedAmount = currencyFormat.format(amount)
    return when (type) {
        TransactionType.INCOME -> "+ $formattedAmount"
        TransactionType.EXPENSE -> "- $formattedAmount"
    }
}
