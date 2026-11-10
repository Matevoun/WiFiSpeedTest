package org.wda.wifispeedtest

import android.content.Context
import android.net.wifi.ScanResult
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import kotlin.math.log10
import kotlin.math.pow

/**
 * Détecte le type de sécurité à partir des capabilities.
 */
fun getSecurityType(context: Context, result: ScanResult): String {
    val capabilities = result.capabilities
    return when {
        capabilities.contains("WPA3", ignoreCase = true) -> context.getString(R.string.security_type_wpa3)
        capabilities.contains("WPA2", ignoreCase = true) -> context.getString(R.string.security_type_wpa2)
        capabilities.contains("WPA", ignoreCase = true) -> context.getString(R.string.security_type_wpa)
        capabilities.contains("WEP", ignoreCase = true) -> context.getString(R.string.security_type_wep)
        else -> context.getString(R.string.security_type_open)
    }
}

/**
 * Convertit le code standard WiFi en texte lisible.
 */
fun getWifiStandard(context: Context, standard: Int): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when (standard) {
            ScanResult.WIFI_STANDARD_LEGACY -> context.getString(R.string.wifi_standard_legacy)
            ScanResult.WIFI_STANDARD_11N -> context.getString(R.string.wifi_standard_11n)
            ScanResult.WIFI_STANDARD_11AC -> context.getString(R.string.wifi_standard_11ac)
            ScanResult.WIFI_STANDARD_11AX -> context.getString(R.string.wifi_standard_11ax)
            else -> context.getString(R.string.unknown)
        }
    } else context.getString(R.string.not_applicable)
}

/**
 * Approximation simple de la distance (m) basée sur RSSI et fréquence.
 */
fun getApproximateDistance(level: Int, frequency: Int): Double {
    // This model is based on the free space path loss formula, adjusted for a more realistic
    // transmit power of ~13 dBm. The previous value assumed 0 dBm, which is often too low.
    // Note that this is still a rough approximation.
    val exp = (40.55 - (20 * log10(frequency.toDouble())) + kotlin.math.abs(level)) / 20.0
    return 10.0.pow(exp)
}

/**
 * Retourne un SSID lisible, en utilisant le SSID de la connexion actuelle comme fallback pour le réseau connecté.
 * Gère également les cas où le SSID est "<unknown ssid>" ou entouré de guillemets.
 */
fun getSsidName(
    context: Context,
    result: ScanResult,
    currentBssid: String? = null,
    currentSsid: String? = null
): String {
    val ssidFromScan = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        result.wifiSsid?.toString()
    } else {
        @Suppress("DEPRECATION")
        result.SSID
    })?.trim('"')
        ?.filter { it >= ' ' && it != '\u007F' }
        ?.trim()

    // Un nom vide, ou fait seulement de nuls, n'est pas un SSID.
    fun isValidSsid(ssid: String?): Boolean {
        return !ssid.isNullOrBlank() && ssid != "<unknown ssid>"
    }

    // Si le SSID du scan est valide, on le retourne.
    if (isValidSsid(ssidFromScan)) {
        return ssidFromScan!!
    }

    // Sinon, c'est un réseau masqué ou le SSID est invalide.
    // On vérifie si c'est le réseau actuel pour utiliser le nom de la connexion.
    val cleanedCurrentSsid = currentSsid
        ?.trim('"')
        ?.filter { it >= ' ' && it != '\u007F' }
        ?.trim()
    if (result.BSSID.equals(currentBssid, ignoreCase = true) && isValidSsid(cleanedCurrentSsid)) {
        return cleanedCurrentSsid!!
    }

    // En dernier recours, on indique que c'est un réseau masqué.
    return context.getString(R.string.network_hidden_ssid)
}


/**
 * Calcule le canal à partir de la fréquence (approximation suffisante pour affichage/CSV).
 */
fun getChannelFromFrequency(frequency: Int): Int {
    return when {
        // 2.4 GHz
        frequency == 2484 -> 14 // canal spécial 14
        frequency in 2412..2472 -> ((frequency - 2412) / 5) + 1
        // 5 GHz (approximation)
        frequency in 5000..5949 -> ((frequency - 5000) / 5)
        // 6 GHz (simplifié)
        frequency >= 5950 -> ((frequency - 5950) / 5)
        else -> -1
    }
}

fun getChannelWidthText(context: Context, width: Int): String {
    return when (width) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> context.getString(R.string.channel_width_20)
        ScanResult.CHANNEL_WIDTH_40MHZ -> context.getString(R.string.channel_width_40)
        ScanResult.CHANNEL_WIDTH_80MHZ -> context.getString(R.string.channel_width_80)
        ScanResult.CHANNEL_WIDTH_160MHZ -> context.getString(R.string.channel_width_160)
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> context.getString(R.string.channel_width_80_80)
        ScanResult.CHANNEL_WIDTH_320MHZ -> context.getString(R.string.channel_width_320)
        else -> context.getString(R.string.unknown)
    }
}

/**
 * Détecte la bande WiFi (2.4 / 5 / 6) de façon non recouvrante.
 */
fun getBandFromFrequency(context: Context, frequency: Int): String {
    return when {
        frequency in 2400..2500 -> context.getString(R.string.frequency_band_2_4_ghz)
        frequency >= 5950 -> context.getString(R.string.frequency_band_6_ghz)
        frequency >= 5000 -> context.getString(R.string.frequency_band_5_ghz)
        else -> context.getString(R.string.unknown)
    }
}

/**
 * Combine bande + largeur en texte lisible (ex : "2.4 GHz (20 MHz)").
 */
fun getBandAndWidth(context: Context, result: ScanResult): String {
    val band = getBandFromFrequency(context, result.frequency)
    val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        "(${getChannelWidthText(context, result.channelWidth)})"
    } else {
        ""
    }
    return "$band $width".trim()
}

/**
 * UI-only helper (renvoie Color Compose).
 */
@Composable
fun getSignalColor(level: Int): Color {
    return when {
        level > -55 -> colorResource(id = R.color.signal_strong)
        level > -65 -> colorResource(id = R.color.signal_good)
        level > -75 -> colorResource(id = R.color.signal_fair)
        level > -85 -> colorResource(id = R.color.signal_weak)
        else -> colorResource(id = R.color.signal_very_weak)
    }
}