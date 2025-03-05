import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer

object AIModel {
    private lateinit var interpreter: Interpreter

    fun loadModel(context: Context) {
        val modelFile = loadModelFile(context)
        interpreter = Interpreter(modelFile)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("CNNmodel.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun evaluateDrawing(bitmap: Bitmap): Int {
        val inputBuffer: ByteBuffer = preprocessBitmap(bitmap)  // Use ByteBuffer directly
        val outputArray = Array(1) { FloatArray(26) }  // Assuming 10 output classes

        interpreter.run(inputBuffer, outputArray)
        return outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1
    }
}
