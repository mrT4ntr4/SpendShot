package com.spendshot.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class YoloDetector(
    context: Context,
    assetPath: String,
    private val labels: Array<String>
) {
    private var interpreter: Interpreter? = null
    private val INPUT_SIZE = 640

    // Config for Letterboxing
    private var scaleFactor: Float = 1.0f
    private var padX: Float = 0f
    private var padY: Float = 0f

    init {
        try {
            val model = ModelLoader.loadModel(context, assetPath)
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)
            // Log.i("YoloDebug", "Model loaded successfully.")
        } catch (e: Exception) {
            // Log.e("YoloDebug", "Error loading model: ${e.message}")
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        if (interpreter == null) return emptyList()

        // 1. LETTERBOX RESIZE
        val letterboxedBitmap = letterboxBitmap(bitmap)
        val inputBuffer = convertBitmapToByteBuffer(letterboxedBitmap)

        // 2. RUN INFERENCE
        val detections = ArrayList<Detection>()
        val outputTensor = interpreter!!.getOutputTensor(0)
        val shape = outputTensor.shape()

        // Handle shape [1, 6, 8400]
        if (shape[1] == 6 && shape[2] == 8400) {
            val output = Array(1) { Array(6) { FloatArray(8400) } }
            interpreter!!.run(inputBuffer, output)
            parseOutputChannelsFirst(output[0], detections)
        }
        else {
            // Log.e("YoloDebug", "Unexpected Shape: ${shape.contentToString()}")
        }

        // 1. Run NMS to merge the duplicates
        val finalDetections = applyNMS(detections)

        // 2. Log only the winners
        for (d in finalDetections) {
            // Log.i("YoloDebug", "Final Detection -> Class: ${labels.getOrElse(d.classId){"?"}} Score: ${d.score} Box: ${d.box}")
        }

        return finalDetections
    }

    private fun letterboxBitmap(original: Bitmap): Bitmap {
        val background = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(background)
        canvas.drawColor(Color.BLACK)

        val originalW = original.width.toFloat()
        val originalH = original.height.toFloat()

        scaleFactor = min(INPUT_SIZE / originalW, INPUT_SIZE / originalH)
        val newW = originalW * scaleFactor
        val newH = originalH * scaleFactor

        padX = (INPUT_SIZE - newW) / 2f
        padY = (INPUT_SIZE - newH) / 2f

        val destRect = RectF(padX, padY, padX + newW, padY + newH)
        canvas.drawBitmap(original, null, destRect, null)

        return background
    }

    private fun parseOutputChannelsFirst(output: Array<FloatArray>, detections: ArrayList<Detection>) {
        val anchors = 8400
        val numClasses = min(labels.size, output.size - 4)

        for (i in 0 until anchors) {
            // 1. Find Score
            var maxScore = 0f
            var classId = -1
            for (c in 0 until numClasses) {
                val score = output[4 + c][i]
                if (score > maxScore) {
                    maxScore = score
                    classId = c
                }
            }

            if (maxScore > 0.25f) { // Threshold
                var cx = output[0][i]
                var cy = output[1][i]
                var w = output[2][i]
                var h = output[3][i]

                // --- FIX: Check if Normalized (0.0 - 1.0) ---
                // If the box is tiny (< 1.0), it's likely normalized. Scale it to 640.
                if (w < 1.0f && h < 1.0f && cx < 1.0f && cy < 1.0f) {
                    cx *= INPUT_SIZE
                    cy *= INPUT_SIZE
                    w *= INPUT_SIZE
                    h *= INPUT_SIZE
                }
                // --------------------------------------------

                // 2. Remove Padding
                val unpadX = cx - padX
                val unpadY = cy - padY

                // 3. Scale back up to Original Image
                val origCx = unpadX / scaleFactor
                val origCy = unpadY / scaleFactor
                val origW = w / scaleFactor
                val origH = h / scaleFactor

                // 4. Convert Center to Top-Left
                val left = origCx - origW / 2
                val top = origCy - origH / 2
                val right = origCx + origW / 2
                val bottom = origCy + origH / 2

                val box = RectF(left, top, right, bottom)
                detections.add(Detection(box, maxScore, classId))

            }
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val input = intValues[pixel++]
                byteBuffer.putFloat(((input shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((input shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((input and 0xFF) / 255.0f)
            }
        }
        return byteBuffer
    }

    private fun applyNMS(detections: ArrayList<Detection>): List<Detection> {
        val nmsList = ArrayList<Detection>()
        detections.sortByDescending { it.score }
        while (detections.isNotEmpty()) {
            val best = detections[0]
            nmsList.add(best)
            detections.removeAt(0)
            val iter = detections.iterator()
            while (iter.hasNext()) {
                val other = iter.next()
                if (calculateIoU(best.box, other.box) > 0.45f) iter.remove()
            }
        }
        return nmsList
    }

    private fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val xA = max(boxA.left, boxB.left)
        val yA = max(boxA.top, boxB.top)
        val xB = min(boxA.right, boxB.right)
        val yB = min(boxA.bottom, boxB.bottom)
        val interArea = max(0f, xB - xA) * max(0f, yB - yA)
        val unionArea = (boxA.width() * boxA.height()) + (boxB.width() * boxB.height()) - interArea
        return interArea / unionArea
    }

    data class Detection(val box: RectF, val score: Float, val classId: Int)
}
