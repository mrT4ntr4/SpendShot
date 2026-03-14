package com.spendshot.android.utils

import android.content.Context
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Utility for loading TFLite models from assets.
 *
 * Models are loaded directly from the assets directory as plain .tflite files
 * and memory-mapped for efficient use with the TFLite Interpreter.
 */
object ModelLoader {

    /**
     * Loads a TFLite model from assets using memory-mapped file I/O.
     *
     * @param context Application context
     * @param assetName Asset file name (e.g., "model_classifier.tflite")
     * @return ByteBuffer containing the model, ready for TFLite Interpreter
     */
    fun loadModel(context: Context, assetName: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(assetName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
