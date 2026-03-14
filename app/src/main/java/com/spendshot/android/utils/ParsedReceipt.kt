package com.spendshot.android.utils

// Enum removed

import com.spendshot.android.data.TransactionType
import java.util.Date

data class ParsedReceipt(
    val amount: Double,
    val merchant: String,
    val note: String = "",
    val detectedAppLabel: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val timestamp: Date = Date(),
    val category: String? = null, // Updated to String
    val categoryIcon: String? = null,
    val originalOcrMerchant: String? = null, // For OCR correction learning
    val manualEntryReason: String? = null // Reason for manual entry fallback (e.g., zero amount, unknown merchant)
)

