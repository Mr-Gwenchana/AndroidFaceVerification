package com.bachelor.projectiii.faceRecognition

import com.bachelor.projectiii.data.local.dao.BiometricDao
import com.bachelor.projectiii.data.local.entity.BiometricEntity
import com.bachelor.projectiii.utils.FaceRecognitionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceTemplateCacheTest {

    private fun createSampleBase64Vector(valueFill: Float = 1.0f): String {
        val byteBuffer = ByteBuffer.allocate(FaceRecognitionUtils.EMBEDDING_BYTE_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until FaceRecognitionUtils.EMBEDDING_DIMENSION) {
            byteBuffer.putFloat(valueFill)
        }
        return java.util.Base64.getEncoder().encodeToString(byteBuffer.array())
    }

    private class FakeBiometricDao(
        private val flow: Flow<List<BiometricEntity>>
    ) : BiometricDao {
        override suspend fun insertOrReplace(entity: BiometricEntity) {}
        override suspend fun insertOrReplaceAll(entities: List<BiometricEntity>) {}
        override suspend fun update(entity: BiometricEntity) {}
        override suspend fun delete(entity: BiometricEntity) {}
        override suspend fun deleteByBioId(bioId: Long) {}
        override suspend fun deleteByBioIds(bioIds: List<Long>) {}
        override suspend fun deleteAll() {}
        override suspend fun getByBioId(bioId: Long): BiometricEntity? = null
        override suspend fun getByIdNumber(idNumber: String): BiometricEntity? = null
        override suspend fun getAllBioIds(): List<Long> = emptyList()
        override fun getAllAsFlow(): Flow<List<BiometricEntity>> = flow
        override suspend fun getAll(): List<BiometricEntity> = emptyList()
        override suspend fun getCount(): Int = 0
    }

    @Test
    fun updateCache_validEntities_populatesProfilesCorrectly() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val flow = MutableStateFlow<List<BiometricEntity>>(emptyList())
        val cache = FaceTemplateCache(FakeBiometricDao(flow), testScope)

        val template1 = createSampleBase64Vector(1.0f)
        val template2 = createSampleBase64Vector(2.0f)

        val entity1 = BiometricEntity(
            bioId = 101L,
            doorId = 1,
            idNumber = "NV001",
            fullName = "Nguyen Van A",
            faceTempsJson = "[\"$template1\", \"$template2\"]",
            isNoTemplate = false
        )

        val entity2 = BiometricEntity(
            bioId = 102L,
            doorId = 1,
            idNumber = "NV002",
            fullName = "Tran Van B",
            faceTempsJson = "[\"invalid_base64\"]",
            isNoTemplate = false
        )

        val entity3NoTemplate = BiometricEntity(
            bioId = 103L,
            doorId = 1,
            idNumber = "NV003",
            fullName = "Le Van C",
            faceTempsJson = "[\"$template1\"]",
            isNoTemplate = true
        )

        cache.updateCache(listOf(entity1, entity2, entity3NoTemplate))

        val profiles = cache.getProfiles()
        assertEquals(1, profiles.size)
        assertEquals(1, cache.getCount())

        val profile = profiles[0]
        assertEquals(101L, profile.bioId)
        assertEquals("Nguyen Van A", profile.fullName)
        assertEquals(2, profile.rgbVectors.size)
        assertEquals(128, profile.rgbVectors[0].size)
    }

    @Test
    fun clear_removesAllProfiles() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val flow = MutableStateFlow<List<BiometricEntity>>(emptyList())
        val cache = FaceTemplateCache(FakeBiometricDao(flow), testScope)

        val template = createSampleBase64Vector(1.0f)
        val entity = BiometricEntity(
            bioId = 1L,
            doorId = 1,
            idNumber = "NV01",
            fullName = "Test",
            faceTempsJson = "[\"$template\"]"
        )

        cache.updateCache(listOf(entity))
        assertEquals(1, cache.getCount())

        cache.clear()
        assertEquals(0, cache.getCount())
        assertTrue(cache.getProfiles().isEmpty())
    }
}
