package org.wda.wifispeedtest.ui

import android.net.wifi.ScanResult
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wda.wifispeedtest.R
import org.wda.wifispeedtest.getApproximateDistance
import org.wda.wifispeedtest.getBandAndWidth
import org.wda.wifispeedtest.getChannelFromFrequency
import org.wda.wifispeedtest.getSecurityType
import org.wda.wifispeedtest.getSignalColor
import org.wda.wifispeedtest.getSsidName
import org.wda.wifispeedtest.getWifiStandard

/**
 * Affiche la liste des résultats du scan WiFi.
 *
 * @param scanResults La liste des [ScanResult] à afficher.
 * @param currentBssid Le BSSID du réseau actuellement connecté, pour le surligner.
 * @param onItemClick La fonction à appeler lorsqu'un élément de la liste est cliqué.
 */
@Composable
fun ScanResultsScreen(
    scanResults: List<ScanResult>,
    currentBssid: String?,
    currentSsid: String?,
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
                currentBssid = currentBssid,
                currentSsid = currentSsid
            )
        }
    }
}

/**
 * Affiche une carte avec les informations détaillées d'un réseau WiFi.
 *
 * @param result Le [ScanResult] contenant les informations à afficher.
 * @param onClick La fonction à appeler lorsque la carte est cliquée.
 * @param currentBssid Le BSSID du réseau actuellement connecté.
 */
@Composable
fun WifiInfoItem(
    result: ScanResult,
    onClick: () -> Unit,
    currentBssid: String?,
    currentSsid: String?
) {
    val context = LocalContext.current
    val ssid = getSsidName(context, result, currentBssid, currentSsid)
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
                        Text(stringResource(id = R.string.network_status_connected_symbol), fontSize = 26.sp, modifier = Modifier.padding(start = 10.dp))
                    }
                }
                Text(stringResource(id = R.string.network_action_details_symbol), fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(start = 16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_power)) }
                append("${result.level} dBm")
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_mac)) }
                append(result.BSSID)
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_channel)) }
                val channel = getChannelFromFrequency(result.frequency)
                append(if (channel > 0) "$channel" else stringResource(id = R.string.unknown))
            })
            // Nouvelle : Bande de fréquence (2.4/5/6 GHz)
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_band)) }
                append(getBandAndWidth(context, result))  // Combo GHz (MHz) : un seul champ, clean et complet
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_security)) }
                append(getSecurityType(context, result))
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_wifi_standard)) }
                    append(getWifiStandard(context, result.wifiStandard))
                })
            }
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_distance)) }
                val distance = getApproximateDistance(result.level, result.frequency)
                append(if (distance > 100) stringResource(id = R.string.network_info_distance_over_100) else stringResource(id = R.string.network_info_distance_value).format(distance))
            })
        }
    }
}