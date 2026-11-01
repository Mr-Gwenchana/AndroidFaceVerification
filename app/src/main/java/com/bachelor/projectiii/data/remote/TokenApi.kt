package com.bachelor.projectiii.data.remote

import android.util.Base64
import com.bachelor.projectiii.models.mqtt.token.TokenRequestPayload
import com.bachelor.projectiii.models.mqtt.token.TokenResponsePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Handles HTTP requests to ApiGateway to obtain device JWT tokens.
 */
class TokenApi(
    private val httpClient: OkHttpClient = defaultHttpClient,
    private val json: Json = defaultJson
) {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val defaultJson by lazy {
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
        }

        val defaultHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Computes Base64(SHA256(mac + salt)) without line breaks.
         */
        fun computePasswordHash(mac: String, salt: String): String {
            val raw = mac.trim() + salt.trim()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(digest, Base64.NO_WRAP)
        }
    }

    /**
     * Calls POST /api/devicecomm/getmqtttoken and returns the JWT token.
     */
    suspend fun getToken(baseUrl: String, mac: String, salt: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = baseUrl.trim().trimEnd('/')
                val password = computePasswordHash(mac, salt)
                val requestPayload = TokenRequestPayload(
                    macAddress = mac.trim(),
                    password = password
                )
                val requestBody = json.encodeToString(
                    TokenRequestPayload.serializer(),
                    requestPayload
                ).toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder()
                    .url("$cleanUrl/api/devicecomm/getmqtttoken")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("HTTP ${response.code}: ${response.message}")
                        )
                    }

                    val bodyString = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response body"))

                    val tokenResponse = json.decodeFromString<TokenResponsePayload>(bodyString)
                    if (tokenResponse.code == "OK") {
                        val token = tokenResponse.data?.token
                        if (!token.isNullOrBlank()) {
                            Result.success(token)
                        } else {
                            Result.failure(IllegalStateException("Token missing in response payload"))
                        }
                    } else {
                        Result.failure(
                            Exception("Server error: ${tokenResponse.code} - ${tokenResponse.message.orEmpty()}")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
