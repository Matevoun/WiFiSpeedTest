package org.wda.wifispeedtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import android.graphics.Bitmap

// Wrapper pour associer un ScanResult à sa localisation et son timestamp.
data class LocatedScanResult(
    val scanResult: ScanResult,
    val location: String?,
    val wallClockTimestamp: Long,
    val connected: Boolean = false
)

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    // Stocke l'historique complet de tous les scans.
    private val _allHistoricalScanResults = MutableStateFlow<List<LocatedScanResult>>(emptyList())

    // La liste affichée à l'écran (synthèse des résultats les plus récents/forts).
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _currentBssid = MutableStateFlow<String?>(null)
    val currentBssid: StateFlow<String?> = _currentBssid.asStateFlow()

    private val _currentSsid = MutableStateFlow<String?>(null)
    val currentSsid: StateFlow<String?> = _currentSsid.asStateFlow()

    private val _shareFileEvent = MutableSharedFlow<Uri>()
    val shareFileEvent = _shareFileEvent.asSharedFlow()

    private val _shareImageEvent = MutableSharedFlow<Uri>()
    val shareImageEvent = _shareImageEvent.asSharedFlow()

    private val _lastScanAt = MutableStateFlow<Long?>(null)
    val lastScanAt: StateFlow<Long?> = _lastScanAt.asStateFlow()
    
    private val _showCloseAppDialog = MutableStateFlow(false)
    val showCloseAppDialog: StateFlow<Boolean> = _showCloseAppDialog.asStateFlow()

    private val _closeAppEvent = MutableSharedFlow<Unit>()
    val closeAppEvent = _closeAppEvent.asSharedFlow()

    // La localisation du DERNIER scan.
    private val _currentLocation = MutableStateFlow<String?>(null)
    val currentLocation: StateFlow<String?> = _currentLocation.asStateFlow()

    private var scanRetryCount = 0
    private val maxScanRetries = 5
    private val baseRetryDelayMs = 1000L

    private var scanRetryJob: Job? = null
    private var discardNextScan = false
    private var scanTicket = 0

    private val wifiScanReceiver = object : BroadcastReceiver() {
        @Suppress("DEPRECATION")
        override fun onReceive(context: Context, intent: Intent) {
            if (discardNextScan) {
                discardNextScan = false
                _isScanning.update { false }
                return
            }
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            _isScanning.update { false }
            updateCurrentConnectionInfo()

            if (success) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                try {
                    @SuppressLint("MissingPermission")
                    val newScanResults = wifiManager.scanResults
                    val locationForThisScan = _currentLocation.value
                    val timestampForThisScan = System.currentTimeMillis()
                    _lastScanAt.value = timestampForThisScan

                    val connectedBssid = currentBssid.value
                    val newLocatedResults = newScanResults.map { result ->
                        LocatedScanResult(
                            result,
                            locationForThisScan,
                            timestampForThisScan,
                            result.BSSID.equals(connectedBssid, ignoreCase = true)
                        )
                    }

                    // Ajoute les nouveaux résultats à la liste historique.
                    _allHistoricalScanResults.update { currentHistoricalResults ->
                        currentHistoricalResults + newLocatedResults
                    }

                    updateDisplayedScanResults()

                } catch (e: SecurityException) {
                    Toast.makeText(context, context.getString(R.string.scan_access_results_failed_toast, e.localizedMessage), Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("WifiViewModel", "Scan failed, keeping previous results.")
            }
        }
    }

    // Dernier relevé de chaque BSSID, du plus fort au plus faible.
    private fun updateDisplayedScanResults() {
        // Regroupe tout l'historique par BSSID.
        val groupedByBssid = _allHistoricalScanResults.value.groupBy { it.scanResult.BSSID }

        // Ecran : dernier releve de ce BSSID. A horodatage egal, on garde le plus fort.
        val representativeResults = groupedByBssid.mapNotNull { (_, results) ->
            results.maxWithOrNull(
                compareBy<LocatedScanResult> { it.wallClockTimestamp }
                    .thenBy { it.scanResult.level }
            )?.scanResult
        }

        val processedResults = representativeResults.sortedByDescending { it.level }

        _scanResults.update { processedResults }
    }

    fun registerScanReceiver(context: Context) {
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(wifiScanReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(wifiScanReceiver, intentFilter)
            }
        } catch (e: Exception) {
            Log.e("WifiViewModel", "Error registering scan receiver", e)
        }
    }

    fun unregisterScanReceiver(context: Context) {
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) {
            Log.e("WifiViewModel", "Error unregistering scan receiver", e)
        }
    }

    fun startScan() {
        val context = getApplication<Application>()
        if (scanRetryJob?.isActive == true || _isScanning.value) {
            Toast.makeText(context, R.string.scan_already_in_progress_toast, Toast.LENGTH_SHORT).show()
            return
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            Toast.makeText(context, R.string.scan_wifi_disabled_toast, Toast.LENGTH_LONG).show()
            return
        }

        // On lance toute la séquence dans une coroutine.
        viewModelScope.launch {
            val ticket = ++scanTicket
            _isScanning.update { true }

            // Réinitialise la localisation pour forcer la mise à jour de l'UI
            _currentLocation.value = null

            // Étape 1: Obtenir la localisation et attendre la fin.
            val location = fetchCurrentLocation()
            _currentLocation.value = location

            // Étape 2: Démarrer le scan Wi-Fi, seulement après avoir eu la localisation.
            triggerWifiScan()
            delay(20.seconds)
            if (ticket == scanTicket) {
                _isScanning.value = false
            }
        }
    }

    private fun triggerWifiScan() {
        val context = getApplication<Application>()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        @Suppress("DEPRECATION")
        val success = try {
            @SuppressLint("MissingPermission")
            wifiManager.startScan()
        } catch (e: SecurityException) {
            Toast.makeText(context, context.getString(R.string.scan_blocked_security_toast, e.localizedMessage), Toast.LENGTH_LONG).show()
            _isScanning.update { false }
            Log.e("WifiViewModel", "startScan SecurityException", e)
            false
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.scan_failed_exception_toast, e.localizedMessage), Toast.LENGTH_LONG).show()
            _isScanning.update { false }
            Log.e("WifiViewModel", "startScan Exception", e)
            false
        }

        if (success) {
            scanRetryJob?.cancel()
            scanRetryJob = null
            scanRetryCount = 0
        } else {
            scanRetryCount++
            val waitMs = baseRetryDelayMs * (1L shl (scanRetryCount - 1)).coerceAtMost(30)
            Toast.makeText(context, context.getString(R.string.scan_rejected_toast, waitMs / 1000, scanRetryCount), Toast.LENGTH_LONG).show()

            scanRetryJob = viewModelScope.launch {
                delay(waitMs.milliseconds)
                if (scanRetryCount <= maxScanRetries) {
                    Log.i("WifiViewModel", "Retrying wifi scan (attempt $scanRetryCount)")
                    triggerWifiScan()
                } else {
                    Log.w("WifiViewModel", "Max scan retries reached. Aborting further automatic retries.")
                    scanRetryCount = 0
                    _isScanning.update { false }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): String? {
        val context = getApplication<Application>()
        if (!hasLocationPermission(context)) return null

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        return try {
            val location: Location? = withTimeoutOrNull(7.seconds) {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()
                fusedLocationClient.getCurrentLocation(request, null).await()
            }

            if (location != null) {
                "${location.latitude},${location.longitude}"
            } else {
                Log.w("WifiViewModel", "Primary location attempt failed, trying last known location.")
                val lastLocation = withTimeoutOrNull(2.seconds) { fusedLocationClient.lastLocation.await() }
                if (lastLocation != null) {
                    "${lastLocation.latitude},${lastLocation.longitude}"
                } else {
                    Log.w("WifiViewModel", "All location attempts failed.")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("WifiViewModel", "Exception while getting location", e)
            null
        }
    }

    private fun hasLocationPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun updateCurrentBssid(bssid: String?) {
        val newBssid = bssid?.uppercase()
        if (newBssid != _currentBssid.value) {
            _currentBssid.value = newBssid
        }
    }

    fun updateCurrentConnectionInfo() {
        val context = getApplication<Application>()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo

        val bssid = if (info != null && info.networkId != -1 && !info.bssid.isNullOrBlank() && info.bssid != "02:00:00:00:00:00") {
            info.bssid
        } else {
            null
        }
        updateCurrentBssid(bssid)

        val ssid = info?.ssid?.trim('\'')
        _currentSsid.value = if (ssid == "<unknown ssid>") null else ssid
    }

    fun clearSession() {
        discardNextScan = _isScanning.value
        scanRetryJob?.cancel()
        scanRetryJob = null
        scanRetryCount = 0
        _allHistoricalScanResults.value = emptyList()
        _scanResults.value = emptyList()
        _currentLocation.value = null
        _lastScanAt.value = null
        _isScanning.value = true

        val context = getApplication<Application>()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            _isScanning.value = false
            Toast.makeText(context, R.string.scan_wifi_disabled_toast, Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            val ticket = ++scanTicket
            val location = fetchCurrentLocation()
            _currentLocation.value = location
            triggerWifiScan()
            delay(20.seconds)
            if (ticket == scanTicket) {
                _isScanning.value = false
            }
        }
    }

    fun saveChannelImage(bitmap: Bitmap) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "wifi_channels_${System.currentTimeMillis()}.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri == null) {
                    Toast.makeText(context, R.string.export_creating_file_failed, Toast.LENGTH_LONG).show()
                    return@launch
                }
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(context, R.string.channels_saved_toast, Toast.LENGTH_LONG).show()
                _shareImageEvent.emit(uri)
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.channels_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
    
    fun requestCloseApp() {
        _showCloseAppDialog.value = true
    }

    fun confirmCloseApp() {
        viewModelScope.launch {
            _closeAppEvent.emit(Unit)
        }
        _showCloseAppDialog.value = false
    }

    fun dismissCloseAppDialog() {
        _showCloseAppDialog.value = false
    }

    fun deleteLocalExports() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val deleted = deleteOwned(
                context,
                MediaStore.Files.getContentUri("external"),
                "wifi_scan_%.csv"
            ) + deleteOwned(
                context,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "wifi_channels_%.png"
            )
            val message = if (deleted > 0) {
                context.getString(R.string.delete_exports_done)
            } else {
                context.getString(R.string.delete_exports_none)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteOwned(context: Context, collection: Uri, nameLike: String): Int {
        val resolver = context.contentResolver
        val uris = mutableListOf<Uri>()
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf(nameLike),
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                uris.add(ContentUris.withAppendedId(collection, cursor.getLong(idColumn)))
            }
        }
        var count = 0
        uris.forEach { uri ->
            try {
                if (resolver.delete(uri, null, null) > 0) count++
            } catch (_: SecurityException) {
            }
        }
        return count
    }

    fun exportToCsvAndShare() {
        val historicalResults = _allHistoricalScanResults.value
        val context = getApplication<Application>()
        if (historicalResults.isEmpty()) {
            Toast.makeText(context, R.string.export_no_results_toast, Toast.LENGTH_SHORT).show()
            return
        }
        generateAndShareCsv(historicalResults)
    }

    private fun generateAndShareCsv(historicalResults: List<LocatedScanResult>) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val csvContent = buildString {
                    append("DateTime,Timestamp (ms),Latitude,Longitude,SSID,BSSID,Frequence (MHz),Canal,Puissance (dBm),Securite,Bande,Standard,Distance (m),Connecte\n")
                    historicalResults.forEach { locatedResult ->
                        val r = locatedResult.scanResult
                        val isConnected = locatedResult.connected
                        
                        var ssidName = getSsidName(context, r, currentBssid.value, currentSsid.value)
                        ssidName = ssidName.replace("\"", "\"\"")
                        val ssid = "\"$ssidName\""
                        
                        val freq = r.frequency
                        val channel = getChannelFromFrequency(freq)
                        val security = getSecurityType(context, r)
                        val band = buildString {
                            val ghz = getBandFromFrequency(context, freq)
                            append(ghz)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val width = getChannelWidthText(context, r.channelWidth)
                                if (width.isNotEmpty()) append(" ($width)")
                            }
                        }
                        val standard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getWifiStandard(context, r.wifiStandard) else context.getString(R.string.not_applicable)
                        val distance = getApproximateDistance(r.level, freq)
                        
                        val timestamp = locatedResult.wallClockTimestamp
                        val dateTime = try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH)
                            val netDate = Date(timestamp)
                            sdf.format(netDate)
                        } catch (_: Exception) {
                            "N/A"
                        }
                        
                        val positionRaw = locatedResult.location
                        val positionCols = if (positionRaw.isNullOrBlank() || !positionRaw.contains(",")) {
                            Pair(context.getString(R.string.not_applicable), context.getString(R.string.not_applicable))
                        } else {
                            val parts = positionRaw.split(",")
                            Pair(parts.getOrNull(0)?.trim() ?: "", parts.getOrNull(1)?.trim() ?: "")
                        }

                        append("$dateTime,$timestamp,${positionCols.first},${positionCols.second},$ssid,${r.BSSID},$freq,$channel,${r.level},$security,$band,$standard,${String.format(Locale.US, "%.1f", distance)},${if (isConnected) "OUI" else "NON"}\n")
                    }
                }

                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "wifi_scan_${System.currentTimeMillis()}.csv")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri == null) {
                    Toast.makeText(context, R.string.export_creating_file_failed, Toast.LENGTH_LONG).show()
                    return@launch
                }
                resolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, R.string.export_success_toast, Toast.LENGTH_LONG).show()
                _shareFileEvent.emit(uri)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.export_failed_toast, e.message), Toast.LENGTH_LONG).show()
                Log.e("WifiViewModel", "Error exporting to CSV", e)
            }
        }
    }
}
