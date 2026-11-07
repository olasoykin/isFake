package com.isfake.app.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.isfake.app.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    startLat: Double = -34.6037,
    startLon: Double = -58.3816,
    selectedLat: Double? = null,
    selectedLon: Double? = null,
    centerTrigger: Int = 0,
    onLocationSelected: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Initialize OSMDroid configuration with application package name
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        val sharedPrefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        load(context, sharedPrefs)
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(startLat, startLon))
            isTilesScaledToDpi = true

            // Tap listener to pick location
            overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        onLocationSelected(p.latitude, p.longitude)
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean {
                        onLocationSelected(p.latitude, p.longitude)
                        return true
                    }
                })
            )
        }
    }

    // Center map on marker when trigger increments
    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0 && selectedLat != null && selectedLon != null) {
            mapView.controller.animateTo(GeoPoint(selectedLat, selectedLon))
        }
    }

    // Update pin marker on map
    DisposableEffect(selectedLat, selectedLon) {
        if (selectedLat != null && selectedLon != null) {
            mapView.overlays.removeAll { it is Marker }

            val marker = Marker(mapView).apply {
                position = GeoPoint(selectedLat, selectedLon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = context.getString(R.string.map_selected_location)
                snippet = context.getString(R.string.map_coordinates, selectedLat, selectedLon)
            }
            mapView.overlays.add(marker)
            mapView.controller.animateTo(GeoPoint(selectedLat, selectedLon))
            mapView.invalidate()
        }
        onDispose { }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }
}
