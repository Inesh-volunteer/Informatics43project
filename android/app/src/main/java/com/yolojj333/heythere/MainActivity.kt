package com.yolojj333.heythere

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
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

                                if (fileSize > 5242880L) { // 5MB limit
                                    isSavingProfile = false
                                    Toast.makeText(context, "Image is too large. Limit is 5MB.", Toast.LENGTH_LONG).show()
                                    return@ProfileScreen
                                }

                                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()

                                FirebaseManager.uploadProfileImage(
                                    uri = uri,
                                    onSuccess = { publicDownloadUrl: String ->
                                        // Inject the public URL into the object before saving to Firestore
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
                                        Log.e("UploadError", "Firebase Storage failed: $errorMsg")
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else if (imageUrl.isNullOrEmpty()) {
                                // User removed the photo. Delete from Storage, then save the empty array to Firestore.
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
                                // Image is already an HTTPS URL, just save text changes
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var myPhysicalLocation by remember { mutableStateOf<LatLng?>(null) }

    var searchRadiusKm by remember { mutableFloatStateOf(10f) }
    var sortByDistance by remember { mutableStateOf(true) }

    var selectedUserId by remember { mutableStateOf<String?>(null) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

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
                    myPhysicalLocation = exactLatLng

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
                        onUserUpdate(currentUser.copy(locationData = newLocationData))
                    }
                }
            }
        }

        if (hasLocationPermission) {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000L
            ).build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper())
        }
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    val globalActiveUsers = remember(currentUser.subscribedTags, allCloudUsers, currentUser.privacySettings.isGlobalLocationOn, hasLocationPermission, currentTick) {
        if (!hasLocationPermission || !currentUser.privacySettings.isGlobalLocationOn) emptyList()
        else {
            val tenMinutesInMillis = 10 * 60 * 1000L
            val active = allCloudUsers.filter {
                it.userId != currentUser.userId &&
                        it.privacySettings.isGlobalLocationOn &&
                        (currentTick - it.locationData.lastUpdatedTimestamp) <= tenMinutesInMillis
            }
            if (currentUser.subscribedTags.isEmpty()) active
            else active.filter { cloudUser -> cloudUser.subscribedTags.any { tag -> currentUser.subscribedTags.contains(tag) } }
        }
    }

    val listUsers = remember(globalActiveUsers, myPhysicalLocation, searchRadiusKm, sortByDistance, currentUser.subscribedTags) {
        if (myPhysicalLocation == null) return@remember emptyList<Pair<User, Float>>()

        globalActiveUsers.mapNotNull { targetUser ->
            if (targetUser.locationData.publicLatitude == 0.0) return@mapNotNull null

            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                myPhysicalLocation!!.latitude, myPhysicalLocation!!.longitude,
                targetUser.locationData.publicLatitude, targetUser.locationData.publicLongitude,
                results
            )
            val distanceMeters = results[0]
            if (distanceMeters <= searchRadiusKm * 1000) Pair(targetUser, distanceMeters) else null
        }.sortedWith { a, b ->
            if (sortByDistance) {
                a.second.compareTo(b.second)
            } else {
                val aShared = a.first.subscribedTags.intersect(currentUser.subscribedTags.toSet()).size
                val bShared = b.first.subscribedTags.intersect(currentUser.subscribedTags.toSet()).size
                if (aShared != bShared) bShared.compareTo(aShared)
                else a.second.compareTo(b.second)
            }
        }
    }

    val mapProperties = MapProperties(isMyLocationEnabled = hasLocationPermission, mapType = currentMapType)
    val uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false, mapToolbarEnabled = true)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 100.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(text = "Nearby Users", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Radius: ${searchRadiusKm.toInt()}km", fontSize = 14.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = searchRadiusKm,
                        onValueChange = { searchRadiusKm = it },
                        valueRange = 1f..50f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Sort by:", fontSize = 14.sp, modifier = Modifier.width(90.dp))
                    FilterChip(
                        selected = sortByDistance,
                        onClick = { sortByDistance = true },
                        label = { Text("Distance") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !sortByDistance,
                        onClick = { sortByDistance = false },
                        label = { Text("Interests") }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (listUsers.isEmpty()) {
                    Text("No users found nearby.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        items(listUsers.size) { index ->
                            val (listUser, distance) = listUsers[index]
                            val sharedTags = listUser.subscribedTags.intersect(currentUser.subscribedTags.toSet())
                            val distanceString = if (distance < 1000) "${distance.toInt()} m" else String.format("%.1f km", distance / 1000)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(50.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (listUser.profileImageUrls.isNotEmpty() && listUser.profileImageUrls.first().isNotBlank()) {
                                        coil.compose.AsyncImage(
                                            model = listUser.profileImageUrls.first(),
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = listUser.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = if (sharedTags.isNotEmpty()) sharedTags.take(3).joinToString(", ") else "No shared interests",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(text = distanceString, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapClick = { selectedUserId = null }
            ) {
                globalActiveUsers.forEach { user ->
                    if (user.locationData.publicLatitude != 0.0 && user.locationData.publicLongitude != 0.0) {

                        key(user.userId) {
                            val position = LatLng(user.locationData.publicLatitude, user.locationData.publicLongitude)
                            val markerState = rememberMarkerState(position = position)
                            markerState.position = position

                            var userImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                            var mapPinIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

                            val url = user.profileImageUrls.firstOrNull()

                            // NEW: Added mapPinSize as a key so it redraws when the setting changes
                            LaunchedEffect(url, currentUser.privacySettings.mapPinSize) {
                                if (!url.isNullOrBlank()) {
                                    val request = coil.request.ImageRequest.Builder(context)
                                        .data(url)
                                        .allowHardware(false)
                                        .build()

                                    val result = context.imageLoader.execute(request)
                                    if (result is coil.request.SuccessResult) {
                                        val drawable = result.drawable
                                        if (drawable is BitmapDrawable) {
                                            val rawBitmap = drawable.bitmap

                                            userImage = rawBitmap.asImageBitmap()

                                            // Pass the dynamic setting directly to the graphics utility
                                            mapPinIcon = createCircularMapPin(rawBitmap, currentUser.privacySettings.mapPinSize)

                                            if (selectedUserId == user.userId) {
                                                markerState.showInfoWindow()
                                            }
                                        }
                                    }
                                } else {
                                    userImage = null
                                    mapPinIcon = null
                                }
                            }

                            MarkerInfoWindowContent(
                                state = markerState,
                                icon = mapPinIcon,
                                onClick = {
                                    selectedUserId = user.userId
                                    false
                                },
                                onInfoWindowClose = {
                                    if (selectedUserId == user.userId) selectedUserId = null
                                }
                            ) {
                                val sharedTags = user.subscribedTags.intersect(currentUser.subscribedTags.toSet()).take(3)
                                val tagsText = if (sharedTags.isNotEmpty()) sharedTags.joinToString(", ") else "No shared interests"
                                val diffMs = System.currentTimeMillis() - user.locationData.lastUpdatedTimestamp
                                val diffMins = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMs)
                                val timeString = if (diffMins < 1) "Just now" else "$diffMins mins ago"

                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (userImage != null) {
                                            Image(
                                                bitmap = userImage!!,
                                                contentDescription = "Profile",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = androidx.compose.ui.graphics.Color.Black)
                                        Text(text = tagsText, fontSize = 14.sp, color = androidx.compose.ui.graphics.Color.Black)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = timeString, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
                                    }
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
                        state = MarkerState(position = LatLng(currentUser.locationData.publicLatitude, currentUser.locationData.publicLongitude)),
                        title = "You (Broadcasted)",
                        snippet = "This is where others see you",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                }
            }

            FloatingActionButton(
                onClick = { currentMapType = if (currentMapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Layers, contentDescription = "Toggle Map Type")
            }
        }
    }
}

/**
 * Takes a raw downloaded Bitmap, cuts it into a circle, adds a white border,
 * and converts it into a Google Maps BitmapDescriptor dynamically.
 */
fun createCircularMapPin(bitmap: Bitmap, size: Int): BitmapDescriptor {
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, false)

    val paint = Paint().apply {
        isAntiAlias = true
    }
    val rect = Rect(0, 0, size, size)
    val rectF = RectF(rect)

    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawRoundRect(rectF, size / 2f, size / 2f, paint)

    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaledBitmap, rect, rect, paint)

    paint.xfermode = null
    paint.style = Paint.Style.STROKE
    paint.color = android.graphics.Color.WHITE
    // Dynamically scale the border width to 5% of the total pin size
    paint.strokeWidth = size / 20f
    canvas.drawRoundRect(rectF, size / 2f, size / 2f, paint)

    return BitmapDescriptorFactory.fromBitmap(output)
}