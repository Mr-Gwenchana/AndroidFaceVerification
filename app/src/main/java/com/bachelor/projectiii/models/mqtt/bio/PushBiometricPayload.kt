package com.bachelor.projectiii.models.mqtt.bio

import com.bachelor.projectiii.data.local.entity.BiometricEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Time range representing start and end time of a daily access schedule.
 * Example: { "Start": "08:00:00", "End": "11:00:00" }
 */
@Serializable
data class ScheduleTimeRange(
    @SerialName("Start")
    val start: String,
    @SerialName("End")
    val end: String
)

/**
 * Schedule representing per-day access time ranges for a biometric profile.
 * Example: { "Day": "2026-10-22", "Time": [ { "Start": "08:00:00", "End": "11:00:00" } ] }
 */
@Serializable
data class ScheduleDay(
    @SerialName("Day")
    val day: String,
    @SerialName("Time")
    val time: List<ScheduleTimeRange> = emptyList()
)

/**
 * Incoming payload from MQTT topic "iot/server/push_biometric/<MAC>".
 * Supports both NEW_BIO (create/update) and DELETE_BIO commands.
 */
@Serializable
data class PushBiometricPayload(
    @SerialName("CmdType")
    val cmdType: String,

    @SerialName("BioId")
    val bioId: Long,

    @SerialName("DoorId")
    val doorId: Int = 0,

    @SerialName("IdNumber")
    val idNumber: String = "",

    @SerialName("FullName")
    val fullName: String = "",

    @SerialName("FromDate")
    val fromDate: String? = null,

    @SerialName("ToDate")
    val toDate: String? = null,

    @SerialName("FromTime")
    val fromTime: String? = null,

    @SerialName("ToTime")
    val toTime: String? = null,

    @SerialName("ActiveDays")
    val activeDays: String? = null,

    @SerialName("LstTimes")
    val lstTimes: List<ScheduleDay>? = null,

    @SerialName("FaceTemps")
    val faceTemps: List<String>? = null,

    @SerialName("IrFaceTemps")
    val irFaceTemps: List<String>? = null,

    @SerialName("FingerTemps")
    val fingerTemps: List<String>? = null,

    @SerialName("IrisTemps")
    val irisTemps: List<String>? = null,

    @SerialName("IsNoTemplate")
    val isNoTemplate: Boolean = false,

    @SerialName("FaceImg")
    val faceImg: String? = null,

    @SerialName("FingerImg")
    val fingerImg: String? = null,

    @SerialName("IrisImg")
    val irisImg: String? = null,

    @SerialName("SyncEnded")
    val syncEnded: Boolean = false,

    @SerialName("IsCheckIn")
    val isCheckIn: Boolean = false
) {
    /**
     * Converts MQTT payload into database entity BiometricEntity.
     */
    fun toEntity(json: Json = Json { ignoreUnknownKeys = true }): BiometricEntity {
        return BiometricEntity(
            bioId = bioId,
            doorId = doorId,
            idNumber = idNumber,
            fullName = fullName,
            fromDate = fromDate,
            toDate = toDate,
            fromTime = fromTime,
            toTime = toTime,
            activeDays = activeDays,
            lstTimesJson = lstTimes?.let { json.encodeToString(it) },
            faceTempsJson = faceTemps?.let { json.encodeToString(it) },
            irFaceTempsJson = irFaceTemps?.let { json.encodeToString(it) },
            faceImg = faceImg,
            isNoTemplate = isNoTemplate,
            updatedAt = System.currentTimeMillis()
        )
    }
}
