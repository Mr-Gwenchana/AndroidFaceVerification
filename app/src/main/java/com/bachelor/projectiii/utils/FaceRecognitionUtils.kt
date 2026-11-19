package com.bachelor.projectiii.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Utility functions for face recognition linear algebra and vector operations.
 */
object FaceRecognitionUtils {

    const val EMBEDDING_DIMENSION = 128
    const val EMBEDDING_BYTE_SIZE = EMBEDDING_DIMENSION * Float.SIZE_BYTES // 512 bytes
    const val DEFAULT_MATCH_THRESHOLD = 0.80f

    /**
     * Decodes a Base64 string of 512 bytes (Little-Endian Float32) into a 128-dimensional FloatArray,
     * and applies L2 normalization.
     *
     * @param base64Str Base64 encoded binary vector.
     * @return L2 normalized FloatArray of size 128, or null if decoding fails or size is invalid.
     */
    fun decodeBase64ToVector(base64Str: String): FloatArray? {
        if (base64Str.isBlank()) return null

        val bytes = decodeBase64Bytes(base64Str) ?: return null
        if (bytes.size != EMBEDDING_BYTE_SIZE) return null

        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val vector = FloatArray(EMBEDDING_DIMENSION)
        byteBuffer.asFloatBuffer().get(vector)

        normalizeL2(vector)
        return vector
    }

    /**
     * Normalizes a float vector in-place using L2 normalization (Euclidean norm = 1.0).
     */
    fun normalizeL2(vector: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (value in vector) {
            sumSquares += (value * value).toDouble()
        }

        val norm = sqrt(sumSquares).toFloat()
        if (norm > 1e-10f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }

    /**
     * Calculates the Cosine Similarity between two L2-normalized vectors.
     * For L2-normalized vectors, cosine similarity equals their dot product: sum(v1[i] * v2[i]).
     *
     * @return Cosine similarity clamped in range [-1.0f, 1.0f].
     */
    fun calculateCosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0.0f

        var dot = 0.0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
        }
        return dot.coerceIn(-1.0f, 1.0f)
    }

    /**
     * Converts a cosine similarity value into a normalized confidence score [0.0f, 1.0f].
     */
    fun similarityToScore(cosine: Float): Float {
        return cosine.coerceIn(0.0f, 1.0f)
    }

    /**
     * Directly compares two Base64 template strings and returns the similarity score [0.0f, 1.0f].
     */
    fun compareTemplates(templateA: String, templateB: String): Float {
        val vectorA = decodeBase64ToVector(templateA) ?: return 0.0f
        val vectorB = decodeBase64ToVector(templateB) ?: return 0.0f
        val cosine = calculateCosineSimilarity(vectorA, vectorB)
        return similarityToScore(cosine)
    }

    /**
     * Safe Base64 decoding compatible with Android runtime (API 24+) and local JVM Unit Tests.
     */
    private fun decodeBase64Bytes(str: String): ByteArray? {
        val cleanStr = str.trim()
        return try {
            java.util.Base64.getDecoder().decode(cleanStr)
        } catch (_: Throwable) {
            try {
                android.util.Base64.decode(cleanStr, android.util.Base64.DEFAULT)
            } catch (_: Throwable) {
                null
            }
        }
    }
}
