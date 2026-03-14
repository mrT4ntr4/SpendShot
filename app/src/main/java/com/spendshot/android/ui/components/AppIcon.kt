package com.spendshot.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.spendshot.android.R

@Composable
fun AppIcon(label: String?, modifier: Modifier = Modifier) {
    // Determine which drawable resource to use based on the label
    val iconRes = when (label?.lowercase()) {
        "gpay" -> R.drawable.ic_gpay
        "swiggy" -> R.drawable.ic_swiggy
        "zomato" -> R.drawable.ic_zomato
        "phonepe" -> R.drawable.ic_phonepe
        "paytm" -> R.drawable.ic_paytm
        else -> null // If the label doesn't match, we'll use a fallback
    }

    if (iconRes != null) {
        // Display the icon from your drawable resources
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = modifier,
            tint = androidx.compose.ui.graphics.Color.Unspecified // Important for multi-color icons
        )
    } else {
        // Fallback icon for generic receipts or unknown apps
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = "Receipt",
            modifier = modifier
        )
    }
}
