package com.unibo.petly.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.unibo.petly.ui.PetlyRoute
import com.unibo.petly.ui.composables.BottomBar
import com.unibo.petly.ui.composables.DefaultCard
import com.unibo.petly.ui.composables.ImageDisplay
import com.unibo.petly.utils.LocationService
import com.unibo.petly.utils.PermissionStatus
import com.unibo.petly.utils.StartMonitoringResult
import com.unibo.petly.utils.rememberPermission
import com.unibo.petly.viewmodel.UserViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU) //for requestLocation
fun UserProfileScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    locationService: LocationService
) {
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val user = userViewModel.user!!
    val badgesReceived =
        userViewModel.getReceivedBadgesByUser(user.userId).collectAsState(initial = listOf()).value

    // Profile picture
    var photoUri: Uri? by remember {
        mutableStateOf(
            if (userViewModel.user!!.profileImg?.isNotEmpty() == true) {
                Uri.parse(user.profileImg)
            } else null
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) {
        uri -> photoUri = uri
        if (uri != null) {
            userViewModel.setProfilePicture(uri)
        }
    }

    // Location
    var address by remember { mutableStateOf("Click to show your location") }
    var showLocationDisabledAlert by remember { mutableStateOf(false) }
    var showLocationDeniedAlert by remember { mutableStateOf(false) }
    var showLocationPermanentlyDeniedSnackbar by remember { mutableStateOf(false) }

    fun getLocationName(latitude: Double, longitude: Double, context: Context) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) {
                    val city = addresses[0].locality ?: "City not found"
                    val country = addresses[0].countryName ?: "State not found"
                    address = "$city, $country"
                }
            }
        } catch(exception: Exception) {
            address = "Unable to determine location right now"
        }
    }

    val locationPermission = rememberPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) { status ->
        when (status) {
            PermissionStatus.Granted -> {
                val res = locationService.requestCurrentLocation()
                showLocationDisabledAlert = res == StartMonitoringResult.GPSDisabled
            }
            PermissionStatus.Denied ->
                showLocationDeniedAlert = true
            PermissionStatus.PermanentlyDenied ->
                showLocationPermanentlyDeniedSnackbar = true
            PermissionStatus.Unknown -> {}
        }
    }

    fun requestLocation() {
        if (locationPermission.status.isGranted) {
            val res = locationService.requestCurrentLocation()
            showLocationDisabledAlert = res == StartMonitoringResult.GPSDisabled
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(locationService.coordinates) {
        locationService.coordinates?.let { coordinates ->
            getLocationName(coordinates.latitude, coordinates.longitude, ctx)
        }
    }

    // Screen ui
    Scaffold(
        bottomBar = {
            BottomBar(
                navController = navController,
                currentRoute = PetlyRoute.UserProfile
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp, 16.dp),
            modifier = Modifier.padding(innerPadding)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.size(160.dp)
                    ) {
                        ImageDisplay(
                            uri = photoUri,
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        FilledTonalIconButton(
                            onClick = {
                                launcher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, "Add a photo")
                        }
                    }
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                    )
                    Text(
                        text = "Inscription date: " + user.inscriptionDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            requestLocation()
                        }
                    )
                    Text(
                        text = "Your badges:",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 25.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Start)
                            .padding(top = 20.dp)
                    )
                }
            }
            items(badgesReceived, key = { it.name }){ badge ->
                DefaultCard(
                    title = badge.name,
                    body = badge.description,
                    painter = painterResource(id = badge.imageResourceId),
                )
            }
        }
    }

    if (showLocationDisabledAlert) {
        AlertDialog(
            title = { Text("Location disabled") },
            text = { Text("Location must be enabled to get your current location in the app.") },
            confirmButton = {
                TextButton(onClick = {
                    locationService.openLocationSettings()
                    showLocationDisabledAlert = false
                }) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDisabledAlert = false }) {
                    Text("Dismiss")
                }
            },
            onDismissRequest = { showLocationDisabledAlert = false }
        )
    }

    if (showLocationDeniedAlert) {
        AlertDialog(
            title = { Text("Location permission denied") },
            text = { Text("Location permission is required to get your current location in the app.") },
            confirmButton = {
                TextButton(onClick = {
                    locationPermission.launchPermissionRequest()
                    showLocationDeniedAlert = false
                }) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDeniedAlert = false }) {
                    Text("Dismiss")
                }
            },
            onDismissRequest = { showLocationDeniedAlert = false }
        )
    }

    if (showLocationPermanentlyDeniedSnackbar) {
        LaunchedEffect(snackbarHostState) {
            val res = snackbarHostState.showSnackbar(
                "Location permission is required.",
                "Go to Settings",
                duration = SnackbarDuration.Long
            )
            if (res == SnackbarResult.ActionPerformed) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", ctx.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(ctx.packageManager) != null) {
                    ctx.startActivity(intent)
                }
            }
            showLocationPermanentlyDeniedSnackbar = false
        }
    }
}