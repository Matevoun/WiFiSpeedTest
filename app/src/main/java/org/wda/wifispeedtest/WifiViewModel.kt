package org.wda.wifispeedtest

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.coroutines.tasks.await

class WifiViewModel : ViewModel() {

    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _currentBssid = MutableStateFlow<String?>(null)
    val currentBssid: StateFlow<String?> = _currentBssid.asStateFlow()

    private val _shareFileEvent = MutableSharedFlow<Uri>()
    val shareFileEvent = _shareFileEvent.asSharedFlow()

    private val _showConnectionFailedDialog = MutableStateFlow(false)

    val showConnectionFailedDialog: StateFlow<Boolean> = _showConnectionFailedDialog.asStateFlow()

    // Nouveaux : Pour GPS et timestamp (déplacés au niveau class pour accès global, comme un commodat partagé pour tous les scans)
    private val _currentLocation = MutableStateFlow<String?>(null)  // "lat,lon" ou "N/A" – string pour CSV simple
    val currentLocation: StateFlow<String?> = _currentLocation.asStateFlow()

    private val _scanTimestamp = MutableStateFlow<Long?>(null)  // ms depuis epoch, facile à convertir en date
    val scanTimestamp: StateFlow<Long?> = _scanTimestamp.asStateFlow()

    // Receiver WiFi scan : déclenché par Broadcast, met à jour results et connection (inchangé, juste clean duplicate)
    private val wifiScanReceiver = object : BroadcastReceiver() {
        @Suppress("DEPRECATION")
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            val results = try {
                @SuppressLint("MissingPermission")
                if (success) {
                    wifiManager.scanResults
                        .filter { !it.SSID.isNullOrBlank() }
                        .sortedByDescending { it.level }
                } else {
                    emptyList()
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Accès aux résultats bloqué : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                emptyList()
            }
            
            _scanResults.update { results }
            _isScanning.update { false }
            updateCurrentConnectionInfo(context)
        }
    }

    fun registerScanReceiver(context: Context) {
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        try {
            context.registerReceiver(wifiScanReceiver, intentFilter)
        } catch (_: Exception) {}
    }

    fun unregisterScanReceiver(context: Context) {
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (_: Exception) {}
    }
    
    @Suppress("DEPRECATION")
    fun startScan(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasNearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasLocation || !hasNearby) {
            Toast.makeText(context, if (!hasLocation) "Permission localisation refusée" else "Permission Nearby WiFi refusée", Toast.LENGTH_SHORT).show()
            return
        }
        
        _isScanning.update { true }

        val success = try {
            @SuppressLint("MissingPermission")
            wifiManager.startScan()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Scan bloqué : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            _isScanning.update { false }
            false
        }

        if (!success) {
            Toast.makeText(context, "Scan échoué (limite système Android)", Toast.LENGTH_SHORT).show()
            _isScanning.update { false }
        } else {
            _scanTimestamp.value = System.currentTimeMillis()  // Timestamp au succès du scan
            getCurrentLocation(context)  // GPS one-shot, async – n'alourdit pas
        }
    }  // Fin de startScan, clean

    // Nouvelle : Récup GPS one-shot (Fused pour précision, sans tracking constant)
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        if (!hasLocationPermission(context)) return  // Utilise ton hasLocation de startScan

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val currentLocationRequest = CurrentLocationRequest.Builder()  // New API Play 21+ : remplace LocationRequest pour getCurrentLocation (plus précis, no legacy)
            .setMaxUpdateAgeMillis(0)  // Fallback accurate : 0ms = fresh loc seulement (équiv setWaitForAccurateLocation(false), fixe unresolved si version trolle)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)  // Timeout implicite ~10s ; précis pour scans statiques WDA
            .build()

        viewModelScope.launch {
            try {
                val location: Location? = fusedLocationClient.getCurrentLocation(currentLocationRequest, null).await()  // Nullable car peut fail
                location?.let {  // Safe call : exécute seulement si non-null
                    val pos = "${it.latitude},${it.longitude}"  // Properties au lieu de getters Java
                    _currentLocation.value = pos
                } ?: run {  // Elvis operator + run : si null, exec ce bloc
                    _currentLocation.value = "N/A"
                }
            } catch (e: Exception) {
                _currentLocation.value = "N/A"  // Fallback si GPS off ou timeout
                Toast.makeText(context, "GPS indispo : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun updateCurrentBssid(bssid: String?) {
        val newBssid = bssid?.uppercase()
        if (newBssid != _currentBssid.value) {
            _currentBssid.value = newBssid
        }
    }

    fun updateCurrentConnectionInfo(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = try { wifiManager.connectionInfo } catch (_: Exception) { null }
        
        val bssid = if (info != null && info.networkId != -1 && !info.bssid.isNullOrBlank()) {
            info.bssid
        } else {
            null
        }
        updateCurrentBssid(bssid)
    }

    fun dismissConnectionFailedDialog() {
        _showConnectionFailedDialog.value = false
    }

    @SuppressLint("MissingPermission")
    fun connectToNetwork(context: Context, result: ScanResult, password: String?) {
        val ssid = result.getSsidName()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .apply {
                if (!password.isNullOrEmpty()) {
                    if (result.capabilities.contains("WPA3", ignoreCase = true)) {
                        setWpa3Passphrase(password)
                    } else {
                        setWpa2Passphrase(password)
                    }
                }
            }
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Toast.makeText(context, "Connecté automatiquement à $ssid !", Toast.LENGTH_LONG).show()
                connectivityManager.bindProcessToNetwork(network)
            }

            override fun onUnavailable() {
                _showConnectionFailedDialog.value = true
            }
        }

        try {
            connectivityManager.requestNetwork(request, callback)
            Toast.makeText(context, "Tentative de connexion automatique…", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            _showConnectionFailedDialog.value = true
        }
    }

    fun exportToCsvAndShare(context: Context, results: List<ScanResult>) {
        if (results.isEmpty()) {
            Toast.makeText(context, "Aucun réseau à exporter", Toast.LENGTH_SHORT).show()
            return
        }

        val csvContent = buildString {
            append("Timestamp (ms),Latitude,Longitude,SSID,BSSID,Frequence (MHz),Canal,Puissance (dBm),Securite,Bande,Standard,Distance (m)\n")
            results.forEach { r ->
                val isConnected = r.BSSID.equals(currentBssid.value, ignoreCase = true)
                val prefix = if (isConnected) "✓ " else ""
                val ssidName = r.getSsidName().replace("\"", "\"\"")
                val ssid = "\"$prefix$ssidName\""

                val freq = r.frequency
                val channel = getChannelFromFrequency(freq)
                val security = getSecurityType(r)
                val band = buildString {
                    val ghz = when (freq) {
                        in 2400..2500 -> "2.4 GHz"
                        in 5000..6000 -> "5 GHz"
                        in 5955..7115 -> "6 GHz"
                        else -> "Inconnu"
                    }
                    append(ghz)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val width = when (r.channelWidth) {
                            ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
                            ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
                            ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
                            ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
                            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
                            ScanResult.CHANNEL_WIDTH_320MHZ -> "320 MHz"
                            else -> "Inconnu"
                        }
                        append(" ($width)")
                    }
                }
                val standard =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getWifiStandard(r.wifiStandard) else "N/A"
                val distance = getApproximateDistance(r.level, freq)
                val timestamp = scanTimestamp.value?.toString() ?: "N/A"
                val position = currentLocation.value ?: "N/A,N/A"  // Format "lat,lon" ou "N/A,N/A" pour 2 colonnes
                append("$timestamp,$position,$ssid,${r.BSSID},$freq,$channel,${r.level},$security,$band,$standard,${String.format(Locale.US, "%.1f", distance)}\n")
            }
        }

        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        "wifi_scan_${System.currentTimeMillis()}.csv"
                    )
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri == null) {
                    Toast.makeText(context, "Échec de la création du fichier", Toast.LENGTH_LONG)
                        .show()
                    return@launch
                }

                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }

                Toast.makeText(context, "Exporté dans Téléchargements", Toast.LENGTH_LONG).show()
                _shareFileEvent.emit(uri)

            } catch (e: Exception) {
                Toast.makeText(context, "Échec export : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}