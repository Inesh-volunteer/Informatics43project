package com.yolojj333.heythere

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import com.yolojj333.heythere.models.LocationData
import com.yolojj333.heythere.models.User
import com.yolojj333.heythere.ui.AuthScreen
import com.yolojj333.heythere.ui.MessagesScreen
import com.yolojj333.heythere.ui.ProfileScreen
import com.yolojj333.heythere.ui.SettingsScreen
import com.yolojj333.heythere.ui.theme.BeaconTheme
import com.yolojj333.heythere.utils.FirebaseManager
import com.yolojj333.heythere.utils.LocationUtils
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeaconTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BeaconAppRoot()
                }
            }
        }
    }
}

@Composable
fun BeaconAppRoot() {
    val auth = FirebaseAuth.getInstance()
    var isUserLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    if (isUserLoggedIn) {
        MainAppScaffold(onSignOut = {
            auth.signOut()
            isUserLoggedIn = false
        })
    } else {
        AuthScreen(onAuthSuccess = {
            isUserLoggedIn = true
        })
    }
}

@Composable
fun MainAppScaffold(onSignOut: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val sharedPreferences = remember { context.getSharedPreferences("HeyTherePrefs", Context.MODE_PRIVATE) }

    var currentRoute by remember { mutableStateOf("map") }
    var currentUser by remember { mutableStateOf(User()) }
    var isProfileLoaded by remember { mutableStateOf(false) }
    var allCloudUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    var isSavingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            FirebaseManager.getUserProfile(
                uid = uid,
                onResult = { fetchedUser ->
                    if (fetchedUser != null) {
                        currentUser = fetchedUser
                    } else {
                        currentUser = User(userId = uid)
                    }
                    isProfileLoaded = true
                },
                onFailure = {
                    Toast.makeText(context, "Failed to connect to database", Toast.LENGTH_SHORT).show()
                    currentUser = User(userId = uid)
                    isProfileLoaded = true
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        FirebaseManager.listenToAllUsers(
            onResult = { users -> allCloudUsers = users },
            onFailure = { /* Silent fail for now */ }
        )
    }

    if (!isProfileLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val savedLat = sharedPreferences.getFloat("last_lat", 0f).toDouble()
    val savedLng = sharedPreferences.getFloat("last_lng", 0f).toDouble()

    val defaultLat = if (savedLat != 0.0) savedLat else currentUser.locationData.publicLatitude
    val defaultLng = if (savedLng != 0.0) savedLng else currentUser.locationData.publicLongitude

    val defaultLocation = LatLng(defaultLat, defaultLng)
    val initialZoom = if (defaultLat == 0.0 && defaultLng == 0.0) 2f else 14f

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, initialZoom)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Place, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = currentRoute == "map",
                    onClick = { currentRoute = "map" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Message, contentDescription = "Messages") },
                    label = { Text("Messages") },
                    selected = currentRoute == "messages",
                    onClick = { currentRoute = "messages" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = { currentRoute = "profile" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = { currentRoute = "settings" }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRoute) {
                "map" -> LocationPermissionScreen(
                    currentUser = currentUser,
                    cameraPositionState = cameraPositionState,
                    allCloudUsers = allCloudUsers,
                    onUserUpdate = { updatedUser ->
                        currentUser = updatedUser
                        FirebaseManager.saveUserProfile(updatedUser, {}, {})
                    }
                )

                "messages" -> MessagesScreen()

                "profile" -> {
                    if (isSavingProfile) {
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = 50.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    ProfileScreen(
                        user = currentUser,
                        onUserChange = { updatedUser -> currentUser = updatedUser },
                        onSaveProfile = {
                            if (isSavingProfile) return@ProfileScreen
                            isSavingProfile = true

                            val imageUrl = currentUser.profileImageUrls.firstOrNull()

                            if (imageUrl != null && imageUrl.startsWith("content://")) {
                                val uri = android.net.Uri.parse(imageUrl)

                                // LOCAL FILE SIZE CHECK
                                val fileSize = try {
                                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: 0L
                                } catch (e: Exception) {
                                    0L
                                }

                                // 5MB = 5 * 1024 * 1024 bytes
                                if (fileSize > 5242880L) {
                                    isSavingProfile = false
                                    Toast.makeText(context, "Image is too large. Limit is 5MB.", Toast.LENGTH_LONG).show()
                                    return@ProfileScreen
                                }

                                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()

                                FirebaseManager.uploadProfileImage(
                                    uri = uri,
                                    onSuccess = { publicDownloadUrl: String ->
                                        val finalUser = currentUser.copy(profileImageUrls = listOf(publicDownloadUrl))

                                        FirebaseManager.saveUserProfile(
                                            user = finalUser,
                                            onSuccess = {
                                                currentUser = finalUser
                                                isSavingProfile = false
                                                Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {
                                                isSavingProfile = false
                                                Toast.makeText(context, "Failed to save profile.", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onFailure = { error: Exception ->
                                        isSavingProfile = false
                                        val errorMsg = error.message ?: "Image upload failed."
                                        android.util.Log.e("UploadError", "Firebase Storage failed: $errorMsg")
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else if (imageUrl.isNullOrEmpty()) {
                                // User removed the photo. Delete it from cloud storage, then save profile.
                                FirebaseManager.deleteProfileImage(
                                    onSuccess = {
                                        FirebaseManager.saveUserProfile(
                                            user = currentUser,
                                            onSuccess = {
                                                isSavingProfile = false
                                                Toast.makeText(context, "Profile Saved! Photo removed.", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {
                                                isSavingProfile = false
                                                Toast.makeText(context, "Failed to save profile.", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onFailure = {
                                        isSavingProfile = false
                                        Toast.makeText(context, "Failed to delete old photo.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                // Image is an existing HTTPS URL, just save changes
                                FirebaseManager.saveUserProfile(
                                    user = currentUser,
                                    onSuccess = {
                                        isSavingProfile = false
                                        Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = {
                                        isSavingProfile = false
                                        Toast.makeText(context, "Failed to save profile.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    )
                }

                "settings" -> SettingsScreen(
                    privacySettings = currentUser.privacySettings,
                    onSettingsChange = { newSettings ->
                        val updatedUser = currentUser.copy(privacySettings = newSettings)
                        currentUser = updatedUser
                        FirebaseManager.saveUserProfile(updatedUser, {}, {})
                    },
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
fun LocationPermissionScreen(
    currentUser: User,
    cameraPositionState: CameraPositionState,
    allCloudUsers: List<User>,
    onUserUpdate: (User) -> Unit
) {
    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(currentUser.privacySettings.usePreciseLocation) {
        val permissionsToRequest = if (currentUser.privacySettings.usePreciseLocation) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    BeaconMapScreen(
        currentUser = currentUser,
        cameraPositionState = cameraPositionState,
        allCloudUsers = allCloudUsers,
        hasLocationPermission = hasLocationPermission,
        onUserUpdate = onUserUpdate
    )
}

@SuppressLint("MissingPermission")
@Composable
fun BeaconMapScreen(
    currentUser: User,
    cameraPositionState: CameraPositionState,
    allCloudUsers: List<User>,
    hasLocationPermission: Boolean,
    onUserUpdate: (User) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val sharedPreferences = remember { context.getSharedPreferences("HeyTherePrefs", Context.MODE_PRIVATE) }

    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }
    var currentTick by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            currentTick = System.currentTimeMillis()
        }
    }

    DisposableEffect(currentUser.privacySettings.usePreciseLocation, hasLocationPermission, currentUser.privacySettings.activeBlackoutZones) {
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { location ->
                    val exactLatLng = LatLng(location.latitude, location.longitude)

                    sharedPreferences.edit()
                        .putFloat("last_lat", exactLatLng.latitude.toFloat())
                        .putFloat("last_lng", exactLatLng.longitude.toFloat())
                        .apply()

                    var isInsideBlackoutZone = false
                    for (zone in currentUser.privacySettings.activeBlackoutZones) {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                            exactLatLng.latitude, exactLatLng.longitude,
                            zone.latitude, zone.longitude,
                            results
                        )
                        if (results[0] <= zone.radiusMeters) {
                            isInsideBlackoutZone = true
                            break
                        }
                    }

                    val broadcastLat: Double
                    val broadcastLng: Double

                    if (isInsideBlackoutZone) {
                        broadcastLat = 0.0
                        broadcastLng = 0.0
                    } else {
                        val noiseLatLng = LocationUtils.applyLocationNoise(context, exactLatLng)
                        broadcastLat = if (currentUser.privacySettings.usePreciseLocation) exactLatLng.latitude else noiseLatLng.latitude
                        broadcastLng = if (currentUser.privacySettings.usePreciseLocation) exactLatLng.longitude else noiseLatLng.longitude
                    }

                    if (currentUser.locationData.publicLatitude != broadcastLat || currentUser.locationData.publicLongitude != broadcastLng) {
                        val newLocationData = LocationData(
                            publicLatitude = broadcastLat,
                            publicLongitude = broadcastLng,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )

                        val updatedUser = currentUser.copy(locationData = newLocationData)
                        onUserUpdate(updatedUser)
                    }
                }
            }
        }

        if (hasLocationPermission) {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            ).build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper()
            )
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    val mapProperties = MapProperties(
        isMyLocationEnabled = hasLocationPermission,
        mapType = currentMapType
    )

    val uiSettings = MapUiSettings(
        myLocationButtonEnabled = true,
        zoomControlsEnabled = false,
        mapToolbarEnabled = true
    )

    val filteredUsers = remember(
        currentUser.subscribedTags,
        allCloudUsers,
        currentUser.privacySettings.isGlobalLocationOn,
        hasLocationPermission,
        currentTick
    ) {
        if (!hasLocationPermission || !currentUser.privacySettings.isGlobalLocationOn) {
            emptyList()
        } else {
            val tenMinutesInMillis = 10 * 60 * 1000L

            val activeUsers = allCloudUsers.filter {
                it.userId != currentUser.userId &&
                        it.privacySettings.isGlobalLocationOn &&
                        (currentTick - it.locationData.lastUpdatedTimestamp) <= tenMinutesInMillis
            }

            if (currentUser.subscribedTags.isEmpty()) {
                activeUsers
            } else {
                activeUsers.filter { cloudUser ->
                    cloudUser.subscribedTags.any { tag -> currentUser.subscribedTags.contains(tag) }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            filteredUsers.forEach { user ->
                if (user.locationData.publicLatitude != 0.0 && user.locationData.publicLongitude != 0.0) {
                    val position = LatLng(user.locationData.publicLatitude, user.locationData.publicLongitude)

                    MarkerInfoWindowContent(
                        state = MarkerState(position = position)
                    ) {
                        val sharedTags = user.subscribedTags.intersect(currentUser.subscribedTags.toSet()).take(3)
                        val tagsText = if (sharedTags.isNotEmpty()) {
                            sharedTags.joinToString(", ")
                        } else {
                            "No shared interests"
                        }

                        val diffMs = System.currentTimeMillis() - user.locationData.lastUpdatedTimestamp
                        val diffMins = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMs)
                        val timeString = if (diffMins < 1) "Just now" else "$diffMins mins ago"

                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                // Hardcoding colors to ensure visibility against the forced white background
                                Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = androidx.compose.ui.graphics.Color.Black)
                                Text(text = tagsText, fontSize = 14.sp, color = androidx.compose.ui.graphics.Color.Black)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = timeString, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
                            }
                        }
                    }
                }
            }

            if (currentUser.privacySettings.isGlobalLocationOn &&
                !currentUser.privacySettings.usePreciseLocation &&
                currentUser.locationData.publicLatitude != 0.0 &&
                currentUser.locationData.publicLongitude != 0.0) {
                Marker(
                    state = MarkerState(
                        position = LatLng(currentUser.locationData.publicLatitude, currentUser.locationData.publicLongitude)
                    ),
                    title = "You (Broadcasted)",
                    snippet = "This is where others see you",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                currentMapType = if (currentMapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Layers, contentDescription = "Toggle Map Type")
        }
    }
}