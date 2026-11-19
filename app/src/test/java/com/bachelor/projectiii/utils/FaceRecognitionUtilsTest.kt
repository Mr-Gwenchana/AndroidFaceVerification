package com.bachelor.projectiii.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class FaceRecognitionUtilsTest {

    private fun createSampleBase64Vector(valueFill: Float = 1.0f): String {
        val byteBuffer = ByteBuffer.allocate(FaceRecognitionUtils.EMBEDDING_BYTE_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until FaceRecognitionUtils.EMBEDDING_DIMENSION) {
            byteBuffer.putFloat(valueFill)
        }
        return java.util.Base64.getEncoder().encodeToString(byteBuffer.array())
    }

    @Test
    fun decodeBase64ToVector_validInput_returnsNormalized128Vector() {
        val base64 = createSampleBase64Vector(2.0f)
        val vector = FaceRecognitionUtils.decodeBase64ToVector(base64)

        assertNotNull(vector)
        assertEquals(128, vector!!.size)

        // Check L2 norm is approximately 1.0
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v).toDouble()
        }
        val norm = sqrt(sumSquares)
        assertEquals(1.0, norm, 1e-4)
    }

    @Test
    fun decodeBase64ToVector_invalidInput_returnsNull() {
        assertNull(FaceRecognitionUtils.decodeBase64ToVector(""))
        assertNull(FaceRecognitionUtils.decodeBase64ToVector("   "))
        assertNull(FaceRecognitionUtils.decodeBase64ToVector("not_valid_base64!@#$"))

        // Valid base64 but invalid byte length (only 16 bytes instead of 512)
        val shortBytes = ByteArray(16)
        val shortBase64 = java.util.Base64.getEncoder().encodeToString(shortBytes)
        assertNull(FaceRecognitionUtils.decodeBase64ToVector(shortBase64))
    }

    @Test
    fun normalizeL2_normalizesCorrectly() {
        val vector = floatArrayOf(3.0f, 4.0f)
        FaceRecognitionUtils.normalizeL2(vector)

        assertEquals(0.6f, vector[0], 1e-5f)
        assertEquals(0.8f, vector[1], 1e-5f)
    }

    @Test
    fun normalizeL2_zeroVector_doesNotProduceNaN() {
        val vector = floatArrayOf(0.0f, 0.0f, 0.0f)
        FaceRecognitionUtils.normalizeL2(vector)

        assertEquals(0.0f, vector[0], 1e-5f)
        assertEquals(0.0f, vector[1], 1e-5f)
        assertEquals(0.0f, vector[2], 1e-5f)
    }

    @Test
    fun calculateCosineSimilarity_identicalVectors_returnsOne() {
        val v1 = floatArrayOf(0.6f, 0.8f)
        val v2 = floatArrayOf(0.6f, 0.8f)

        val similarity = FaceRecognitionUtils.calculateCosineSimilarity(v1, v2)
        assertEquals(1.0f, similarity, 1e-5f)
    }

    @Test
    fun calculateCosineSimilarity_orthogonalVectors_returnsZero() {
        val v1 = floatArrayOf(1.0f, 0.0f)
        val v2 = floatArrayOf(0.0f, 1.0f)

        val similarity = FaceRecognitionUtils.calculateCosineSimilarity(v1, v2)
        assertEquals(0.0f, similarity, 1e-5f)
    }

    @Test
    fun calculateCosineSimilarity_oppositeVectors_returnsMinusOne() {
        val v1 = floatArrayOf(1.0f, 0.0f)
        val v2 = floatArrayOf(-1.0f, 0.0f)

        val similarity = FaceRecognitionUtils.calculateCosineSimilarity(v1, v2)
        assertEquals(-1.0f, similarity, 1e-5f)
    }

    @Test
    fun similarityToScore_clampsCorrectly() {
        assertEquals(0.85f, FaceRecognitionUtils.similarityToScore(0.85f), 1e-5f)
        assertEquals(0.0f, FaceRecognitionUtils.similarityToScore(-0.3f), 1e-5f)
        assertEquals(1.0f, FaceRecognitionUtils.similarityToScore(1.2f), 1e-5f)
    }

    @Test
    fun compareTemplates_identicalTemplates_returnsOne() {
        val template = createSampleBase64Vector(1.5f)
        val score = FaceRecognitionUtils.compareTemplates(template, template)
        assertEquals(1.0f, score, 1e-4f)
    }

    @Test
    fun compareTemplates_invalidTemplate_returnsZero() {
        val validTemplate = createSampleBase64Vector(1.0f)
        val score = FaceRecognitionUtils.compareTemplates(validTemplate, "invalid_base64")
        assertEquals(0.0f, score, 1e-5f)
    }
}
