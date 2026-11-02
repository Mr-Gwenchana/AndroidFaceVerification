package com.bachelor.projectiii.mqtt

import android.util.Log
import com.bachelor.projectiii.interfaces.mqtt.MqttTopicHandler

/**
 * Dynamic message router matching incoming topic names to registered MqttTopicHandler instances.
 * Decoupled from specific handler implementations via the MqttTopicHandler interface.
 */
class MqttMessageRouter(
    private val handlers: List<MqttTopicHandler>
) {
    companion object {
        private const val TAG = "MqttRouter"

        /**
         * Matches actual topic against topic pattern supporting '+' and '#' wildcards.
         */
        internal fun matchTopic(pattern: String, actual: String): Boolean {
            if (pattern == actual || pattern == "#") return true
            val p = pattern.split("/")
            val a = actual.split("/")
            if (pattern.endsWith("/#")) {
                val prefix = pattern.removeSuffix("/#").split("/")
                if (a.size < prefix.size) return false
                for (i in prefix.indices) {
                    if (prefix[i] != "+" && prefix[i] != a[i]) return false
                }
                return true
            }
            if (p.size != a.size) return false
            for (i in p.indices) {
                if (p[i] != "+" && p[i] != a[i]) return false
            }
            return true
        }
    }

    /**
     * Routes received payload to the first matching registered handler.
     */
    suspend fun route(topic: String, payload: ByteArray) {
        val handler = handlers.firstOrNull { matchTopic(it.topicPattern, topic) }
        if (handler != null) {
            try {
                handler.handle(topic, payload)
            } catch (e: Exception) {
                Log.e(TAG, "Handler failed for topic '$topic': ${e.message}", e)
            }
        } else {
            Log.w(TAG, "No handler registered for topic: $topic")
        }
    }
}
