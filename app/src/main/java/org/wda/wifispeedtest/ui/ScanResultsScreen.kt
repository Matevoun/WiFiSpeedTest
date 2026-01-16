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

@Composable
fun ScanResultsScreen(
    scanResults: List<ScanResult>,
    currentBssid: String?,
    currentSsid: String?,
    onItemClick: (ScanResult) -> Unit,
    onDetailsClick: (ScanResult) -> Unit
) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize()
    ) {
        items(scanResults) { result ->
            WifiInfoItem(
                result = result,
                onConnectClick = { onItemClick(result) },
                onDetailsClick = { onDetailsClick(result) },
                currentBssid = currentBssid,
                currentSsid = currentSsid
            )
        }
    }
}

@Composable
fun WifiInfoItem(
    result: ScanResult,
    onConnectClick: () -> Unit,
    onDetailsClick: () -> Unit,
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
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyConnected) {
                getSignalColor(result.level)
            } else {
                getSignalColor(result.level).copy(alpha = 0.6f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onConnectClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ssid,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (isCurrentlyConnected) {
                        Text(stringResource(id = R.string.network_status_connected_symbol), fontSize = 24.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Text(stringResource(id = R.string.network_action_details_symbol), fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(start = 12.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(4.dp))

            Column(modifier = Modifier.clickable(onClick = onDetailsClick)) {
                val detailFontSize = 13.sp

                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_power)) }
                    append(" ${result.level} dBm")
                }, fontSize = detailFontSize)
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_mac)) }
                    append(" ${result.BSSID}")
                }, fontSize = detailFontSize)
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_channel)) }
                    val channel = getChannelFromFrequency(result.frequency)
                    append(if (channel > 0) " $channel" else " ${stringResource(id = R.string.unknown)}")
                }, fontSize = detailFontSize)
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_band)) }
                    append(" ${getBandAndWidth(context, result)}")
                }, fontSize = detailFontSize)
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_security)) }
                    append(" ${getSecurityType(context, result)}")
                }, fontSize = detailFontSize)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_wifi_standard)) }
                        append(" ${getWifiStandard(context, result.wifiStandard)}")
                    }, fontSize = detailFontSize)
                }
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(id = R.string.network_info_distance)) }
                    val distance = getApproximateDistance(result.level, result.frequency)
                    append(if (distance > 100) " ${stringResource(id = R.string.network_info_distance_over_100)}" else " ${stringResource(id = R.string.network_info_distance_value).format(distance)}")
                }, fontSize = detailFontSize)
            }
        }
    }
}
