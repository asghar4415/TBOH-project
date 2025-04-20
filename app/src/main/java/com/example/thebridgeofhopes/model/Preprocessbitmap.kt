import android.graphics.*
import androidx.compose.runtime.Composable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
    val inputSize = 28  // Model input size

    // Step 1: Convert to grayscale
    val grayscaleBitmap = toGrayscale(bitmap)

    // Step 2: Invert colors
    val invertedBitmap = invertColors(grayscaleBitmap)

    // Step 3: Apply Gaussian Blur
    val blurredBitmap = applyGaussianBlur(invertedBitmap)

    // Step 4: Apply Adaptive Thresholding
    val thresholdBitmap = applyAdaptiveThreshold(blurredBitmap)

    // Step 5: Crop to content
    val croppedBitmap = cropToContent(thresholdBitmap)

    // Step 6: Scale and pad to 28x28
    val scaledPaddedBitmap = scaleAndPadTo28x28(croppedBitmap)

    // Step 7: Apply Dilation
    val dilatedBitmap = applyDilation(scaledPaddedBitmap)

    // Step 8: Prepare ByteBuffer for model input
    val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize)
    byteBuffer.order(ByteOrder.nativeOrder())

    // Normalize pixel values (0 to 1)
    for (y in 0 until inputSize) {
        for (x in 0 until inputSize) {
            val pixel = dilatedBitmap.getPixel(x, y)
            val normalizedValue = Color.red(pixel) / 255.0f
            byteBuffer.putFloat(normalizedValue)
        }
    }

    return byteBuffer
}

// Scale and pad image to 28x28 while preserving aspect ratio
fun scaleAndPadTo28x28(bitmap: Bitmap, targetSize: Int = 28, scaledMaxSide: Int = 24): Bitmap {
    // Step 1: Resize keeping aspect ratio
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height
    val maxSide = max(originalWidth, originalHeight)
    val scaleFactor = scaledMaxSide.toFloat() / maxSide.toFloat()
    val newWidth = (originalWidth * scaleFactor).toInt()
    val newHeight = (originalHeight * scaleFactor).toInt()

    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

    // Step 2: Pad to targetSize x targetSize
    val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(paddedBitmap)
    canvas.drawColor(Color.BLACK) // Fill with black

    // Calculate padding offsets
    val left = (targetSize - newWidth) / 2
    val top = (targetSize - newHeight) / 2

    // Draw the scaled bitmap centered
    canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)

    return paddedBitmap
}

// Convert image to grayscale
fun toGrayscale(bitmap: Bitmap): Bitmap {
    val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return grayscaleBitmap
}

// Invert colors
fun invertColors(bitmap: Bitmap): Bitmap {
    val invertedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val r = 255 - Color.red(pixel)
            val g = 255 - Color.green(pixel)
            val b = 255 - Color.blue(pixel)
            invertedBitmap.setPixel(x, y, Color.rgb(r, g, b))
        }
    }
    return invertedBitmap
}

// Apply Gaussian Blur (approximation using RenderScript)
fun applyGaussianBlur(bitmap: Bitmap): Bitmap {
    val blurredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL) // Approximate Gaussian blur
    val canvas = Canvas(blurredBitmap)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return blurredBitmap
}

// Apply Adaptive Thresholding
fun applyAdaptiveThreshold(bitmap: Bitmap): Bitmap {
    val thresholdBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val meanOffset = 15
    for (i in pixels.indices) {
        val r = Color.red(pixels[i])
        pixels[i] = if (r < 128 - meanOffset) Color.BLACK else Color.WHITE
    }

    thresholdBitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return thresholdBitmap
}

// Crop to Content
fun cropToContent(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    var minX = width
    var minY = height
    var maxX = 0
    var maxY = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (bitmap.getPixel(x, y) != Color.BLACK) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    return if (maxX > minX && maxY > minY) {
        Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1)
    } else {
        bitmap
    }
}

// Apply Dilation
fun applyDilation(bitmap: Bitmap): Bitmap {
    val dilatedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val width = bitmap.width
    val height = bitmap.height
    val output = pixels.copyOf()

    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val index = y * width + x
            if (pixels[index] == Color.BLACK) {
                if (pixels[index - 1] == Color.WHITE ||
                    pixels[index + 1] == Color.WHITE ||
                    pixels[index - width] == Color.WHITE ||
                    pixels[index + width] == Color.WHITE
                ) {
                    output[index] = Color.WHITE
                }
            }
        }
    }

    dilatedBitmap.setPixels(output, 0, width, 0, 0, width, height)
    return dilatedBitmap
}