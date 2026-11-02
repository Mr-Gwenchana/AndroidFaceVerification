package com.bachelor.projectiii.mqtt

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * HiveMQ MQTT 5.0 async client wrapper optimized for Android embedded access control.
 * Manages connection, subscriptions, and publishing with native Coroutine support.
 */
class MqttClient(
    private val onDisconnectedListener: ((cause: Throwable?) -> Unit)? = null
) {
    companion object {
        private const val TAG = "MqttClient"
    }

    private var client: Mqtt5AsyncClient? = null

    val isConnected: Boolean
        get() = client?.state?.isConnected == true

    /**
     * Connects to MQTT broker with MQTT 5.0 simpleAuth and persistent session.
     */
    suspend fun connect(
        host: String,
        port: Int,
        mac: String,
        token: String,
        enableTls: Boolean = false,
        cleanStart: Boolean = false,
        keepAliveSeconds: Int = 60,
        sessionExpirySeconds: Long = 7 * 24 * 3600L
    ): Result<Mqtt5ConnAck> = suspendCancellableCoroutine { continuation ->
        try {
            // Disconnect obsolete client if already connected before building a new one
            client?.let { oldClient ->
                if (oldClient.state.isConnected) {
                    oldClient.disconnect()
                }
            }

            var asyncClient: Mqtt5AsyncClient? = null
            val clientBuilder = MqttClient.builder()
                .useMqttVersion5()
                .identifier(mac)
                .serverHost(host)
                .serverPort(port)
                .addDisconnectedListener { context ->
                    // Ignore disconnect events from obsolete/replaced clients
                    if (client !== asyncClient) return@addDisconnectedListener
                    // Ignore intentional disconnections triggered by user/app
                    if (context.source.name == "USER") {
                        return@addDisconnectedListener
                    }
                    val cause = context.cause
                    Log.w(TAG, "MQTT disconnected: ${cause?.message}", cause)
                    onDisconnectedListener?.invoke(cause)
                }

            if (enableTls) {
                clientBuilder.sslWithDefaultConfig()
            }

            asyncClient = clientBuilder.buildAsync()
            client = asyncClient

            val connectFuture = asyncClient.connectWith()
                .cleanStart(cleanStart)
                .sessionExpiryInterval(sessionExpirySeconds)
                .keepAlive(keepAliveSeconds)
                .simpleAuth()
                    .username(mac)
                    .password(token.toByteArray(Charsets.UTF_8))
                    .applySimpleAuth()
                .send()

            connectFuture.whenComplete { connAck, throwable ->
                if (continuation.isActive) {
                    if (throwable != null) {
                        Log.e(TAG, "Connect failed: ${throwable.message}", throwable)
                        continuation.resume(Result.failure(throwable))
                    } else {
                        Log.i(TAG, "Connected to $host:$port successfully (ReasonCode: ${connAck.reasonCode})")
                        continuation.resume(Result.success(connAck))
                    }
                }
            }

            continuation.invokeOnCancellation {
                if (!connectFuture.isDone) {
                    connectFuture.cancel(true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connect setup: ${e.message}", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Gracefully disconnects from broker.
     */
    suspend fun disconnect(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val currentClient = client
        if (currentClient == null || !currentClient.state.isConnected) {
            if (continuation.isActive) continuation.resume(Result.success(Unit))
            return@suspendCancellableCoroutine
        }

        currentClient.disconnect().whenComplete { _, throwable ->
            client = null
            if (continuation.isActive) {
                if (throwable != null) {
                    Log.w(TAG, "Disconnect completed with error: ${throwable.message}")
                    continuation.resume(Result.failure(throwable))
                } else {
                    Log.i(TAG, "Disconnected cleanly.")
                    continuation.resume(Result.success(Unit))
                }
            }
        }
    }

    /**
     * Subscribes to topic filter with specified QoS and delivers payloads to callback.
     */
    suspend fun subscribe(
        topicFilter: String,
        qos: MqttQos = MqttQos.EXACTLY_ONCE,
        onMessage: (topic: String, payload: ByteArray) -> Unit
    ): Result<Mqtt5SubAck> = suspendCancellableCoroutine { continuation ->
        val currentClient = client
        if (currentClient == null || !currentClient.state.isConnected) {
            if (continuation.isActive) {
                continuation.resume(Result.failure(IllegalStateException("MQTT client is not connected")))
            }
            return@suspendCancellableCoroutine
        }

        val subFuture = currentClient.subscribeWith()
            .topicFilter(topicFilter)
            .qos(qos)
            .callback { publish ->
                val topic = publish.topic.toString()
                val payload = publish.payloadAsBytes
                onMessage(topic, payload)
            }
            .send()

        subFuture.whenComplete { subAck, throwable ->
            if (continuation.isActive) {
                if (throwable != null) {
                    Log.e(TAG, "Subscribe failed on $topicFilter: ${throwable.message}", throwable)
                    continuation.resume(Result.failure(throwable))
                } else {
                    Log.i(TAG, "Subscribed to $topicFilter (QoS: $qos)")
                    continuation.resume(Result.success(subAck))
                }
            }
        }

        continuation.invokeOnCancellation {
            if (!subFuture.isDone) {
                subFuture.cancel(true)
            }
        }
    }

    /**
     * Publishes payload to topic asynchronously.
     */
    suspend fun publish(
        topic: String,
        payload: ByteArray,
        qos: MqttQos = MqttQos.AT_LEAST_ONCE,
        retain: Boolean = false
    ): Result<Mqtt5PublishResult> = suspendCancellableCoroutine { continuation ->
        val currentClient = client
        if (currentClient == null || !currentClient.state.isConnected) {
            if (continuation.isActive) {
                continuation.resume(Result.failure(IllegalStateException("MQTT client is not connected")))
            }
            return@suspendCancellableCoroutine
        }

        val pubFuture = currentClient.publishWith()
            .topic(topic)
            .payload(payload)
            .qos(qos)
            .retain(retain)
            .send()

        pubFuture.whenComplete { result, throwable ->
            if (continuation.isActive) {
                if (throwable != null) {
                    Log.e(TAG, "Publish failed on $topic: ${throwable.message}", throwable)
                    continuation.resume(Result.failure(throwable))
                } else {
                    continuation.resume(Result.success(result))
                }
            }
        }

        continuation.invokeOnCancellation {
            if (!pubFuture.isDone) {
                pubFuture.cancel(true)
            }
        }
    }
}
