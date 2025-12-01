@file:Suppress("DEPRECATION", "RedundantQualifierName", "RegExpRedundantEscape")

package org.wda.wifispeedtest

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.MacAddress
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    // Remplacé Handler par lifecycleScope pour poster sur le thread UI
    private var wifiScanReceiver: BroadcastReceiver? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    val currentBssidState = mutableStateOf<String?>(null)

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateCurrentBssid()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectivityManager = getSystemService(ConnectivityManager::class.java)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WifiScannerApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentBssid() // mise à jour immédiate

        // Enregistrement du receiver d'état Wi‑Fi (unique, on évite double enregistrement)
        try {
            registerReceiver(wifiStateReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        } catch (_: IllegalArgumentException) {
            // déjà enregistré ou autre problème ; on ignore
        }
    }

    override fun onPause() {
        super.onPause()
        // Désinscription sécurisée du scan receiver s'il existe
        wifiScanReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
            } finally {
                wifiScanReceiver = null
            }
        }

        // Désinscription sécurisée du wifiStateReceiver
        try {
            unregisterReceiver(wifiStateReceiver)
        } catch (_: IllegalArgumentException) {
        }

        // Désinscription du callback réseau si présent
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            } finally {
                networkCallback = null
            }
        }
    }

    fun hasAllPermissions(context: Context): Boolean {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Compose gère le layout ; rien à faire ici
    }

    @SuppressLint("MissingPermission")
    internal fun updateCurrentBssid() {
        val wifiManager = applicationContext.getSystemService(WifiManager::class.java) ?: return
        @Suppress("DEPRECATION")
        val info = try { wifiManager.connectionInfo } catch (_: Exception) { null }
        val bssid = if (info != null && info.networkId != -1 && !info.bssid.isNullOrBlank()) {
            info.bssid.uppercase()
        } else null

        if (bssid != currentBssidState.value) {
            currentBssidState.value = bssid
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(context: Context, onResults: (List<ScanResult>) -> Unit) {
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java) ?: run {
            onResults(emptyList())
            return
        }

        // Si un scanReceiver est déjà enregistré, on le désinscrit proprement avant d'enregistrer un nouveau
        wifiScanReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
            wifiScanReceiver = null
        }

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    @Suppress("DEPRECATION")
                    val results = try {
                        wifiManager.scanResults
                            .filter { it.getSsidName().isNotBlank() }
                            .sortedByDescending { it.level }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    onResults(results)

                    // Désinscription après réception
                    try { c.unregisterReceiver(this) } catch (_: IllegalArgumentException) {}
                    wifiScanReceiver = null
                } else {
                    // Si échec, on renvoie liste vide et désinscrit
                    onResults(emptyList())
                    try { c.unregisterReceiver(this) } catch (_: IllegalArgumentException) {}
                    wifiScanReceiver = null
                }
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        try {
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        } catch (_: SecurityException) {
            // Permissions manquantes
            try { context.unregisterReceiver(wifiScanReceiver) } catch (_: IllegalArgumentException) {}
            wifiScanReceiver = null
            Toast.makeText(context, "Permission manquante pour scanner les réseaux.", Toast.LENGTH_LONG).show()
            onResults(emptyList())
        } catch (_: Exception) {
            // Erreur générique lors du démarrage du scan
            try { context.unregisterReceiver(wifiScanReceiver) } catch (_: IllegalArgumentException) {}
            wifiScanReceiver = null
            onResults(emptyList())
        }
    }

    private fun connectWithSpecifier(
        context: Context,
        specifier: WifiNetworkSpecifier,
        ssid: String,
        expectedBssid: String
    ) {
        // Nettoyage du callback précédent
        networkCallback?.let {
            try { connectivityManager?.unregisterNetworkCallback(it) } catch (_: Exception) {}
            networkCallback = null
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {

            private fun ui(action: () -> Unit) {
                // Utilisation de lifecycleScope pour poster sur le thread UI
                lifecycleScope.launch(Dispatchers.Main) { action() }
            }

            override fun onAvailable(network: Network) {
                ui {
                    val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
                    @Suppress("DEPRECATION")
                    val connectedBssid = try { wifiManager?.connectionInfo?.bssid?.uppercase() } catch (_: Exception) { null }

                    when {
                        connectedBssid == null -> {
                            Toast.makeText(context, "✓ Réseau $ssid disponible (BSSID non fourni)", Toast.LENGTH_LONG).show()
                        }
                        connectedBssid.equals(expectedBssid.uppercase(), ignoreCase = true) -> {
                            Toast.makeText(context, "✓ Connecté à $ssid ($connectedBssid)", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(context, "⚠ Connecté à $ssid mais BSSID différent ($connectedBssid ≠ $expectedBssid)", Toast.LENGTH_LONG).show()
                        }
                    }

                    updateCurrentBssid()
                }
            }

            override fun onUnavailable() {
                ui {
                    Toast.makeText(context, "Connexion impossible à $ssid (refus système)", Toast.LENGTH_LONG).show()
                }
            }

            override fun onLost(network: Network) {
                ui {
                    Toast.makeText(context, "Connexion perdue à $ssid", Toast.LENGTH_LONG).show()
                    updateCurrentBssid()
                }
            }
        }

        try {
            connectivityManager?.requestNetwork(request, networkCallback!!, 10000)
            Toast.makeText(context, "Connexion à $ssid en cours…", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Permission refusée par le système: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur de connexion: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasWifiPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToNetwork(context: Context, result: ScanResult, password: String? = null) {
        if (!hasWifiPermissions(context)) {
            Toast.makeText(context, "Permissions manquantes pour se connecter.", Toast.LENGTH_LONG).show()
            return
        }

        val ssid = result.getSsidName()
        val bssid = try {
            MacAddress.fromString(result.BSSID)
        } catch (_: IllegalArgumentException) {
            Toast.makeText(context, "Adresse MAC invalide", Toast.LENGTH_SHORT).show()
            return
        }

        val caps = result.capabilities.uppercase()

        // CAS MODERNES : WPA2 / WPA3 / Ouvert → WifiNetworkSpecifier
        if (caps.contains("WPA2") || caps.contains("WPA3") || caps.contains("ESS") || caps.isBlank()) {
            val builder = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setBssid(bssid)

            if (!password.isNullOrEmpty()) {
                if (caps.contains("WPA3")) builder.setWpa3Passphrase(password)
                else if (caps.contains("WPA2")) builder.setWpa2Passphrase(password)
            }

            connectWithSpecifier(context, builder.build(), ssid, bssid.toString())
            return
        }

        // CAS ANCIENS : WEP / WPA1 uniquement → message clair
        if (caps.contains("WEP") || (caps.contains("WPA") && !caps.contains("WPA2"))) {
            Toast.makeText(
                context,
                "Le type de sécurité $caps n’est plus supporté sur Android ${Build.VERSION.SDK_INT}.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Toast.makeText(context, "Type de sécurité inconnu : $caps", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun WifiScannerApp() {
    var scanResults by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    var isScanning by remember { mutableStateOf(false) }
    var resultToShowDialog by remember { mutableStateOf<ScanResult?>(null) }

    val currentBssid by (mainActivity?.currentBssidState ?: remember { mutableStateOf(null) })

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            isScanning = true
            mainActivity?.startScan(context) { results ->
                scanResults = results
                isScanning = false
            }
        } else {
            Toast.makeText(context, "Les permissions sont nécessaires. Activez-les dans les paramètres.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
        }
    }

    val startScan = {
        if (mainActivity?.hasAllPermissions(context) == true) {
            isScanning = true
            mainActivity.startScan(context) { results ->
                scanResults = results
                isScanning = false
                mainActivity.updateCurrentBssid()
            }
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!isScanning) startScan() }) {
                Text("Actualiser", modifier = Modifier.padding(16.dp))
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                scanResults.isEmpty() && !isScanning -> {
                    HomeScreen()
                }
                else -> {
                    ScanResultsScreen(
                        scanResults = scanResults,
                        currentBssid = currentBssid,
                        onItemClick = { resultToShowDialog = it }
                    )
                }
            }

            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }

            resultToShowDialog?.let { result ->
                ConnectionDialog(
                    result = result,
                    onDismiss = { resultToShowDialog = null },
                    onConnect = { password ->
                        if (mainActivity?.hasAllPermissions(context) == true) {
                            mainActivity.connectToNetwork(context, result, password)
                        } else {
                            Toast.makeText(context, "Permissions manquantes. Veuillez les activer dans les paramètres.", Toast.LENGTH_LONG).show()
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        }
                        resultToShowDialog = null
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Cliquez sur le bouton 'Actualiser' pour rechercher les réseaux WiFi.",
                modifier = Modifier.padding(32.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ScanResultsScreen(
    scanResults: List<ScanResult>,
    currentBssid: String?,
    onItemClick: (ScanResult) -> Unit
) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize()
    ) {
        items(scanResults) { result ->
            WifiInfoItem(
                result = result,
                onClick = { onItemClick(result) },
                currentBssid = currentBssid
            )
        }
    }
}

@Composable
fun WifiInfoItem(result: ScanResult, onClick: () -> Unit, currentBssid: String?) {
    val ssid = result.getSsidName()
    val isCurrentlyConnected = currentBssid != null &&
            result.BSSID.equals(currentBssid, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyConnected)
                getSignalColor(result.level).copy(alpha = 0.92f)
            else
                getSignalColor(result.level)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ← LIGNE TITRE : SSID + check + flèche → TOUT ALIGNÉ PARFAITEMENT AU CENTRE VERTICAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ssid,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    if (isCurrentlyConnected) {
                        Text(
                            text = "✓",
                            fontSize = 26.sp,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }

                // Flèche parfaitement centrée verticalement avec le SSID
                Text(
                    text = "›",
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Le reste inchangé
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Puissance : ") }
                append("${result.level} dBm")
            })
            @Suppress("DEPRECATION")
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("MAC : ") }
                append(result.BSSID)
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Protection : ") }
                append(getSecurityType(result))
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Standard WiFi : ") }
                    append(getWifiStandard(result.wifiStandard))
                })
            }
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Distance : ") }
                val distance = getApproximateDistance(result.level, result.frequency)
                append(if (distance > 100) ">100 m" else "~%.1f m".format(distance))
            })
        }
    }
}

@Composable
fun ConnectionDialog(result: ScanResult, onDismiss: () -> Unit, onConnect: (String?) -> Unit) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val isSecure = getSecurityType(result) != "Ouvert"
    val ssid = result.getSsidName()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connexion à $ssid") },
        text = {
            Column {
                if (isSecure) {
                    Text("Ce réseau est sécurisé.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Masquer" else "Afficher")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Réseau ouvert. Se connecter ?")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConnect(if (isSecure) password else null)
            }) {
                Text("Se connecter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

fun getSecurityType(result: ScanResult): String {
    @Suppress("DEPRECATION")
    val capabilities = result.capabilities
    return when {
        capabilities.contains("WPA3") -> "WPA3"
        capabilities.contains("WPA2") -> "WPA2"
        capabilities.contains("WPA") -> "WPA"
        capabilities.contains("WEP") -> "WEP"
        else -> "Ouvert"
    }
}

fun getWifiStandard(standard: Int): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when (standard) {
            ScanResult.WIFI_STANDARD_LEGACY -> "802.11a/b/g"
            ScanResult.WIFI_STANDARD_11N -> "WiFi 4 (802.11n)"
            ScanResult.WIFI_STANDARD_11AC -> "WiFi 5 (802.11ac)"
            ScanResult.WIFI_STANDARD_11AX -> "WiFi 6 (802.11ax)"
            else -> "Inconnu"
        }
    } else {
        "N/A"
    }
}

fun getSignalColor(level: Int): Color {
    return when {
        level > -55 -> Color(0xFF4CAF50)
        level > -65 -> Color(0xFF8BC34A)
        level > -75 -> Color(0xFFFFC107)
        level > -85 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

fun getApproximateDistance(level: Int, frequency: Int): Double {
    val exp = (27.55 - (20 * log10(frequency.toDouble())) + abs(level)) / 20.0
    return 10.0.pow(exp)
}

/**
 * Récupère le SSID du ScanResult en tenant compte de la version d'Android.
 * Utilise result.SSID (déprécié) pour les versions < TIRAMISU (33)
 * et result.wifiSsid?.toString() (si non null) pour TIRAMISU+.
 */
@Suppress("DEPRECATION")
fun ScanResult.getSsidName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Pour Android 13 (TIRAMISU) et supérieur, on utilise wifiSsid
        // qui est l'objet WifiSsid encapsulant le SSID. On s'assure qu'il n'est pas null.
        this.wifiSsid?.toString() ?: this.SSID // On garde la valeur dépréciée comme fallback
    } else {
        // Pour les versions antérieures, on utilise l'attribut SSID déprécié.
        this.SSID
    }
}