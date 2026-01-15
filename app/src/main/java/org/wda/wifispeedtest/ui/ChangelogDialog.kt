package org.wda.wifispeedtest.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.wda.wifispeedtest.R

/**
 * Affiche la boîte de dialogue de l'historique des versions (changelog).
 *
 * @param show True pour afficher la boîte de dialogue, false pour la cacher.
 * @param onDismiss La fonction à appeler pour fermer la boîte de dialogue.
 */
@Composable
fun ChangelogDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show) return

    val changelogEntries = stringArrayResource(id = R.array.changelog_entries)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.changelog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                changelogEntries.forEach {
                    Text(it)
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
