package com.yolojj333.heythere.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yolojj333.heythere.models.User
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    user: User,
    onUserChange: (User) -> Unit,
    onSaveProfile: () -> Unit // NEW: Parameter to handle the save action
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let {
                val updatedUser = user.copy(profileImageUrls = listOf(it.toString()))
                onUserChange(updatedUser)
            }
        }
    )

    val predefinedTags = listOf(
        "Coffee", "Gym", "Coding", "Music", "Movies", "Art", "Books",
        "Traveling", "Photography", "Cooking", "Sports", "Technology",
        "Outdoors", "Fitness", "Pets", "DIY", "Fashion", "Gaming", "Hiking",
        "Foodie", "Dancing", "Yoga", "Anime", "Board Games", "Cars", "Investing"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        // HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Your Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImageUrls.isNotEmpty() && user.profileImageUrls.first().isNotBlank()) {
                    AsyncImage(
                        model = user.profileImageUrls.first(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = "Add Photo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // ---> ADJUST THIS VALUE (e.g., 4.dp, 8.dp, 16.dp) TO MOVE THE CONTENT UP OR DOWN <---
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = user.displayName,
            onValueChange = { onUserChange(user.copy(displayName = it)) },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // BIO FIELD
        OutlinedTextField(
            value = user.bio,
            onValueChange = { onUserChange(user.copy(bio = it)) },
            label = { Text("Bio") },
            placeholder = { Text("Tell me about yourself") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // AGE SLIDER
        Text(text = "Age: ${user.age}", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = user.age.toFloat(),
            onValueChange = { onUserChange(user.copy(age = it.roundToInt())) },
            valueRange = 18f..111f,
            steps = 111 - 18 - 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = user.gender,
            onValueChange = { onUserChange(user.copy(gender = it)) },
            label = { Text("Gender Identity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // INTERESTS
        Text(text = "Interests", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(text = "Select tags that match your interests.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            predefinedTags.forEach { tag ->
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

        Spacer(modifier = Modifier.height(32.dp))

        // NEW: Save Profile Button
        Button(
            onClick = onSaveProfile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}