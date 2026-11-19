package com.bachelor.projectiii.models

/**
 * Result model representing a face matching comparison against stored biometric templates.
 */
data class FaceMatchResModel(
    val bioId: Long? = null,
    val fullName: String? = null,
    val score: Float = 0.0f
) {
    val isMatched: Boolean
        get() = bioId != null
}
