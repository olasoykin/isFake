package com.isfake.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.isfake.app.MainActivity
import com.isfake.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MockLocationService : Service() {

    companion object {
        const val CHANNEL_ID = "mock_location_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_ALTITUDE = "extra_altitude"
        const val EXTRA_ACCURACY = "extra_accuracy"
        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val ACTION_STOP = "action_stop_mock"

        private const val PREFS_NAME = "mock_location_service_prefs"
        private const val KEY_IS_ACTIVE = "key_is_active"
        private const val KEY_LAT = "key_lat"
        private const val KEY_LNG = "key_lng"
        private const val KEY_ALTITUDE = "key_altitude"
        private const val KEY_ACCURACY = "key_accuracy"
        private const val KEY_INTERVAL = "key_interval"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
        val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

        fun isServiceActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return _isRunning.value || prefs.getBoolean(KEY_IS_ACTIVE, false)
        }

        fun getSavedLocation(context: Context): Pair<Double, Double>? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_IS_ACTIVE, false)) return null
            val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
            val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull() ?: return null
            return Pair(lat, lng)
        }

        fun start(
            context: Context,
            lat: Double,
            lng: Double,
            altitude: Double = 10.0,
            accuracy: Float = 1.0f,
            intervalMs: Long = 1000L
        ) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LNG, lng)
                putExtra(EXTRA_ALTITUDE, altitude)
                putExtra(EXTRA_ACCURACY, accuracy)
                putExtra(EXTRA_INTERVAL_MS, intervalMs)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var locationManager: LocationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mockJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var prefs: SharedPreferences

    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var altitude: Double = 10.0
    private var accuracy: Float = 1.0f
    private var intervalMs: Long = 1000L

    private val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        "fused"
    )

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMockingService()
            return START_NOT_STICKY
        }

        if (intent != null) {
            lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
            lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
            altitude = intent.getDoubleExtra(EXTRA_ALTITUDE, 10.0)
            accuracy = intent.getFloatExtra(EXTRA_ACCURACY, 1.0f)
            intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 1000L)

            saveServiceState(isActive = true)
        } else {
            // Service was killed and restarted by the system (START_STICKY recovery)
            val wasActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
            if (wasActive) {
                lat = prefs.getString(KEY_LAT, "0.0")?.toDoubleOrNull() ?: 0.0
                lng = prefs.getString(KEY_LNG, "0.0")?.toDoubleOrNull() ?: 0.0
                altitude = prefs.getFloat(KEY_ALTITUDE, 10.0f).toDouble()
                accuracy = prefs.getFloat(KEY_ACCURACY, 1.0f)
                intervalMs = prefs.getLong(KEY_INTERVAL, 1000L)
            } else {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        acquireWakeLock()
        startForegroundInternal()
        addTestProviders()
        startMocking()

        _isRunning.value = true
        _currentLocation.value = Pair(lat, lng)

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "isFake:MockLocationServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // Safe max timeout 24 hours
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null
    }

    private fun saveServiceState(isActive: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_IS_ACTIVE, isActive)
            if (isActive) {
                putString(KEY_LAT, lat.toString())
                putString(KEY_LNG, lng.toString())
                putFloat(KEY_ALTITUDE, altitude.toFloat())
                putFloat(KEY_ACCURACY, accuracy)
                putLong(KEY_INTERVAL, intervalMs)
            }
            apply()
        }
    }

    private fun startForegroundInternal() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMockingService() {
        _isRunning.value = false
        _currentLocation.value = null
        saveServiceState(isActive = false)
        removeTestProviders()
        releaseWakeLock()
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // Ignore
        }
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep running in foreground persistently when the app is swiped away from Recents
    }

    private fun addTestProviders() {
        providers.forEach { provider ->
            try {
                locationManager.removeTestProvider(provider)
            } catch (e: Exception) {
                // Provider may not exist yet, ignore
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    locationManager.addTestProvider(
                        provider,
                        false, false, false, false, true, true, true,
                        ProviderProperties.POWER_USAGE_LOW,
                        ProviderProperties.ACCURACY_FINE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    locationManager.addTestProvider(
                        provider,
                        false, false, false, false, true, true, true,
                        android.location.Criteria.POWER_LOW,
                        android.location.Criteria.ACCURACY_FINE
                    )
                }
                locationManager.setTestProviderEnabled(provider, true)
            } catch (e: SecurityException) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    // App is not set as mock location app in developer options
                    stopMockingService()
                }
            } catch (e: Exception) {
                // Ignore other errors (e.g. fused provider not allowed on some devices)
            }
        }
    }

    private fun startMocking() {
        mockJob?.cancel()
        mockJob = serviceScope.launch {
            while (true) {
                providers.forEach { provider ->
                    try {
                        val mockLocation = Location(provider).apply {
                            latitude = lat
                            longitude = lng
                            this.altitude = altitude
                            time = System.currentTimeMillis()
                            this.accuracy = accuracy
                            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                            speed = 0.0f
                            bearing = 0.0f
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                verticalAccuracyMeters = accuracy
                                speedAccuracyMetersPerSecond = 0.1f
                                bearingAccuracyDegrees = 0.1f
                            }
                        }
                        locationManager.setTestProviderLocation(provider, mockLocation)
                    } catch (e: Exception) {
                        // Provider might have been removed or not supported, ignore
                    }
                }
                delay(intervalMs)
            }
        }
    }

    private fun removeTestProviders() {
        mockJob?.cancel()
        providers.forEach { provider ->
            try {
                locationManager.setTestProviderEnabled(provider, false)
                locationManager.removeTestProvider(provider)
            } catch (e: Exception) {
                // Ignore if already removed
            }
        }
    }

    override fun onDestroy() {
        stopMockingService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_STOP
        }.let {
            PendingIntent.getService(
                this,
                1,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val latStr = "%.6f".format(lat)
        val lngStr = "%.6f".format(lng)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, latStr, lngStr))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop),
                stopIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
