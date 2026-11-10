package org.wda.wifispeedtest.ui

import android.net.wifi.ScanResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.Alignment
import org.wda.wifispeedtest.R
import org.wda.wifispeedtest.getBandFromFrequency
import org.wda.wifispeedtest.getChannelFromFrequency
import org.wda.wifispeedtest.getSecurityType
import org.wda.wifispeedtest.getSignalColor
import org.wda.wifispeedtest.getSsidName
import org.wda.wifispeedtest.getApproximateDistance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.FlowRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Picture
import androidx.compose.ui.graphics.drawscope.draw

@Composable
fun ScanResultsScreen(
    scanResults: List<ScanResult>,
    currentBssid: String?,
    currentSsid: String?,
    scanLocation: String?,
    scanTimestamp: Long?,
    onOpenWifiSettings: () -> Unit,
    onNewSurvey: () -> Unit,
    onDetailsClick: (ScanResult) -> Unit,
    onSaveChannelImage: (Bitmap) -> Unit
) {
    var confirmNewSurvey by remember { mutableStateOf(false) }

    if (confirmNewSurvey) {
        AlertDialog(
            onDismissRequest = { confirmNewSurvey = false },
            title = { Text(stringResource(R.string.new_survey_dialog_title)) },
            text = { Text(stringResource(R.string.new_survey_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewSurvey = false
                    onNewSurvey()
                }) {
                    Text(stringResource(R.string.new_survey_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewSurvey = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = onOpenWifiSettings,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    stringResource(R.string.action_open_wifi_settings),
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
            FloatingActionButton(
                onClick = { confirmNewSurvey = true },
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    stringResource(R.string.action_new_survey),
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageTab(
                selected = pagerState.currentPage == 0,
                label = stringResource(R.string.page_networks),
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
            )
            PageTab(
                selected = pagerState.currentPage == 1,
                label = stringResource(R.string.page_channels),
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            if (page == 0) {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(scanResults, key = { it.BSSID }) { result ->
                        WifiInfoItem(
                            result = result,
                            onDetailsClick = { onDetailsClick(result) },
                            currentBssid = currentBssid,
                            currentSsid = currentSsid
                        )
                    }
                }
            } else {
                ChannelPage(
                    scanResults = scanResults,
                    scanLocation = scanLocation,
                    scanTimestamp = scanTimestamp,
                    onSaveImage = onSaveChannelImage
                )
            }
        }
    }
}

@Composable
fun WifiInfoItem(
    result: ScanResult,
    onDetailsClick: () -> Unit,
    currentBssid: String?,
    currentSsid: String?
) {
    val context = LocalContext.current
    val connectedSymbol = stringResource(R.string.network_status_connected_symbol)
    val hiddenLabel = stringResource(R.string.network_hidden_ssid)
    val ssid = getSsidName(context, result, currentBssid, currentSsid)
    val isHidden = ssid == hiddenLabel
    val isCurrentlyConnected = currentBssid != null &&
            result.BSSID.equals(currentBssid, ignoreCase = true)
    val channel = getChannelFromFrequency(result.frequency)
    val channelText = if (channel > 0) {
        channel.toString()
    } else {
        stringResource(R.string.unknown)
    }
    val distance = getApproximateDistance(result.level, result.frequency)
    val distanceText = if (distance > 100) {
        stringResource(R.string.network_info_distance_over_100)
    } else {
        stringResource(R.string.network_info_distance_value, distance)
    }
    val summary = stringResource(
        R.string.network_list_summary,
        result.level,
        distanceText,
        channelText,
        getBandFromFrequency(context, result.frequency),
        getSecurityType(context, result)
    )

    Card(
        onClick = onDetailsClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyConnected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildString {
                        append(ssid)
                        if (isCurrentlyConnected) {
                            append(" ")
                            append(connectedSymbol)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontStyle = if (isHidden) FontStyle.Italic else FontStyle.Normal,
                    color = if (isHidden) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = stringResource(R.string.network_more_symbol),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            SignalGauge(
                level = result.level,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            Text(
                text = summary,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun SignalGauge(level: Int, modifier: Modifier = Modifier) {
    val fraction = (level.coerceIn(-90, -30) + 90) / 60f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(14.dp)
                .background(getSignalColor(level))
        )
    }
}

@Composable
private fun PageTab(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 12.dp else 8.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    }
                )
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
        )
    }
}

private data class ChannelHit(
    val channel: Int,
    val frequency: Int,
    val level: Int,
    val width: Int
)

private data class BandPlan(
    val titleRes: Int,
    val matches: (Int) -> Boolean,
    val axis: List<Int>,
    val emphasized: Set<Int>
)

@Composable
private fun ChannelPage(
    scanResults: List<ScanResult>,
    scanLocation: String?,
    scanTimestamp: Long?,
    onSaveImage: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val picture = remember { Picture() }
    val saveFailedText = stringResource(R.string.channels_save_failed)
    val hits = scanResults.mapNotNull { result ->
        val channel = getChannelFromFrequency(result.frequency)
        if (channel <= 0) {
            null
        } else {
            ChannelHit(channel, result.frequency, result.level, result.channelWidth)
        }
    }
    val bands = listOf(
        BandPlan(
            titleRes = R.string.channels_band_24,
            matches = { it in 2400..2500 },
            axis = (1..13).toList(),
            emphasized = setOf(1, 6, 11)
        ),
        BandPlan(
            titleRes = R.string.channels_band_5,
            matches = { it in 5000..5945 },
            axis = listOf(
                36, 40, 44, 48, 52, 56, 60, 64,
                100, 104, 108, 112, 116, 120, 124, 128,
                132, 136, 140, 144, 149, 153, 157, 161, 165, 169, 173, 177
            ),
            emphasized = setOf(36, 40, 44, 48)
        ),
        BandPlan(
            titleRes = R.string.channels_band_6,
            matches = { it >= 5950 },
            axis = (1..233 step 4).toList(),
            emphasized = emptySet()
        )
    ).mapNotNull { plan ->
        val bandHits = hits.filter { plan.matches(it.frequency) }
        if (bandHits.isEmpty()) null else plan to bandHits
    }
    val whenScanned = scanTimestamp ?: System.currentTimeMillis()
    val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(whenScanned))
    val placeText = if (scanLocation.isNullOrBlank() || !scanLocation.contains(",")) {
        stringResource(R.string.channels_gps_unavailable)
    } else {
        scanLocation
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
        Column(
            modifier = Modifier.drawWithContent {
                val width = size.width.toInt()
                val height = size.height.toInt()
                if (width > 0 && height > 0) {
                    val pictureCanvas = androidx.compose.ui.graphics.Canvas(
                        picture.beginRecording(width, height)
                    )
                    draw(this, layoutDirection, pictureCanvas, size) {
                        drawRect(Color.White)
                        this@drawWithContent.drawContent()
                    }
                    picture.endRecording()
                }
                drawContent()
            }
        ) {
            bands.forEach { (plan, bandHits) ->
                val channels = (plan.axis + bandHits.map { it.channel }).distinct().sorted()
                val empty = channels.filter { channel -> bandHits.none { it.channel == channel } }
                val adviceBody = if (empty.isNotEmpty()) {
                    val listed = shortenChannelList(
                        formatChannelList(
                            empty,
                            { from, to -> resources.getString(R.string.channels_range, from, to) },
                            { head, last -> resources.getString(R.string.channels_list_last, head, last) }
                        )
                    )
                    if (empty.size == 1) {
                        stringResource(R.string.channels_advice_empty_one, listed)
                    } else {
                        stringResource(R.string.channels_advice_empty_many, listed)
                    }
                } else {
                    recommendedChannel(bandHits, channels)?.let {
                        stringResource(R.string.channels_advice_rest, it)
                    }
                }
                if (adviceBody != null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(plan.titleRes))
                            }
                            append(" ")
                            append(adviceBody)
                        },
                        modifier = Modifier.padding(top = 12.dp),
                        fontSize = 15.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (channels.size > 13) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        channels.forEach { channel ->
                            ChannelBar(
                                hit = bandHits.filter { it.channel == channel }.maxByOrNull { it.level },
                                label = channel.toString(),
                                emphasize = channel in plan.emphasized,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        channels.forEach { channel ->
                            ChannelBar(
                                hit = bandHits.filter { it.channel == channel }.maxByOrNull { it.level },
                                label = channel.toString(),
                                emphasize = channel in plan.emphasized,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.channels_scan_date))
                    }
                    append(" ")
                    append(dateText)
                },
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 14.sp
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.channels_scan_gps))
                    }
                    append(" ")
                    append(placeText)
                },
                fontSize = 14.sp
            )
        }
        }
        Button(
            onClick = {
                scope.launch {
                    try {
                        if (picture.width > 0 && picture.height > 0) {
                            onSaveImage(Bitmap.createBitmap(picture))
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, saveFailedText, Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.channels_save_image), maxLines = 1)
        }
    }
}

@Composable
private fun ChannelBar(
    hit: ChannelHit?,
    label: String,
    emphasize: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = if (hit == null) 0f else (hit.level.coerceIn(-90, -30) + 90) / 60f
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (hit != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0f, 1f))
                        .background(congestionColor(hit.level))
                )
            }
        }
        Text(
            text = label,
            fontSize = if (emphasize) 14.sp else 11.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun congestionColor(level: Int): Color {
    return when {
        level > -55 -> colorResource(R.color.signal_very_weak)
        level > -65 -> colorResource(R.color.signal_weak)
        level > -75 -> colorResource(R.color.signal_fair)
        else -> colorResource(R.color.signal_strong)
    }
}

private fun formatChannelList(
    channels: List<Int>,
    range: (Int, Int) -> String,
    joinLast: (String, String) -> String
): String {
    if (channels.isEmpty()) return ""
    val parts = mutableListOf<String>()
    var start = channels.first()
    var previous = start
    channels.drop(1).forEach { channel ->
        if (channel == previous + 1) {
            previous = channel
        } else {
            parts += if (previous == start) start.toString() else range(start, previous)
            start = channel
            previous = channel
        }
    }
    parts += if (previous == start) start.toString() else range(start, previous)
    if (parts.size == 1) return parts[0]
    return joinLast(parts.dropLast(1).joinToString(", "), parts.last())
}

private fun shortenChannelList(list: String, maxChars: Int = 22): String {
    if (list.length <= maxChars) return list
    val limit = (maxChars - 1).coerceAtLeast(1)
    val cut = list.lastIndexOf(',', limit)
    val end = if (cut > 0) cut else limit
    return list.substring(0, end).trimEnd() + "..."
}

private fun channelRadius(width: Int): Int = when (width) {
    ScanResult.CHANNEL_WIDTH_40MHZ -> 4
    ScanResult.CHANNEL_WIDTH_80MHZ, ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 8
    ScanResult.CHANNEL_WIDTH_160MHZ -> 12
    ScanResult.CHANNEL_WIDTH_320MHZ -> 16
    else -> 2
}

private fun channelScore(hits: List<ChannelHit>, channel: Int): Double {
    return hits.sumOf { hit ->
        val distance = abs(hit.channel - channel)
        val radius = channelRadius(hit.width)
        if (distance > radius) {
            0.0
        } else {
            val overlap = 1.0 - distance.toDouble() / (radius + 1)
            val strength = (hit.level.coerceIn(-90, -30) + 90) / 60.0
            overlap * strength
        }
    }
}

private fun recommendedChannel(hits: List<ChannelHit>, candidates: List<Int>): Int? {
    if (hits.isEmpty() || candidates.isEmpty()) return null
    return candidates.minWithOrNull(
        compareBy<Int> { channelScore(hits, it) }.thenBy { it }
    )
}