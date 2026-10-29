package com.bachelor.projectiii.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bachelor.projectiii.core.ServiceLocator
import com.bachelor.projectiii.data.local.prefs.DevicePrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val devicePrefs = remember { ServiceLocator.devicePrefs }

    // API Server
    var apiBaseUrl by remember { mutableStateOf("") }
    var apiSaltKey by remember { mutableStateOf("") }

    // Device Identity
    var macAddress by remember { mutableStateOf("") }

    // MQTT
    var brokerHost by remember { mutableStateOf("") }
    var brokerPort by remember { mutableStateOf("") }
    var keepAlive by remember { mutableStateOf("") }
    var reconnectDelay by remember { mutableStateOf("") }
    var enableTls by remember { mutableStateOf(false) }
    var cleanStart by remember { mutableStateOf(false) }

    // Periodic & Sync timers
    var healthcheckInterval by remember { mutableStateOf("") }
    var bioSyncInterval by remember { mutableStateOf("") }
    var doorSyncInterval by remember { mutableStateOf("") }
    var bioSyncTimeout by remember { mutableStateOf("") }

    // Load saved settings from DataStore
    LaunchedEffect(Unit) {
        val config = devicePrefs.configFlow.first()
        apiBaseUrl = config.apiBaseUrl
        apiSaltKey = config.apiSaltKey
        macAddress = config.macAddress

        brokerHost = config.brokerHost
        brokerPort = config.brokerPort?.toString() ?: ""
        keepAlive = config.keepAlive?.toString() ?: ""
        reconnectDelay = config.reconnectDelay?.toString() ?: ""
        enableTls = config.enableTls
        cleanStart = config.cleanStart

        healthcheckInterval = config.healthcheckInterval?.toString() ?: ""
        bioSyncInterval = config.bioSyncInterval?.toString() ?: ""
        doorSyncInterval = config.doorSyncInterval?.toString() ?: ""
        bioSyncTimeout = config.bioSyncTimeout?.toString() ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài Đặt Cấu Hình Thiết Bị",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: API Server
            SectionCard(title = "1. API AUTH GATEWAY") {
                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    label = { Text("Địa chỉ API Server") },
                    leadingIcon = {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = Color.Gray)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiSaltKey,
                    onValueChange = { apiSaltKey = it },
                    label = { Text("Salt key") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color.Gray)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = textFieldColors()
                )
            }

            // Group 2: Device Identity
            SectionCard(title = "2. ĐỊNH DANH THIẾT BỊ") {
                OutlinedTextField(
                    value = macAddress,
                    onValueChange = { macAddress = it },
                    label = { Text("Địa chỉ MAC Thiết Bị") },
                    leadingIcon = {
                        Icon(Icons.Default.Lan, contentDescription = null, tint = Color.Gray)
                    },
                    trailingIcon = {
                        // Scan Wi-Fi MAC button
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val scanned = DevicePrefs.getWifiMacAddress()
                                if (scanned != null) {
                                    macAddress = scanned
                                    Toast.makeText(context, "Đã lấy MAC: $scanned", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Không tìm thấy địa chỉ MAC", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Quét MAC Wi-Fi",
                                tint = Color(0xFF00E676)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors()
                )
            }

            // Group 3: MQTT
            SectionCard(title = "3. MQTT") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = brokerHost,
                        onValueChange = { brokerHost = it },
                        label = { Text("Địa chỉ Host / IP Broker") },
                        leadingIcon = {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = brokerPort,
                        onValueChange = { brokerPort = it },
                        label = { Text("Cổng (Port)") },
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = keepAlive,
                        onValueChange = { keepAlive = it },
                        label = { Text("Keep-Alive (giây)") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = reconnectDelay,
                        onValueChange = { reconnectDelay = it },
                        label = { Text("Chờ kết nối lại (giây)") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sử dụng TLS / SSL (MQTTS)",
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = enableTls,
                        onCheckedChange = { enableTls = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E676)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clean Session",
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = cleanStart,
                        onCheckedChange = { cleanStart = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E676)
                        )
                    )
                }
            }

            // Group 4: Periodic Timers & Sync
            SectionCard(title = "4. CHU KỲ ĐỒNG BỘ") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = healthcheckInterval,
                        onValueChange = { healthcheckInterval = it },
                        label = { Text("Healthcheck định kỳ (giây)") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = bioSyncInterval,
                        onValueChange = { bioSyncInterval = it },
                        label = { Text("Đồng bộ Bio (giây)") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = doorSyncInterval,
                        onValueChange = { doorSyncInterval = it },
                        label = { Text("Đồng bộ cửa (giây)") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = bioSyncTimeout,
                        onValueChange = { bioSyncTimeout = it },
                        label = { Text("Bio Sync Timeout (giây)") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    coroutineScope.launch {
                        devicePrefs.saveSettings(
                            apiBaseUrl = apiBaseUrl,
                            apiSaltKey = apiSaltKey,
                            macAddress = macAddress,
                            brokerHost = brokerHost,
                            brokerPort = brokerPort.toIntOrNull(),
                            enableTls = enableTls,
                            cleanStart = cleanStart,
                            keepAlive = keepAlive.toIntOrNull(),
                            reconnectDelay = reconnectDelay.toIntOrNull(),
                            healthcheckInterval = healthcheckInterval.toIntOrNull(),
                            bioSyncInterval = bioSyncInterval.toIntOrNull(),
                            doorSyncInterval = doorSyncInterval.toIntOrNull(),
                            bioSyncTimeout = bioSyncTimeout.toIntOrNull()
                        )
                        Toast.makeText(context, "Đã lưu cấu hình thiết bị thành công!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LƯU CẤU HÌNH",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF00E676),
    unfocusedBorderColor = Color(0xFF444444),
    focusedLabelColor = Color(0xFF00E676),
    unfocusedLabelColor = Color.Gray
)
