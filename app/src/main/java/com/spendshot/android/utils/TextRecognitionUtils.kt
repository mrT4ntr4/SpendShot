package com.spendshot.android.utils

import android.util.Log
import com.google.mlkit.vision.text.Text

object TextRecognitionUtils {

    // A set of generic keywords that trigger the fallback search
    /**
     * Analyzes the OCR text result to find the most accurate merchant name,
     * with special handling for GPay screenshots.
     *
     * @param ocrResult The result from ML Kit's full-screen text recognizer.
     * @param primaryMerchantGuess The merchant name found by your initial object detection.
     * @param detectedAppName The name of the app classified from the screenshot (e.g., "gpay").
     * @return The refined merchant name.
     */
    fun findRefinedMerchantName(
        ocrResult: Text,
        primaryMerchantGuess: String,
        detectedAppName: String?
    ): String {

            // 2. Fallback logic: Search for "Banking name:" in the OCR text.
            val allLines = ocrResult.textBlocks.flatMap { it.lines }

            // 3. Find the line containing "Banking name:".
            val bankingNameLine = allLines.find {
                it.text.contains("Banking", ignoreCase = true) && it.text.contains("name", ignoreCase = true)
            }
            // Log.i("YoloDebug", "BankingNameLine : $bankingNameLine");

            if (bankingNameLine != null) {
                // "Banking name:" was found. The actual name is usually on the same line after the colon.
                val lineText = bankingNameLine.text
                val colonIndex = lineText.indexOf(":")

                if (colonIndex != -1 && colonIndex < lineText.length - 1) {
                    val refinedName = lineText.substring(colonIndex + 1).trim()
                    if (refinedName.isNotBlank()) {
                        return refinedName // SUCCESS: Return the refined name.
                    }
                }
            }

        // 4. If conditions aren't met or fallback fails, return the original guess from the model.
        return primaryMerchantGuess
    }
}
