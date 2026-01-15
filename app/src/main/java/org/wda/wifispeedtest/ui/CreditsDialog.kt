package org.wda.wifispeedtest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.wda.wifispeedtest.R

/**
 * Affiche la boîte de dialogue "À propos & Crédits".
 *
 * @param show True pour afficher la boîte de dialogue, false pour la cacher.
 * @param onDismiss La fonction à appeler pour fermer la boîte de dialogue.
 * @param onShowChangelog La fonction à appeler pour afficher l'historique des versions.
 */
@Composable
fun CreditsDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onShowChangelog: () -> Unit
) {
    if (!show) return

    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.about_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val creditsAnnotated = buildAnnotatedString {
                    append(stringResource(id = R.string.about_developed_by))
                    append(" ")
                    pushLink(link = androidx.compose.ui.text.LinkAnnotation.Url(url = "https://mathieu.charreyre.net"))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append(stringResource(id = R.string.about_developer_name))
                    }
                    pop()
                    append(" ")
                    append(stringResource(id = R.string.about_for_organization))
                    append(" ")
                    pushLink(link = androidx.compose.ui.text.LinkAnnotation.Url(url = "https://wda-fr.org"))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append(stringResource(id = R.string.about_wda_organization_name))
                    }
                    pop()
                    append(".")
                }

                Text(
                    text = creditsAnnotated,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f))
                )

                Text(stringResource(id = R.string.about_license_info))

                Spacer(modifier = Modifier.height(8.dp))

                val tutorialAnnotated = buildAnnotatedString {
                    append(stringResource(id = R.string.about_read_tutorial_prefix))
                    append(" ")
                    pushLink(link = androidx.compose.ui.text.LinkAnnotation.Url(url = "https://forum.wda-fr.org/viewtopic.php?t=2463"))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append(stringResource(id = R.string.about_tutorial_link_text))
                    }
                    pop()
                    append(" ")
                    append(stringResource(id = R.string.about_on_forum_suffix))
                    append(" ")
                    pushLink(link = androidx.compose.ui.text.LinkAnnotation.Url(url = "https://forum.wda-fr.org"))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append(stringResource(id = R.string.about_forum_link_text))
                    }
                    pop()
                    append(".")
                }

                Text(
                    text = tutorialAnnotated,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lien pour afficher le changelog
                Text(
                    text = stringResource(id = R.string.action_show_changelog),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowChangelog() },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(stringResource(id = R.string.about_support_association), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { uriHandler.openUri("https://www.helloasso.com/associations/association-wda/formulaires/1") }) {
                            Text(stringResource(id = R.string.about_donations_helloasso))
                        }
                        TextButton(onClick = { uriHandler.openUri("https://www.paypal.com/donate/?hosted_button_id=DZRY58WUANTAJ") }) {
                            Text(stringResource(id = R.string.about_donations_paypal))
                        }
                        TextButton(onClick = { uriHandler.openUri("https://wda-fr.org/index.php?page=help") }) {
                            Text(stringResource(id = R.string.about_donations_info))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_close))
            }
        }
    )
}
