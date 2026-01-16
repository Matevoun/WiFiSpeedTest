package org.wda.wifispeedtest.ui

import android.net.wifi.ScanResult
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wda.wifispeedtest.R
import org.wda.wifispeedtest.getApproximateDistance
import org.wda.wifispeedtest.getBandAndWidth
import org.wda.wifispeedtest.getChannelFromFrequency
import org.wda.wifispeedtest.getSecurityType
import org.wda.wifispeedtest.getSsidName
import org.wda.wifispeedtest.getWifiStandard

@Composable
fun ApDetailsDialog(
    result: ScanResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val ssid = getSsidName(context, result)
    val security = getSecurityType(context, result)
    val bandAndWidth = getBandAndWidth(context, result)
    val channel = getChannelFromFrequency(result.frequency)
    val distance = getApproximateDistance(result.level, result.frequency)
    val wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getWifiStandard(context, result.wifiStandard)
    } else {
        stringResource(id = R.string.not_applicable)
    }
    val distanceString = if (distance > 100) {
        stringResource(id = R.string.network_info_distance_over_100)
    } else {
        stringResource(id = R.string.network_info_distance_value, distance)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = ssid) },
        text = {
            Column {
                DetailRow(label = stringResource(R.string.network_info_power), value = "${result.level} dBm")
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = stringResource(R.string.network_info_mac), value = result.BSSID)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = stringResource(R.string.network_info_security), value = security)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = stringResource(R.string.network_info_band), value = bandAndWidth)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = stringResource(R.string.network_info_channel), value = channel.toString())
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = stringResource(R.string.network_info_wifi_standard), value = wifiStandard)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = stringResource(R.string.network_info_distance), value = distanceString)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = "$label ", fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}
