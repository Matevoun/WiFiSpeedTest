
@file:Suppress("DEPRECATION", "RedundantQualifierName", "RegExpRedundantEscape")

package org.wda.wifispeedtest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    private val viewModel: WifiViewModel by viewModels()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            viewModel.updateCurrentConnectionInfo(applicationContext)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            viewModel.updateCurrentConnectionInfo(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WifiScannerApp(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateCurrentConnectionInfo(this)
        viewModel.registerScanReceiver(this)
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onPause() {
        super.onPause()
        viewModel.unregisterScanReceiver(this)
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun hasAllPermissions(context: Context): Boolean {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        context.startActivity(intent)
    }
    
    fun hasWifiPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun WifiScannerApp(viewModel: WifiViewModel) {
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val currentBssid by viewModel.currentBssid.collectAsStateWithLifecycle()
    val showConnectionFailedDialog by viewModel.showConnectionFailedDialog.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    var resultToShowDialog by remember { mutableStateOf<ScanResult?>(null) }

    LaunchedEffect(Unit) {
        viewModel.shareFileEvent.collect { uri ->
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/csv"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager le fichier CSV"))
        }
    }

    if (showConnectionFailedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConnectionFailedDialog() },
            title = { Text("Échec de la connexion") },
            text = { Text("Impossible de se connecter automatiquement. Voulez-vous ouvrir les paramètres WiFi pour une connexion manuelle ?") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.dismissConnectionFailedDialog()
                        mainActivity?.openWifiSettings(context)
                    }
                ) {
                    Text("Ouvrir les paramètres")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConnectionFailedDialog() }) {
                    Text("Annuler")
                }
            }
        )
    }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            viewModel.startScan(context)
        } else {
            Toast.makeText(context, "Les permissions sont nécessaires. Activez-les dans les paramètres.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
        }
    }

    val startScan = {
        if (mainActivity?.hasAllPermissions(context) == true) {
            viewModel.startScan(context)
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                FloatingActionButton(
                    onClick = { viewModel.exportToCsvAndShare(context, scanResults) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("CSV", modifier = Modifier.padding(16.dp))
                }

                FloatingActionButton(
                    onClick = { viewModel.getCurrentLocation(context) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text("GPS", fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                }

                FloatingActionButton(
                    onClick = { if (!isScanning) startScan() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Actualiser", modifier = Modifier.padding(16.dp))
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                scanResults.isEmpty() && !isScanning -> HomeScreen()
                else -> ScanResultsScreen(
                    scanResults = scanResults,
                    currentBssid = currentBssid,
                    onItemClick = { resultToShowDialog = it }
                )
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
                        if (mainActivity?.hasWifiPermissions(context) == true) {
                            viewModel.connectToNetwork(context, result, password)
                        } else {
                            Toast.makeText(context, "Permissions manquantes.", Toast.LENGTH_LONG).show()
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
    val uriHandler = LocalUriHandler.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Main message in the center
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 150.dp), // Pushed up a bit
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Cliquez sur le bouton 'Actualiser' pour rechercher les réseaux WiFi.",
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center
            )
        }

        // Version and Disclaimer at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = try { "v${BuildConfig.VERSION_NAME}" } catch (_: Exception) { "v0.8" }, // Fallback si BuildConfig foire (rare)
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // --- Disclaimer --- 
            val annotatedString = buildAnnotatedString {
                append("Développé bénévolement par ")
                pushStringAnnotation(tag = "dev_url", annotation = "https://mathieu.charreyre.net")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("Mathieu CHARREYRE")
                }
                pop()
                append(" pour l'")
                pushStringAnnotation(tag = "wda_url", annotation = "https://wda-fr.org")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("Association WDA")
                }
                pop()
                append(".\n(Licence MIT)")
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "dev_url", start = offset, end = offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                    annotatedString.getStringAnnotations(tag = "wda_url", start = offset, end = offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                }
            )

            // --- Donation --- 
            val donationString = buildAnnotatedString {
                append("Si cette application vous est utile, pensez à ")
                pushStringAnnotation(tag = "donate_url", annotation = "https://wda-fr.org/index.php?page=help")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("faire un don à l'association")
                }
                pop()
                append(".")
            }

            ClickableText(
                text = donationString,
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                onClick = { offset ->
                    donationString.getStringAnnotations(tag = "donate_url", start = offset, end = offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                }
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
                        Text("✓", fontSize = 26.sp, modifier = Modifier.padding(start = 10.dp))
                    }
                }
                Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(start = 16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Puissance : ") }
                append("${result.level} dBm")
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("MAC : ") }
                append(result.BSSID)
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Canal : ") }
                val channel = getChannelFromFrequency(result.frequency)
                append(if (channel > 0) "$channel" else "Inconnu")
            })
            // Nouvelle : Bande de fréquence (2.4/5/6 GHz)
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Bande : ") }
                append(getBandAndWidth(result))  // Combo GHz (MHz) : un seul champ, clean et complet
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
                            TextButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.padding(end = 4.dp) // Évite le débord, comme un chat qui laisse de l'espace
                            ) {
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
            TextButton(onClick = { onConnect(if (isSecure) password else null) }) {
                Text("Se connecter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

fun getSecurityType(result: ScanResult): String {
    val capabilities = result.capabilities
    return when {
        capabilities.contains("WPA3") -> "WPA3-PSK"
        capabilities.contains("WPA2") -> "WPA2-PSK"
        capabilities.contains("WPA") -> "WPA-PSK"
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
    } else "N/A"
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

@Suppress("DEPRECATION")
fun ScanResult.getSsidName(): String {
    val modern = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.wifiSsid?.toString()
    } else null

    return when {
        !modern.isNullOrBlank() -> modern
        !this.SSID.isNullOrBlank() && this.SSID != "<unknown ssid>" -> this.SSID
        else -> "Réseau caché"
    }
}

fun getChannelFromFrequency(frequency: Int): Int {
    return when (frequency) {
        in 2412..2484 -> ((frequency - 2412) / 5) + 1
        in 5170..5825 -> ((frequency - 5000) / 5)
        in 5955..7115 -> ((frequency - 5950) / 5)
        else -> -1
    }
}

fun getChannelWidthText(width: Int): String {
    return when (width) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
        ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
        ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
        ScanResult.CHANNEL_WIDTH_320MHZ -> "320 MHz"
        else -> "Inconnu"
    }
}

// Détecte la bande WiFi basée sur la fréquence (2.4/5/6 GHz)
// Utilise 'when (frequency)' pour plus de clarté : frequency est le sujet direct des guards
fun getBandFromFrequency(frequency: Int): String {
    return when (frequency) {  // frequency en star : plus narratif, moins de répétitions
        in 2400..2500 -> "2.4 GHz"
        in 5000..6000 -> "5 GHz"
        in 5955..7115 -> "6 GHz"  // Aligné sur getChannelFromFrequency pour cohérence
        else -> "Inconnu"
    }
}

// Combine bande (GHz) et largeur (MHz) en une string lisible, e.g. "2.4 GHz (20 MHz)"
// Utile pour UI et CSV : garde ça DRY (Don't Repeat Yourself), comme un bon commodat partagé
fun getBandAndWidth(result: ScanResult): String {
    val band = getBandFromFrequency(result.frequency)
    val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        "(${getChannelWidthText(result.channelWidth)})"
    } else {
        ""  // Pas de largeur sur vieux Android, on zappe les ()
    }
    return "$band $width".trim()  // Trim pour virer l'espace si pas de width
}