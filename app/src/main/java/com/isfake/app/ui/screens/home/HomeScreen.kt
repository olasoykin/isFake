package com.isfake.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isfake.app.data.model.FakeLocation
import com.isfake.app.R
import com.isfake.app.service.MockLocationService
import com.isfake.app.ui.components.BatteryOptimizationDialog
import com.isfake.app.ui.components.OsmMapView
import com.isfake.app.ui.components.SettingsDialog
import com.isfake.app.util.BatteryOptimizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val updateIntervalMs by viewModel.updateIntervalMs.collectAsStateWithLifecycle()
    val accuracyMeters by viewModel.accuracyMeters.collectAsStateWithLifecycle()
    val altitudeMeters by viewModel.altitudeMeters.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    val startMockSimulation = {
        if (viewModel.startService()) {
            MockLocationService.start(
                context = context,
                lat = uiState.selectedLat!!,
                lng = uiState.selectedLng!!,
                altitude = altitudeMeters.toDouble(),
                accuracy = accuracyMeters,
                intervalMs = updateIntervalMs
            )
        }
    }

    // Permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled */ }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    LaunchedEffect(Unit) {
        val locationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!locationGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Dialogs
    if (showBatteryOptimizationDialog) {
        BatteryOptimizationDialog(
            onDisableOptimization = {
                showBatteryOptimizationDialog = false
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                startMockSimulation()
            },
            onContinueAnyway = {
                showBatteryOptimizationDialog = false
                startMockSimulation()
            },
            onDismiss = {
                showBatteryOptimizationDialog = false
            }
        )
    }

    if (uiState.showOnboarding) {
        OnboardingDialog(onDismiss = { viewModel.dismissOnboarding() })
    }

    if (uiState.showSavePresetDialog) {
        SavePresetDialog(
            nameValue = uiState.presetNameInput,
            onNameChange = { viewModel.onPresetNameChanged(it) },
            onConfirm = { viewModel.saveCurrentAsPreset() },
            onDismiss = { viewModel.dismissSavePresetDialog() }
        )
    }

    if (uiState.showSettingsDialog) {
        SettingsDialog(
            initialIntervalMs = updateIntervalMs,
            initialAccuracy = accuracyMeters,
            initialAltitude = altitudeMeters,
            onSave = { interval, acc, alt ->
                viewModel.updateSettings(interval, acc, alt)
                if (uiState.isServiceActive && uiState.selectedLat != null && uiState.selectedLng != null) {
                    MockLocationService.start(
                        context = context,
                        lat = uiState.selectedLat!!,
                        lng = uiState.selectedLng!!,
                        altitude = alt.toDouble(),
                        accuracy = acc,
                        intervalMs = interval
                    )
                }
            },
            onDismiss = { viewModel.dismissSettings() }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "isFake",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        actions = {
                            IconButton(onClick = { viewModel.showSettings() }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.content_settings)
                                )
                            }
                            IconButton(onClick = { viewModel.showOnboarding() }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = stringResource(R.string.content_developer_instructions)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )

                    // TOP ACTION CARD RIGIDLY ANCHORED IN TOPBAR
                    TopActionCard(
                        isServiceActive = uiState.isServiceActive,
                        selectedLat = uiState.selectedLat,
                        selectedLng = uiState.selectedLng,
                        onStartService = {
                            if (uiState.selectedLat == null || uiState.selectedLng == null) {
                                viewModel.startService()
                            } else if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                                showBatteryOptimizationDialog = true
                            } else {
                                startMockSimulation()
                            }
                        },
                        onStopService = {
                            viewModel.stopService()
                            MockLocationService.stop(context)
                        },
                        onSaveBookmark = { viewModel.showSavePresetDialog() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.selectedTabIndex == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) }
                )
                NavigationBarItem(
                    selected = uiState.selectedTabIndex == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    icon = { Icon(Icons.Default.EditLocation, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_coords)) }
                )
                NavigationBarItem(
                    selected = uiState.selectedTabIndex == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_presets)) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTabIndex) {
                0 -> {
                    // MAP TAB
                    OsmMapView(
                        modifier = Modifier.fillMaxSize(),
                        startLat = uiState.selectedLat ?: -34.6037,
                        startLon = uiState.selectedLng ?: -58.3816,
                        selectedLat = uiState.selectedLat,
                        selectedLon = uiState.selectedLng,
                        centerTrigger = uiState.centerTrigger,
                        onLocationSelected = { lat, lng ->
                            viewModel.onMapLocationSelected(lat, lng)
                        }
                    )

                    // RECENTER FAB ON MAP
                    if (uiState.selectedLat != null) {
                        FloatingActionButton(
                            onClick = { viewModel.centerLocation() },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = stringResource(R.string.content_center_location),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                1 -> {
                        // COORDINATES TAB
                        CoordinatesInputScreen(
                            latInput = uiState.latInput,
                            lngInput = uiState.lngInput,
                            onLatChanged = { viewModel.onLatInputChanged(it) },
                            onLngChanged = { viewModel.onLngInputChanged(it) },
                            onApply = { viewModel.onTabSelected(0) }
                        )
                    }

                    2 -> {
                        // PRESETS TAB
                        PresetsListScreen(
                            presets = presets,
                            onUsePreset = { preset ->
                                viewModel.usePreset(preset)
                                viewModel.onTabSelected(0)
                            },
                            onDeletePreset = { preset ->
                                viewModel.deletePreset(preset)
                            }
                        )
                    }
                }
            }
        }
    }

/**
 * Top control card showing location state and the primary Start / Stop button
 */
@Composable
private fun TopActionCard(
    isServiceActive: Boolean,
    selectedLat: Double?,
    selectedLng: Double?,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onSaveBookmark: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isServiceActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isServiceActive) R.string.active_status else R.string.status_location_ready),
                    style = MaterialTheme.typography.titleSmall
                )
                if (selectedLat != null && selectedLng != null) {
                    Text(
                        text = "%.5f, %.5f".format(selectedLat, selectedLng),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.tap_map_set_point),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedLat != null && selectedLng != null && !isServiceActive) {
                IconButton(
                    onClick = onSaveBookmark,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = stringResource(R.string.save_preset),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = if (isServiceActive) onStopService else onStartService,
                enabled = selectedLat != null || isServiceActive,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceActive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isServiceActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(if (isServiceActive) R.string.action_stop else R.string.action_simulate),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Standard Coordinates Input Tab
 */
@Composable
private fun CoordinatesInputScreen(
    latInput: String,
    lngInput: String,
    onLatChanged: (String) -> Unit,
    onLngChanged: (String) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.manual_coordinates_title),
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = latInput,
            onValueChange = onLatChanged,
            label = { Text(stringResource(R.string.lat_hint)) },
            placeholder = { Text("-34.603700") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lngInput,
            onValueChange = onLngChanged,
            label = { Text(stringResource(R.string.lng_hint)) },
            placeholder = { Text("-58.381600") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.view_on_map))
        }
    }
}

/**
 * Standard Presets Tab
 */
@Composable
private fun PresetsListScreen(
    presets: List<FakeLocation>,
    onUsePreset: (FakeLocation) -> Unit,
    onDeletePreset: (FakeLocation) -> Unit
) {
    if (presets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.presets_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.preset_coordinates, preset.latFormatted(), preset.lngFormatted()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = { onUsePreset(preset) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.action_load))
                            }
                            IconButton(onClick = { onDeletePreset(preset) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavePresetDialog(
    nameValue: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_preset)) },
        text = {
            OutlinedTextField(
                value = nameValue,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.preset_name_hint)) },
                placeholder = { Text(stringResource(R.string.preset_example)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = nameValue.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.onboarding_dev_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_dev_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(stringResource(R.string.onboarding_dev_step_1), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.onboarding_dev_step_2), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.onboarding_dev_step_3), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.onboarding_dev_step_4), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.onboarding_dev_step_5), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.onboarding_dev_step_6), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.onboarding_got_it))
            }
        }
    )
}
