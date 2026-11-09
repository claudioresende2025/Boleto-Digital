package com.boletodigital.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boletodigital.app.update.RemoteUpdate
import androidx.compose.ui.Modifier as ComposeModifier

@Composable
fun UpdateAvailableDialog(
    update: RemoteUpdate,
    baixando: Boolean,
    progresso: Int,
    onAtualizar: () -> Unit,
    onDepois: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!baixando) onDepois() },
        title = {
            Text(
                text = "Nova versão ${update.versionName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Há uma atualização disponível. Toque em Atualizar para baixar e instalar sem copiar o APK manualmente.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (update.notes.isNotBlank()) {
                    Text(update.notes, style = MaterialTheme.typography.bodySmall)
                }
                if (baixando) {
                    Spacer(ComposeModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progresso / 100f },
                        modifier = ComposeModifier.fillMaxWidth()
                    )
                    Text("Baixando… $progresso%", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAtualizar,
                enabled = !baixando,
                modifier = ComposeModifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(if (baixando) "Baixando…" else "Atualizar agora")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDepois,
                enabled = !baixando,
                modifier = ComposeModifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("Depois")
            }
        }
    )
}
