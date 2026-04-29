package com.yolojj333.heythere
import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.yolojj333.heythere.models.LocationData
import com.yolojj333.heythere.models.User
import com.yolojj333.heythere.ui.AuthScreen
import com.yolojj333.heythere.ui.ProfileScreen
import com.yolojj333.heythere.ui.SettingsScreen
import com.yolojj333.heythere.ui.theme.BeaconTheme
import com.yolojj333.heythere.utils.FirebaseManager
import com.yolojj333.heythere.utils.LocationUtils

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

    var currentRoute by remember { mutableStateOf("map") }
    var currentUser by remember { mutableStateOf(User()) }
    var isProfileLoaded by remember { mutableStateOf(false) }

    // NEW: State to hold all the real users fetched from Firestore
    var allCloudUsers by remember { mutableStateOf<List<User>>(emptyList()) }

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

    // NEW: Start listening to the live Firestore database for all users
    LaunchedEffect(Unit) {
        FirebaseManager.listenToAllUsers(
            onResult = { users -> allCloudUsers = users },
            onFailure = { /* Silent fail for now */ }
        )
    }

    val defaultLocation = LatLng(34.0522, -118.2437)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    if (!isProfileLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
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

                "profile" -> ProfileScreen(
                    user = currentUser,
                    onUserChange = { updatedUser -> currentUser = updatedUser }
                )

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
        // We consider permission granted if they gave at least coarse location
        hasLocationPermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    // By passing the setting into LaunchedEffect, it will re-trigger the permission
    // request if the user toggles the switch in their Settings tab.
    LaunchedEffect(currentUser.privacySettings.usePreciseLocation) {
        val permissionsToRequest = if (currentUser.privacySettings.usePreciseLocation) {
            // Ask for both (required by Android 12+ to show the precise/approximate toggle)
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            // Only ask for approximate location
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    if (hasLocationPermission) {
        BeaconMapScreen(currentUser, cameraPositionState, allCloudUsers, onUserUpdate)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Location permission is required to find nearby connections.")
        }
    }
}

@SuppressLint("MissingPermission") // Suppressed because we already checked permissions in the parent screen
@Composable
fun BeaconMapScreen(
    currentUser: User,
    cameraPositionState: CameraPositionState,
    allCloudUsers: List<User>,
    onUserUpdate: (User) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Fetch actual GPS location when the map loads and upload it
    LaunchedEffect(Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val exactLatLng = LatLng(location.latitude, location.longitude)
                val noiseLatLng = LocationUtils.applyLocationNoise(exactLatLng)

                val newLocationData = LocationData(
                    preciseLatitude = exactLatLng.latitude,
                    preciseLongitude = exactLatLng.longitude,
                    noiseLatitude = noiseLatLng.latitude,
                    noiseLongitude = noiseLatLng.longitude,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )

                val updatedUser = currentUser.copy(locationData = newLocationData)
                onUserUpdate(updatedUser)

                // Snap the map camera to the user's real location
                cameraPositionState.position = CameraPosition.fromLatLngZoom(exactLatLng, 14f)
            }
        }
    }

    val mapProperties = MapProperties(
        isMyLocationEnabled = currentUser.privacySettings.isGlobalLocationOn
    )

    val uiSettings = MapUiSettings(
        myLocationButtonEnabled = true,
        zoomControlsEnabled = false
    )

    // Filter cloud users (exclude ourselves, and apply tag logic)
    val filteredUsers = remember(currentUser.subscribedTags, allCloudUsers) {
        // Step 1: Remove ourselves from the map
        val otherUsers = allCloudUsers.filter { it.userId != currentUser.userId && it.privacySettings.isGlobalLocationOn }

        // Step 2: Filter by shared tags
        if (currentUser.subscribedTags.isEmpty()) {
            otherUsers
        } else {
            otherUsers.filter { cloudUser ->
                cloudUser.subscribedTags.any { tag -> currentUser.subscribedTags.contains(tag) }
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        filteredUsers.forEach { user ->
            val displayLat = if (user.privacySettings.usePreciseLocation) user.locationData.preciseLatitude else user.locationData.noiseLatitude
            val displayLng = if (user.privacySettings.usePreciseLocation) user.locationData.preciseLongitude else user.locationData.noiseLongitude

            // Only draw a pin if they actually have a valid location
            if (displayLat != 0.0 && displayLng != 0.0) {
                val position = LatLng(displayLat, displayLng)
                val tagsString = user.subscribedTags.joinToString(", ")
                val locationType = if (user.privacySettings.usePreciseLocation) "(Precise)" else "(Approximate)"

                com.google.maps.android.compose.Marker(
                    state = com.google.maps.android.compose.MarkerState(position = position),
                    title = "${user.displayName} $locationType",
                    snippet = "Likes: $tagsString"
                )
            }
        }
    }
}