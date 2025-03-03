package com.example.thebridgeofhopes.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object BitmapUtils {
    fun createBitmapFromCanvas(canvas: Canvas): Bitmap {
        val bitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
        val newCanvas = Canvas(bitmap)
        newCanvas.drawColor(Color.WHITE)
        newCanvas.drawBitmap(bitmap, 0f, 0f, Paint())
        return bitmap
    }
}
