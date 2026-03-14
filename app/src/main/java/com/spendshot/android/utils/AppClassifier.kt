package com.spendshot.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp

class AppClassifier(
    context: Context,
    assetPath: String,
    private val labels: Array<String>
) {
    private var interpreter: Interpreter? = null
    private val INPUT_SIZE = 224

    init {
        try {
            val model = ModelLoader.loadModel(context, assetPath)
            interpreter = Interpreter(model)
            // Log.d("AppClassifier", "Model loaded successfully.")
        } catch (e: Exception) {
            // Log.e("AppClassifier", "Error loading model: ${e.message}")
        }
    }

    fun classify(bitmap: Bitmap): Classification {
        if (interpreter == null) return Classification("Error", 0f)

        // 1. Create the Input Tensor
        // DataType.FLOAT32 is standard for Teachable Machine
        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        val cropSize = Math.min(bitmap.width, bitmap.height)

        val imageProcessor = ImageProcessor.Builder()
            // CORRECTED: Pass cropSize for BOTH height and width
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))

            // Then resize that square down to 224x224
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))

            .add(NormalizeOp(127.5f, 127.5f))
            .build()

        tensorImage = imageProcessor.process(tensorImage)

        // 3. Run Inference
        val outputBuffer = Array(1) { FloatArray(labels.size) }
        interpreter!!.run(tensorImage.buffer, outputBuffer)

        // 4. Find Best Score
        val scores = outputBuffer[0]
        var maxIndex = -1
        var maxScore = 0f

        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxScore = scores[i]
                maxIndex = i
            }
        }

        return if (maxIndex != -1) {
            Classification(labels[maxIndex], maxScore)
        } else {
            Classification("Unknown", 0f)
        }
    }

    data class Classification(val label: String, val score: Float)
}