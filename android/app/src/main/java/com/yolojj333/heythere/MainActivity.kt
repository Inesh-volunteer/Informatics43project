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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
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

    // Dynamic initial location based on last known data
    val defaultLat = currentUser.locationData.publicLatitude
    val defaultLng = currentUser.locationData.publicLongitude
    val defaultLocation = LatLng(defaultLat, defaultLng)

    // Zoom out to see the globe if they are at 0,0, otherwise zoom in on their last known location
    val initialZoom = if (defaultLat == 0.0 && defaultLng == 0.0) 2f else 12f

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

    // Always show the map, passing down whether they granted permission or not
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

    var myRealLocation by remember { mutableStateOf<LatLng?>(null) }

    // NEW: A ticking clock that forces the map to refresh every 60 seconds
    var currentTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            currentTick = System.currentTimeMillis()
        }
    }

    LaunchedEffect(currentUser.privacySettings.usePreciseLocation, hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val exactLatLng = LatLng(location.latitude, location.longitude)
                    myRealLocation = exactLatLng

                    val noiseLatLng = LocationUtils.applyLocationNoise(exactLatLng)

                    val broadcastLat = if (currentUser.privacySettings.usePreciseLocation) exactLatLng.latitude else noiseLatLng.latitude
                    val broadcastLng = if (currentUser.privacySettings.usePreciseLocation) exactLatLng.longitude else noiseLatLng.longitude

                    val newLocationData = LocationData(
                        publicLatitude = broadcastLat,
                        publicLongitude = broadcastLng,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )

                    val updatedUser = currentUser.copy(locationData = newLocationData)
                    onUserUpdate(updatedUser)

                    cameraPositionState.position = CameraPosition.fromLatLngZoom(exactLatLng, 14f)
                }
            }
        }
    }

    val mapProperties = MapProperties(
        isMyLocationEnabled = false
    )

    val uiSettings = MapUiSettings(
        myLocationButtonEnabled = true,
        zoomControlsEnabled = false
    )

    // THE RULE: Filter out users without permissions, without shared tags, AND who have been offline for >10 minutes.
    val filteredUsers = remember(
        currentUser.subscribedTags,
        allCloudUsers,
        currentUser.privacySettings.isGlobalLocationOn,
        hasLocationPermission,
        currentTick // Passing the tick here forces this block to re-run every minute
    ) {
        if (!hasLocationPermission || !currentUser.privacySettings.isGlobalLocationOn) {
            emptyList()
        } else {
            val tenMinutesInMillis = 10 * 60 * 1000L

            // Filter out ourselves, hidden users, AND stale locations
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

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        filteredUsers.forEach { user ->
            if (user.locationData.publicLatitude != 0.0 && user.locationData.publicLongitude != 0.0) {
                val position = LatLng(user.locationData.publicLatitude, user.locationData.publicLongitude)
                val tagsString = user.subscribedTags.joinToString(", ")

                Marker(
                    state = MarkerState(position = position),
                    title = user.displayName,
                    snippet = "Likes: $tagsString"
                )
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

        myRealLocation?.let { realLatLng ->
            Marker(
                state = MarkerState(position = realLatLng),
                title = "You (Actual Device)",
                snippet = "Your true GPS location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }
    }
}