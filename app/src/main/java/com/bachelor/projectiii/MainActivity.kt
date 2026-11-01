package com.bachelor.projectiii

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.bachelor.projectiii.mqtt.MqttForegroundService
import com.bachelor.projectiii.screens.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, MqttForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}