package com.bachelor.projectiii.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bachelor.projectiii.models.mqtt.bio.ScheduleDay
import kotlinx.serialization.json.Json

/**
 * Room Entity representing stored biometric user profiles and access schedules.
 * Corresponds to SQLite table "biometric_data".
 */
@Entity(tableName = "biometric_data")
data class BiometricEntity(
    @PrimaryKey
    @ColumnInfo(name = "bio_id")
    val bioId: Long,

    @ColumnInfo(name = "door_id")
    val doorId: Int,

    @ColumnInfo(name = "id_number")
    val idNumber: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "from_date")
    val fromDate: String? = null,

    @ColumnInfo(name = "to_date")
    val toDate: String? = null,

    @ColumnInfo(name = "from_time")
    val fromTime: String? = null,

    @ColumnInfo(name = "to_time")
    val toTime: String? = null,

    @ColumnInfo(name = "active_days")
    val activeDays: String? = null,

    @ColumnInfo(name = "lst_times_json")
    val lstTimesJson: String? = null,

    @ColumnInfo(name = "face_temps_json")
    val faceTempsJson: String? = null,

    @ColumnInfo(name = "ir_face_temps_json")
    val irFaceTempsJson: String? = null,

    @ColumnInfo(name = "face_img")
    val faceImg: String? = null,

    @ColumnInfo(name = "is_no_template")
    val isNoTemplate: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Helper to deserialize LstTimes JSON string into List of ScheduleDay.
     */
    fun getScheduleDays(json: Json = Json { ignoreUnknownKeys = true }): List<ScheduleDay>? {
        if (lstTimesJson.isNullOrBlank()) return null
        return try {
            json.decodeFromString<List<ScheduleDay>>(lstTimesJson)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Helper to deserialize FaceTemps JSON string into List of base64 template Strings.
     */
    fun getFaceTemplates(json: Json = Json { ignoreUnknownKeys = true }): List<String>? {
        if (faceTempsJson.isNullOrBlank()) return null
        return try {
            json.decodeFromString<List<String>>(faceTempsJson)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Helper to deserialize IrFaceTemps JSON string into List of base64 template Strings.
     */
    fun getIrFaceTemplates(json: Json = Json { ignoreUnknownKeys = true }): List<String>? {
        if (irFaceTempsJson.isNullOrBlank()) return null
        return try {
            json.decodeFromString<List<String>>(irFaceTempsJson)
        } catch (_: Exception) {
            null
        }
    }
}
