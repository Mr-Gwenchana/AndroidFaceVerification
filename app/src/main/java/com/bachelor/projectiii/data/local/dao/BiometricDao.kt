package com.bachelor.projectiii.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bachelor.projectiii.data.local.entity.BiometricEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SQLite table "biometric_data".
 */
@Dao
interface BiometricDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: BiometricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(entities: List<BiometricEntity>)

    @Update
    suspend fun update(entity: BiometricEntity)

    @Delete
    suspend fun delete(entity: BiometricEntity)

    @Query("DELETE FROM biometric_data WHERE bio_id = :bioId")
    suspend fun deleteByBioId(bioId: Long)

    @Query("DELETE FROM biometric_data WHERE bio_id IN (:bioIds)")
    suspend fun deleteByBioIds(bioIds: List<Long>)

    @Query("DELETE FROM biometric_data")
    suspend fun deleteAll()

    @Query("SELECT * FROM biometric_data WHERE bio_id = :bioId LIMIT 1")
    suspend fun getByBioId(bioId: Long): BiometricEntity?

    @Query("SELECT * FROM biometric_data WHERE id_number = :idNumber LIMIT 1")
    suspend fun getByIdNumber(idNumber: String): BiometricEntity?

    @Query("SELECT bio_id FROM biometric_data")
    suspend fun getAllBioIds(): List<Long>

    @Query("SELECT * FROM biometric_data ORDER BY updated_at DESC")
    fun getAllAsFlow(): Flow<List<BiometricEntity>>

    @Query("SELECT * FROM biometric_data")
    suspend fun getAll(): List<BiometricEntity>

    @Query("SELECT COUNT(*) FROM biometric_data")
    suspend fun getCount(): Int
}
