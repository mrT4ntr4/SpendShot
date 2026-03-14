package com.spendshot.android.utils

import java.text.DecimalFormat

fun formatNumber(amount: Double): String {
    if (amount == 0.0) {
        return "0"
    }
    val dfAbbr = DecimalFormat("#.##")
    val dfFull = DecimalFormat("₹#,##,##,###.00")

    return when {
        amount >= 10000000 -> "₹${dfAbbr.format(amount / 10000000)} Cr"
        amount >= 100000 -> "₹${dfAbbr.format(amount / 100000)} L"
        else -> dfFull.format(amount)
    }
}
