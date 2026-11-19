package com.bachelor.projectiii.faceRecognition

import com.bachelor.projectiii.data.local.dao.BiometricDao
import com.bachelor.projectiii.data.local.entity.BiometricEntity
import com.bachelor.projectiii.utils.FaceRecognitionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

/**
 * Pre-decoded in-memory profile holding 128-d L2 normalized float vectors.
 */
data class CachedBioProfile(
    val bioId: Long,
    val fullName: String,
    val rgbVectors: List<FloatArray>
)

/**
 * In-memory RAM cache for face biometric templates.
 *
 * Keeps pre-decoded 128-dimensional FloatArray vectors ready for sub-millisecond 1:N face matching
 * in the real-time camera loop (15-30 FPS) without SQLite queries or JSON parsing.
 *
 * Automatically synchronizes with Room SQLite database via [BiometricDao.getAllAsFlow].
 */
class FaceTemplateCache(
    private val biometricDao: BiometricDao,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val profilesRef = AtomicReference<List<CachedBioProfile>>(emptyList())
    private val json = Json { ignoreUnknownKeys = true }

    init {
        startAutoSync()
    }

    private fun startAutoSync() {
        coroutineScope.launch {
            biometricDao.getAllAsFlow().collectLatest { entities ->
                updateCache(entities)
            }
        }
    }

    /**
     * Rebuilds the in-memory cache from a list of BiometricEntity records.
     */
    fun updateCache(entities: List<BiometricEntity>) {
        val newProfiles = ArrayList<CachedBioProfile>(entities.size)

        for (entity in entities) {
            if (entity.isNoTemplate) continue

            val templates = entity.getFaceTemplates(json) ?: continue
            if (templates.isEmpty()) continue

            val vectors = ArrayList<FloatArray>(templates.size)
            for (template in templates) {
                val vector = FaceRecognitionUtils.decodeBase64ToVector(template)
                if (vector != null) {
                    vectors.add(vector)
                }
            }

            if (vectors.isNotEmpty()) {
                newProfiles.add(
                    CachedBioProfile(
                        bioId = entity.bioId,
                        fullName = entity.fullName,
                        rgbVectors = vectors
                    )
                )
            }
        }

        profilesRef.set(newProfiles)
    }

    /**
     * Lock-free read of all cached profiles for the real-time camera recognition loop.
     */
    fun getProfiles(): List<CachedBioProfile> = profilesRef.get()

    /**
     * Returns the total number of cached profiles.
     */
    fun getCount(): Int = profilesRef.get().size

    /**
     * Manually triggers a reload from the database.
     */
    suspend fun reload() {
        val entities = biometricDao.getAll()
        updateCache(entities)
    }

    /**
     * Clears all cached templates in memory.
     */
    fun clear() {
        profilesRef.set(emptyList())
    }

    /**
     * Cancels background synchronization.
     */
    fun release() {
        coroutineScope.cancel()
    }
}
