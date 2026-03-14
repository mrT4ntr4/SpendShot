package com.spendshot.android.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.spendshot.android.utils.YoloDetector
import com.spendshot.android.utils.AppClassifier
import com.spendshot.android.utils.ParsedReceipt
import com.spendshot.android.data.TransactionType
import com.spendshot.android.utils.TextRecognitionUtils.findRefinedMerchantName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import androidx.core.graphics.ColorUtils

@Singleton
class ReceiptProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appClassifier: AppClassifier,
    private val yoloDetector: YoloDetector
) {

    private val tag = "ReceiptProcessor"

    suspend fun processReceipt(uri: Uri): ParsedReceipt = withContext(Dispatchers.IO) {
        // Log.i(tag, "=== processReceipt START === uri: $uri")
        val inputStream = context.contentResolver.openInputStream(uri)
        // Log.i(tag, "InputStream opened: ${inputStream != null}")
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("Failed to decode bitmap from URI")

        // 1. App Classification
        val appResult = appClassifier.classify(originalBitmap)
        // Log.i(tag, "Classifier result: ${appResult.label} with score ${appResult.score}")

        // 2. Yolo Detection
        val detections = yoloDetector.detect(originalBitmap)
        if (detections.isEmpty()) {
            return@withContext ParsedReceipt(
                amount = 0.0,
                merchant = "Unknown",
                note = "",
                detectedAppLabel = appResult.label
            )
        }

        // 3. OCR on Detections
        var detectedAmount = "0.00"
        var detectedMerchant = "Unknown"
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        coroutineScope {
            val detectionTasks = detections.map { d ->
                async {
                    // Log.i(tag, "Processing detection - ClassId: ${d.classId}, Box: ${d.box}, Score: ${d.score}")
                    
                    val croppedBitmap = cropBitmap(originalBitmap, d.box)
                    // Log.i(tag, "Cropped bitmap size: ${croppedBitmap.width}x${croppedBitmap.height}")
                    
                    // Ensure minimum size for better OCR accuracy (100x100)
                    // MLKit works much better with larger images
                    val resizedBitmap = ensureMinimumSize(croppedBitmap, 100)
                    if (resizedBitmap !== croppedBitmap) {
                        // Log.i(tag, "Resized bitmap for better OCR: ${resizedBitmap.width}x${resizedBitmap.height}")
                    }
                    
                    val processedBitmap = if (isBitmapDark(resizedBitmap)) {
                        // Log.i(tag, "Bitmap is dark, inverting colors")
                        getInvertedBitmap(resizedBitmap)
                    } else {
                        resizedBitmap
                    }
                    
                    val image = InputImage.fromBitmap(processedBitmap, 0)
                    try {
                        val result = recognizer.process(image).await()
                        val rawText = result.text.trim()
                        // Log.i(tag, "OCR raw text (ClassId: ${d.classId}): '$rawText'")
                        
                        val processedText = if (d.classId == 0) {
                             // Amount
                             val cleaned = rawText.replace(Regex("[^0-9.]"), "")
                             // Log.i(tag, "Amount processed: '$rawText' -> '$cleaned'")
                             cleaned
                        } else {
                             // Merchant
                             val cleaned = rawText.replace("\n", " ")
                             // Log.i(tag, "Merchant processed: '$rawText' -> '$cleaned'")
                             cleaned
                        }
                        processedText to d.classId
                    } catch (e: Exception) {
                        // Log.e(tag, "OCR failed for box (ClassId: ${d.classId}, Size: ${processedBitmap.width}x${processedBitmap.height})", e)
                        "" to -1
                    }
                }
            }
            
            val results = detectionTasks.awaitAll()
            results.forEach { (text, classId) ->
                if (classId == 0) detectedAmount = text
                else if (classId == 1) detectedMerchant = text
            }
            
            // Log.i(tag, "Final OCR results - Amount: '$detectedAmount', Merchant: '$detectedMerchant'")
        }

        // 4. Refinement Logic
        var transactionType = TransactionType.EXPENSE
        val isGPay = appResult.label.equals("GPay", ignoreCase = true)
        
        if (isGPay && detectedMerchant.startsWith("From ", ignoreCase = true)) {
            detectedMerchant = detectedMerchant.removePrefix("From ").trim()
            transactionType = TransactionType.INCOME
        }
        
        if (detectedMerchant.startsWith("Banking name:", ignoreCase = true)) {
             detectedMerchant = detectedMerchant.removePrefix("Banking name:").trim()
        }

        val genericKeywords = setOf("paytm", "phonepemerchant", "verified merchant", "merchant", "unknown", "default")
        val isGenericMerchant = genericKeywords.any { detectedMerchant.contains(it, ignoreCase = true) }

        if (isGPay && isGenericMerchant) {
             // Fallback OCR
             try {
                 val fullImage = InputImage.fromBitmap(originalBitmap, 0)
                 val fullTextResult = recognizer.process(fullImage).await()
                 val refined = findRefinedMerchantName(
                     ocrResult = fullTextResult,
                     primaryMerchantGuess = detectedMerchant,
                     detectedAppName = appResult.label
                 )
                 detectedMerchant = refined
             } catch (e: Exception) {
                 // Log.e(tag, "Full screen OCR failed", e)
             }
        }

        // Swiggy fallback: If app is Swiggy and merchant is still Unknown, default to Instamart
        val isSwiggy = appResult.label.equals("Swiggy", ignoreCase = true)
        if (isSwiggy && (detectedMerchant.equals("Unknown", ignoreCase = true) || detectedMerchant.isBlank())) {
            detectedMerchant = "Instamart"
            // Log.i(tag, "Swiggy detected with unknown merchant, defaulting to Instamart")
        }

        return@withContext ParsedReceipt(
            amount = detectedAmount.toDoubleOrNull() ?: 0.0,
            merchant = detectedMerchant.ifBlank { "Unknown" },
            transactionType = transactionType,
            detectedAppLabel = appResult.label,
            note = ""
        )
    }


    private fun cropBitmap(original: Bitmap, box: android.graphics.RectF): Bitmap {
        val x = max(0, box.left.toInt())
        val y = max(0, box.top.toInt())
        val width = if (x + box.width() > original.width) original.width - x else box.width().toInt()
        val height = if (y + box.height() > original.height) original.height - y else box.height().toInt()
        if (width <= 0 || height <= 0) return original // Fallback
        return Bitmap.createBitmap(original, x, y, width, height)
    }
    
    
    /**
     * Ensures bitmap meets minimum size for better OCR accuracy.
     * Scales up small bitmaps while maintaining aspect ratio using high-quality filtering.
     */
    private fun ensureMinimumSize(bitmap: Bitmap, minSize: Int): Bitmap {
        if (bitmap.width >= minSize && bitmap.height >= minSize) {
            return bitmap // Already meets minimum
        }
        
        // Calculate scale factor to meet minimum while preserving aspect ratio
        val scaleX = if (bitmap.width < minSize) minSize.toFloat() / bitmap.width else 1f
        val scaleY = if (bitmap.height < minSize) minSize.toFloat() / bitmap.height else 1f
        val scale = max(scaleX, scaleY)
        
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        // Use high-quality filtering for better OCR results
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun isBitmapDark(bitmap: Bitmap): Boolean {
        var darkPixels = 0
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val pixelStep = 5 
        for (i in pixels.indices step pixelStep) {
            if (ColorUtils.calculateLuminance(pixels[i]) < 0.5) {
                darkPixels++
            }
        }
        return darkPixels > (pixels.size / pixelStep) / 2
    }
    
    private fun getInvertedBitmap(originalBitmap: Bitmap): Bitmap {
        val invertedBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(invertedBitmap)
        val paint = android.graphics.Paint()
        val matrixInvert = android.graphics.ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrixInvert)
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        return invertedBitmap
    }
}

