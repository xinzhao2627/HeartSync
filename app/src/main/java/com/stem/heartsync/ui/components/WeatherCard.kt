package com.stem.heartsync.ui.components

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.CityPreset
import com.stem.heartsync.data.model.LocationStatus
import com.stem.heartsync.data.model.WeatherData
import com.stem.heartsync.ui.theme.*
import com.stem.heartsync.weather.WeatherManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherCard(
    weatherManager: WeatherManager,
    audioManager: HeartSyncAudioManager,
    classModeEnabled: Boolean,
    sfxEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weather by weatherManager.weather.collectAsState()
    val locationStatus by weatherManager.locationStatus.collectAsState()
    val isRefreshing by weatherManager.isRefreshing.collectAsState()
    val selectedCity by weatherManager.selectedCity.collectAsState()

    var showCityDialog by remember { mutableStateOf(false) }

    // Launcher for requesting location permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, classModeEnabled, sfxEnabled)
            Toast.makeText(context, "Location permission granted! Fetching weather... 🌤️", Toast.LENGTH_SHORT).show()
            weatherManager.checkLocationStatus()
            weatherManager.refreshWeather(forceNetwork = true)
        } else {
            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
            Toast.makeText(context, "Location permission denied. Showing default city.", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SurgicalWhite,
        border = BorderStroke(1.dp, DeepAnchorNavy100),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Badge, Title, City, Refresh Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SoftSkyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weather?.conditionEmoji ?: "⛅",
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Weather",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepAnchorNavy900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = weather?.cityName ?: (selectedCity?.name ?: "Manila, PH"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepAnchorNavy600,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Refresh Button
                IconButton(
                    onClick = {
                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                        weatherManager.checkLocationStatus()
                        weatherManager.refreshWeather(forceNetwork = true)
                        Toast.makeText(context, "Refreshing weather data... 🔄", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeepAnchorNavy100.copy(alpha = 0.5f))
                        .semantics { contentDescription = "Refresh weather forecast" }
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Weather",
                            tint = DeepAnchorNavy900,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Weather Stats Display
            if (weather != null) {
                val data = weather!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "${data.temperature.toInt()}°C",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepAnchorNavy900,
                            lineHeight = 44.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data.conditionDescription,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepAnchorNavy600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Humidity ${data.humidity}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = DeepAnchorNavy600,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Wind ${data.windSpeed.toInt()} km/h",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = DeepAnchorNavy600,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Offline Notice if Cached
                if (data.isCached) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val formattedTime = SimpleDateFormat("h:mm a • MMM d", Locale.getDefault()).format(Date(data.timestamp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = DeepAnchorNavy600.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedCity != null) "Cached at $formattedTime (Preset)" else "Cached at $formattedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepAnchorNavy600.copy(alpha = 0.7f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ACCESSIBILITY ACTION BUTTON WHEN LOCATION IS OFF OR PERMISSION DENIED
            val isLocationOff = locationStatus != LocationStatus.AVAILABLE
            if (isLocationOff) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoftSkyCard,
                    border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (locationStatus == LocationStatus.GPS_OFF) {
                                "Location services are turned off on your device. Double tap to open system settings and turn on location."
                            } else {
                                "Location permission is not granted. Double tap to grant permission."
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = CardiacCrimson600,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (locationStatus == LocationStatus.GPS_OFF) "Device Location is Turned Off" else "Location Access Needed",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (locationStatus == LocationStatus.GPS_OFF) {
                                        "Turn on location to detect local campus weather, or select your city manually below."
                                    } else {
                                        "Grant location permission to detect your current campus weather automatically."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Primary Accessibility Button: Turn On Location / Grant Permission
                            Button(
                                onClick = {
                                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                                    if (locationStatus == LocationStatus.PERMISSION_DENIED) {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    } else {
                                        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                        context.startActivity(settingsIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricBlue,
                                    contentColor = SurgicalWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .semantics {
                                        contentDescription = if (locationStatus == LocationStatus.PERMISSION_DENIED) {
                                            "Grant location permission for weather detection"
                                        } else {
                                            "Open system settings to turn on device location"
                                        }
                                    },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (locationStatus == LocationStatus.PERMISSION_DENIED) "Grant Access" else "Turn On GPS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Secondary Accessible City Selector Button
                            OutlinedButton(
                                onClick = {
                                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                                    showCityDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = DeepAnchorNavy900
                                ),
                                border = BorderStroke(1.dp, DeepAnchorNavy100),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .semantics { contentDescription = "Select city manually from list" },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Select City",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            } else if (selectedCity != null) {
                // If location is available but student had manually selected a city, offer switch back to GPS
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Viewing: ${selectedCity?.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepAnchorNavy600,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                            weatherManager.clearManualCity()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Use GPS 📍",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // Accessible City Selector Dialog
    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            containerColor = SurgicalWhite,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Select Campus Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose your city to fetch weather without enabling GPS location services:",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepAnchorNavy600
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(weatherManager.cityPresets) { city ->
                            val isSelected = selectedCity?.name == city.name
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SoftBlushCard else PaperBackground,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CardiacCrimson600 else DeepAnchorNavy100
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, classModeEnabled, sfxEnabled)
                                        weatherManager.selectPresetCity(city)
                                        showCityDialog = false
                                        Toast.makeText(context, "Location set to ${city.name} 📍", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CardiacCrimson900 else DeepAnchorNavy900
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CardiacCrimson600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCityDialog = false }
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = DeepAnchorNavy900)
                }
            }
        )
    }
}
