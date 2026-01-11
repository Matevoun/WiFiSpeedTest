package org.wda.wifispeedtest.ui

import android.net.wifi.ScanResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.wda.wifispeedtest.R
import org.wda.wifispeedtest.getSecurityType
import org.wda.wifispeedtest.getSsidName

/**
 * Affiche une boîte de dialogue pour se connecter à un réseau WiFi.
 *
 * @param result Le [ScanResult] du réseau auquel se connecter.
 * @param onDismiss La fonction à appeler pour fermer la boîte de dialogue.
 * @param onConnect La fonction à appeler avec le mot de passe (ou null) pour tenter la connexion.
 */
@Composable
fun ConnectionDialog(
    result: ScanResult,
    onDismiss: () -> Unit,
    onConnect: (String?) -> Unit,
    currentBssid: String?,
    currentSsid: String?
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val isSecure = getSecurityType(context, result) != stringResource(id = R.string.security_type_open)
    val ssid = getSsidName(context, result, currentBssid, currentSsid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_connecting_to, ssid)) },
        text = {
            Column {
                if (isSecure) {
                    Text(stringResource(id = R.string.network_secure_description))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(id = R.string.network_password_label)) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.padding(end = 4.dp) // Évite le débord, comme un chat qui laisse de l'espace
                            ) {
                                Text(if (showPassword) stringResource(id = R.string.action_hide) else stringResource(id = R.string.action_show))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(stringResource(id = R.string.network_open_description))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConnect(if (isSecure) password else null) }) {
                Text(stringResource(id = R.string.action_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        }
    )
}
