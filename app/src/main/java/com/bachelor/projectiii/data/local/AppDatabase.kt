package com.bachelor.projectiii.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bachelor.projectiii.data.local.dao.BiometricDao
import com.bachelor.projectiii.data.local.entity.BiometricEntity

/**
 * Main Room Database for local persistence and offline access.
 * Currently configured with BiometricEntity (biometric_data).
 */
@Database(
    entities = [BiometricEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun biometricDao(): BiometricDao
}
