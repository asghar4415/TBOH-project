import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
    val inputSize = 28  // Model input size

    // Resize the image to match the model input size
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

    // Convert the image to grayscale
    val grayscaleBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                0.33f, 0.33f, 0.33f, 0f, 0f,
                0.33f, 0.33f, 0.33f, 0f, 0f,
                0.33f, 0.33f, 0.33f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    canvas.drawBitmap(resizedBitmap, 0f, 0f, paint)

    // Prepare a ByteBuffer for the model input (FLOAT32 format)
    val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize) // 4 bytes per float
    byteBuffer.order(ByteOrder.nativeOrder())

    // Normalize pixels to [-1,1] if required by the model, otherwise [0,1]
    for (y in 0 until inputSize) {
        for (x in 0 until inputSize) {
            val pixel = grayscaleBitmap.getPixel(x, y)
            val grayValue = (pixel shr 16 and 0xFF) / 255.0f  // Extract red channel (grayscale)
            byteBuffer.putFloat(grayValue)
        }
    }

    return byteBuffer
}
