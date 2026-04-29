package com.yolojj333.heythere.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yolojj333.heythere.models.PrivacySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    privacySettings: PrivacySettings,
    onSettingsChange: (PrivacySettings) -> Unit,
    onSignOut: () -> Unit // <-- NEW LOGOUT PARAMETER
) {
    var zoneInput by remember { mutableStateOf("") }

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

        Text(text = "Blackout Zones", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Your location will turn off automatically when entering these zones (Max 3).",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        privacySettings.activeBlackoutZones.forEach { zone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = zone, fontSize = 16.sp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = zoneInput,
                    onValueChange = { zoneInput = it },
                    label = { Text("Enter Address or Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (zoneInput.isNotBlank()) {
                            val newZones = privacySettings.activeBlackoutZones.toMutableList()
                            newZones.add(zoneInput.trim())
                            onSettingsChange(privacySettings.copy(activeBlackoutZones = newZones))
                            zoneInput = ""
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Zone", tint = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            Text(
                text = "Maximum of 3 blackout zones reached.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // NEW LOGOUT BUTTON
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