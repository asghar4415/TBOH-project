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
    fun evaluateDrawing(context: Context ,bitmap: Bitmap): List<Pair<Char, Float>> {
        val inputBuffer: ByteBuffer = preprocessBitmap(bitmap)


        val outputArray = Array(1) { FloatArray(26) }

        interpreter.run(inputBuffer, outputArray)

        // return the highest confidence character and its confidence
        return outputArray[0].mapIndexed { index, confidence ->
            Pair(('A'.code + index).toChar(), confidence * 100)
        }

    }


}

