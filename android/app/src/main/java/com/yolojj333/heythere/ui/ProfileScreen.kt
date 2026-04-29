package com.yolojj333.heythere.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yolojj333.heythere.models.User
import com.yolojj333.heythere.utils.FirebaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    onUserChange: (User) -> Unit
) {
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

    val availableTags = listOf(
        "Hiking", "Coding", "Coffee", "Photography", "Volleyball",
        "Board Games", "Live Music", "Reading", "Foodie"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Your Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = user.displayName,
            onValueChange = { onUserChange(user.copy(displayName = it)) },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = user.bio,
            onValueChange = { onUserChange(user.copy(bio = it)) },
            label = { Text("Currently looking for...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Age: ${user.age}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = user.age.toFloat(),
            onValueChange = { onUserChange(user.copy(age = it.toInt())) },
            valueRange = 18f..120f,
            steps = 102
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Interests (Select multiple)", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        availableTags.chunked(3).forEach { rowTags ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTags.forEach { tag ->
                    val isSelected = user.subscribedTags.contains(tag)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newTags = if (isSelected) {
                                user.subscribedTags - tag
                            } else {
                                user.subscribedTags + tag
                            }
                            onUserChange(user.copy(subscribedTags = newTags))
                        },
                        label = { Text(tag) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isSaving = true
                FirebaseManager.saveUserProfile(
                    user = user,
                    onSuccess = {
                        isSaving = false
                        Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { exception ->
                        isSaving = false
                        Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            Text(if (isSaving) "Saving..." else "Save Profile")
        }
    }
}