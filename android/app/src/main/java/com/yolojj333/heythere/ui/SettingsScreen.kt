package com.yolojj333.heythere.ui

import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yolojj333.heythere.models.BlackoutZone
import com.yolojj333.heythere.models.PrivacySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    privacySettings: PrivacySettings,
    onSettingsChange: (PrivacySettings) -> Unit,
    onSignOut: () -> Unit
) {
    var zoneInput by remember { mutableStateOf("") }
    var radiusInput by remember { mutableFloatStateOf(200f) }
    var intervalInput by remember { mutableStateOf(privacySettings.backgroundUpdateIntervalSeconds.toString()) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGeocoding by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Settings & Privacy", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Location Visibility", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingToggleRow(
            title = "Broadcast My Location",
            description = "When off, you will disappear from the map entirely.",
            isChecked = privacySettings.isGlobalLocationOn,
            onCheckedChange = { onSettingsChange(privacySettings.copy(isGlobalLocationOn = it)) }
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text(text = "Location Accuracy", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingToggleRow(
            title = "Use Precise Location",
            description = if (privacySettings.usePreciseLocation)
                "Your exact GPS location is shared with matching users."
            else
                "A randomized 500m noise radius protects your exact location.",
            isChecked = privacySettings.usePreciseLocation,
            onCheckedChange = { onSettingsChange(privacySettings.copy(usePreciseLocation = it)) }
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text(text = "Background Updates", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        SettingToggleRow(
            title = "Run in Background",
            description = "Allow the app to update your location while minimized.",
            isChecked = privacySettings.isBackgroundLocationEnabled,
            onCheckedChange = { onSettingsChange(privacySettings.copy(isBackgroundLocationEnabled = it)) }
        )

        if (privacySettings.isBackgroundLocationEnabled) {
            OutlinedTextField(
                value = intervalInput,
                onValueChange = { newValue ->
                    intervalInput = newValue
                    val parsed = newValue.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        onSettingsChange(privacySettings.copy(backgroundUpdateIntervalSeconds = parsed))
                    }
                },
                label = { Text("Update Interval (Seconds)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text(text = "Blackout Zones", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Your location will turn off automatically when entering these custom zones (Max 3).",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        privacySettings.activeBlackoutZones.forEach { zone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = zone.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Radius: ${zone.radiusMeters.roundToInt()} meters", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    val newZones = privacySettings.activeBlackoutZones.toMutableList()
                    newZones.remove(zone)
                    onSettingsChange(privacySettings.copy(activeBlackoutZones = newZones))
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Zone", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (privacySettings.activeBlackoutZones.size < 3) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = zoneInput,
                        onValueChange = { zoneInput = it },
                        label = { Text(if (isGeocoding) "Locating..." else "Enter Address or Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isGeocoding
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Radius: ${radiusInput.roundToInt()} meters", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = radiusInput,
                        onValueChange = { radiusInput = it },
                        valueRange = 50f..2000f,
                        enabled = !isGeocoding
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (zoneInput.isNotBlank() && !isGeocoding) {
                                isGeocoding = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val geocoder = Geocoder(context)
                                        @Suppress("DEPRECATION")
                                        val addresses = geocoder.getFromLocationName(zoneInput.trim(), 1)

                                        withContext(Dispatchers.Main) {
                                            isGeocoding = false
                                            if (!addresses.isNullOrEmpty()) {
                                                val location = addresses[0]
                                                val newZone = BlackoutZone(
                                                    name = zoneInput.trim(),
                                                    latitude = location.latitude,
                                                    longitude = location.longitude,
                                                    radiusMeters = radiusInput.toDouble()
                                                )
                                                val newZones = privacySettings.activeBlackoutZones.toMutableList()
                                                newZones.add(newZone)
                                                onSettingsChange(privacySettings.copy(activeBlackoutZones = newZones))
                                                zoneInput = ""
                                                radiusInput = 200f
                                            } else {
                                                Toast.makeText(context, "Address not found. Be more specific.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isGeocoding = false
                                            Toast.makeText(context, "Network error finding address.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGeocoding && zoneInput.isNotBlank()
                    ) {
                        if (isGeocoding) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Add Blackout Zone")
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Maximum of 3 blackout zones reached.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text(text = "Map Appearance", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Map Pin Size", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(text = "${privacySettings.mapPinSize}px", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

        Slider(
            value = privacySettings.mapPinSize.toFloat(),
            onValueChange = { newValue ->
                onSettingsChange(privacySettings.copy(mapPinSize = newValue.roundToInt()))
            },
            valueRange = 50f..250f,
            steps = 19,
            modifier = Modifier.fillMaxWidth()
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out")
        }
    }
}

@Composable
fun SettingToggleRow(title: String, description: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}