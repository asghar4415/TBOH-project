package com.example.thebridgeofhopes.model


import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AIModel {
    private lateinit var interpreter: Interpreter

    fun loadModel(context: Context) {
        val assetManager = context.assets
        val modelFile = assetManager.open("CNNmodel.tflite").readBytes()
        interpreter = Interpreter(ByteBuffer.allocateDirect(modelFile.size).apply {
            order(ByteOrder.nativeOrder())
            put(modelFile)
        })
    }

    fun evaluateDrawing(bitmap: Bitmap): Int {
        val input = Bitmap.createScaledBitmap(bitmap, 28, 28, true) // Resize for CNN
        val byteBuffer = ByteBuffer.allocateDirect(28 * 28 * 4).apply {
            order(ByteOrder.nativeOrder())
            for (y in 0 until 28) {
                for (x in 0 until 28) {
                    putFloat(input.getPixel(x, y).toFloat())
                }
            }
        }

        val output = Array(1) { FloatArray(1) }
        interpreter.run(byteBuffer, output)
        return (output[0][0] * 100).toInt() // Convert to percentage score
    }
}
