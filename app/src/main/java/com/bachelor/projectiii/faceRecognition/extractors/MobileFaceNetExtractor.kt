package com.bachelor.projectiii.faceRecognition.extractors

import android.content.Context
import android.graphics.Bitmap
import com.bachelor.projectiii.interfaces.faceProcessing.IFaceExtractor
import com.bachelor.projectiii.utils.FaceRecognitionUtils
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Feature extractor using MobileFaceNet TensorFlow Lite model to produce 128-d face embeddings.
 */
class MobileFaceNetExtractor(
    context: Context,
    modelName: String = "mobile_facenet.tflite"
) : IFaceExtractor {

    companion object {
        private const val FACE_INPUT_SIZE = 112
        private const val EMBEDDING_DIM = 128
        private const val NORM_MEAN = 127.5f
        private const val NORM_STD = 128.0f
    }

    private val isClosed = AtomicBoolean(false)
    private val interpreter: Interpreter
    private val reusableInputBuffer: ByteBuffer
    private val pixelCache: IntArray

    init {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        val mappedModel = channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )

        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(mappedModel, options)
        inputStream.close()
        fileDescriptor.close()

        reusableInputBuffer = ByteBuffer.allocateDirect(1 * FACE_INPUT_SIZE * FACE_INPUT_SIZE * 3 * Float.SIZE_BYTES).apply {
            order(ByteOrder.nativeOrder())
        }
        pixelCache = IntArray(FACE_INPUT_SIZE * FACE_INPUT_SIZE)
    }

    @Synchronized
    override fun extractEmbedding(faceBitmap: Bitmap): FloatArray? {
        if (isClosed.get() || faceBitmap.isRecycled) return null

        val processedBitmap = if (faceBitmap.width != FACE_INPUT_SIZE || faceBitmap.height != FACE_INPUT_SIZE) {
            Bitmap.createScaledBitmap(faceBitmap, FACE_INPUT_SIZE, FACE_INPUT_SIZE, true)
        } else {
            faceBitmap
        }

        processedBitmap.getPixels(pixelCache, 0, FACE_INPUT_SIZE, 0, 0, FACE_INPUT_SIZE, FACE_INPUT_SIZE)
        if (processedBitmap != faceBitmap) {
            processedBitmap.recycle()
        }

        reusableInputBuffer.rewind()
        for (color in pixelCache) {
            val red = ((color shr 16 and 0xFF) - NORM_MEAN) / NORM_STD
            val green = ((color shr 8 and 0xFF) - NORM_MEAN) / NORM_STD
            val blue = ((color and 0xFF) - NORM_MEAN) / NORM_STD
            reusableInputBuffer.putFloat(red)
            reusableInputBuffer.putFloat(green)
            reusableInputBuffer.putFloat(blue)
        }

        val outputArray = Array(1) { FloatArray(EMBEDDING_DIM) }
        try {
            interpreter.run(reusableInputBuffer, outputArray)
        } catch (_: Exception) {
            return null
        }

        val featureVector = outputArray[0]
        FaceRecognitionUtils.normalizeL2(featureVector)
        return featureVector
    }

    override fun release() {
        if (isClosed.compareAndSet(false, true)) {
            interpreter.close()
        }
    }
}
