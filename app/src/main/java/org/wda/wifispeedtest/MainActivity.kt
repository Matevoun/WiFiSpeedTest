package org.wda.wifispeedtest

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wda.wifispeedtest.ui.ApDetailsDialog
import org.wda.wifispeedtest.ui.ChangelogDialog
import org.wda.wifispeedtest.ui.ConnectionDialog
import org.wda.wifispeedtest.ui.CreditsDialog
import org.wda.wifispeedtest.ui.HomeScreen
import org.wda.wifispeedtest.ui.ScanResultsScreen
import android.net.wifi.ScanResult as WifiScanResult

@Suppress("DEPRECATION", "RegExpRedundantEscape")
class MainActivity : ComponentActivity() {

    private val viewModel: WifiViewModel by viewModels()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            viewModel.updateCurrentConnectionInfo()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            viewModel.updateCurrentConnectionInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.closeAppEvent
            .onEach { finish() }
            .launchIn(lifecycleScope)

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
        viewModel.updateCurrentConnectionInfo()
        viewModel.registerScanReceiver(this)
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        if (connectivityManager != null) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.unregisterScanReceiver(this)
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    /**
     * Unified permissions check for WiFi operations (scan + NEARBY_WIFI_DEVICES on Android 13+).
     */
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
}

@Composable
fun WifiScannerApp(viewModel: WifiViewModel) {
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val minSpinnerShowMs = 400L
    var spinnerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            spinnerVisible = true
        } else {
            delay(minSpinnerShowMs)
            spinnerVisible = false
        }
    }

    val currentBssid by viewModel.currentBssid.collectAsStateWithLifecycle()
    val currentSsid by viewModel.currentSsid.collectAsStateWithLifecycle()
    val showConnectionFailedDialog by viewModel.showConnectionFailedDialog.collectAsStateWithLifecycle()
    val showCloseAppDialog by viewModel.showCloseAppDialog.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    var resultToShowDialog by remember { mutableStateOf<WifiScanResult?>(null) }
    var showApDetailsDialog by remember { mutableStateOf<WifiScanResult?>(null) }
    var showLocationTemporarily by remember { mutableStateOf<String?>(null) }
    val showCreditsDialog = remember { mutableStateOf(false) }
    val showChangelogDialog = remember { mutableStateOf(false) }
    var shouldCheckLocationSettings by remember { mutableStateOf(false) }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            if (loc.contains(",")) {
                showLocationTemporarily = "${context.getString(R.string.location_display_prefix)} $loc"
                delay(3000)
                showLocationTemporarily = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareFileEvent.collect { uri ->
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/csv"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.action_share_csv_title)))
        }
    }

    val showChangelog = {
        showCreditsDialog.value = false
        showChangelogDialog.value = true
    }

    CreditsDialog(
        show = showCreditsDialog.value,
        onDismiss = { showCreditsDialog.value = false },
        onShowChangelog = showChangelog
    )

    ChangelogDialog(
        show = showChangelogDialog.value,
        onDismiss = { showChangelogDialog.value = false }
    )

    if (showConnectionFailedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConnectionFailedDialog() },
            title = { Text(stringResource(id = R.string.network_connection_failed_title)) },
            text = { Text(stringResource(id = R.string.network_connection_failed_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissConnectionFailedDialog()
                        mainActivity?.openWifiSettings(context)
                    }
                ) {
                    Text(stringResource(id = R.string.action_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConnectionFailedDialog() }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
    
    if (showCloseAppDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCloseAppDialog() },
            title = { Text(stringResource(R.string.close_app_dialog_title)) },
            text = { Text(stringResource(R.string.close_app_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmCloseApp() }) {
                    Text(stringResource(R.string.close_app_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCloseAppDialog() }) {
                    Text(stringResource(R.string.close_app_dialog_cancel))
                }
            }
        )
    }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val showPermissionDialog = remember { mutableStateOf(false) }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startScan()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.location_settings_not_enabled),
                Toast.LENGTH_LONG
            ).show()
        }
    }
	
	val actualCheckLocationSettingsAndScan = {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            viewModel.startScan()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e("WifiScannerApp", "Error launching location settings resolution", sendEx)
                }
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.location_check_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(shouldCheckLocationSettings) {
        if (shouldCheckLocationSettings) {
            actualCheckLocationSettingsAndScan()
            shouldCheckLocationSettings = false // Reset the trigger
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            shouldCheckLocationSettings = true
        } else {
            showPermissionDialog.value = true
        }
    }

    if (showPermissionDialog.value) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog.value = false },
            title = { Text(stringResource(R.string.permission_dialog_title)) },
            text = { Text(stringResource(R.string.permission_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog.value = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog.value = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val startScan = {
        if (mainActivity?.hasAllPermissions(context) == true) {
            actualCheckLocationSettingsAndScan()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    showApDetailsDialog?.let {
        ApDetailsDialog(result = it, onDismiss = { showApDetailsDialog = null })
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { showCreditsDialog.value = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(stringResource(id = R.string.action_about), modifier = Modifier.padding(8.dp))
                }

                FloatingActionButton(
                    onClick = { viewModel.exportToCsvAndShare() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(stringResource(id = R.string.action_export_csv), modifier = Modifier.padding(16.dp))
                }
                
                FloatingActionButton(
                    onClick = { viewModel.requestCloseApp() },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(stringResource(id = R.string.close_app_button), modifier = Modifier.padding(16.dp))
                }

                FloatingActionButton(
                    onClick = { if (!isScanning) startScan() },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text(stringResource(id = R.string.action_scan), modifier = Modifier.padding(16.dp))
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
                scanResults.isEmpty() && !isScanning -> HomeScreen(onShowChangelog = showChangelog)
                else -> ScanResultsScreen(
                    scanResults = scanResults,
                    currentBssid = currentBssid,
                    currentSsid = currentSsid,
                    onItemClick = { resultToShowDialog = it },
                    onDetailsClick = { showApDetailsDialog = it }
                )
            }

            showLocationTemporarily?.let { locationText ->
                Text(
                    text = locationText,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (spinnerVisible) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                )
            }

            resultToShowDialog?.let { result ->
                ConnectionDialog(
                    result = result,
                    onDismiss = { resultToShowDialog = null },
                    onConnect = { password ->
                        if (mainActivity?.hasAllPermissions(context) == true) {
                            viewModel.connectToNetwork(result, password)
                        } else {
                            Toast.makeText(context, R.string.permissions_required_toast, Toast.LENGTH_LONG).show()
                        }
                        resultToShowDialog = null
                    },
                    currentBssid = currentBssid,
                    currentSsid = currentSsid
                )
            }
        }
    }
}
