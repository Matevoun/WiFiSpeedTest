package org.wda.wifispeedtest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wda.wifispeedtest.BuildConfig
import org.wda.wifispeedtest.R

@Composable
fun HomeScreen() {
    val uriHandler = LocalUriHandler.current
    Box(modifier = Modifier.fillMaxSize()) {
        // Message principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(id = R.string.scan_start_message),
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center
            )
        }
        // Version et crédits compacts
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = try { "v${BuildConfig.VERSION_NAME}" } catch (_: Exception) { "v0.9" },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            // Ligne compacte pour les crédits
            val compactCredits = buildAnnotatedString {
                append(stringResource(id = R.string.about_developed_by))
                pushStringAnnotation(tag = "dev_url", annotation = "https://mathieu.charreyre.net")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append(" ")
                    append(stringResource(id = R.string.about_developer_name))
                }
                pop()
                append(" ")
                append(stringResource(id = R.string.about_for_organization))
                append(" ")
                pushStringAnnotation(tag = "wda_url", annotation = "https://wda-fr.org")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append(stringResource(id = R.string.about_wda_organization_name))
                }
                pop()
                append(" (CC BY-NC-SA)")
            }
            Text(
                text = compactCredits,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                onTextLayout = {},
            )
            // Ligne compacte pour le don
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.action_support),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                )

                // Button HelloAsso
                TextButton(onClick = { uriHandler.openUri("https://www.helloasso.com/associations/association-wda") }) {
                    Text(stringResource(id = R.string.about_donations_helloasso), style = MaterialTheme.typography.bodySmall)
                }

                // Button PayPal
                TextButton(onClick = { uriHandler.openUri("https://paypal.me/wdassos?country.x=FR&locale.x=fr_FR") }) {
                    Text(stringResource(id = R.string.about_donations_paypal), style = MaterialTheme.typography.bodySmall)
                }

                // Lien "Plus d'infos" vers la page aide (petit, discret)
                Text(
                    text = stringResource(id = R.string.action_more_info),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable { uriHandler.openUri("https://wda-fr.org/index.php?page=help") }
                )
            }
        }
    }
}
