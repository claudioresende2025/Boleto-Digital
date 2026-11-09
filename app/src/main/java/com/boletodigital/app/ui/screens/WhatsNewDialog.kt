package com.boletodigital.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier as ComposeModifier

@Composable
fun WhatsNewDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova versão $versionName",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Atualização com novas formas de digitalizar seu boleto/fatura.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("• Ler boleto pela câmera (QR PIX)", style = MaterialTheme.typography.bodyMedium)
                Text("• Importar boleto ou fatura em PDF", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "• Gerar automaticamente os meses seguintes até a data que você escolher",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(ComposeModifier.height(4.dp))
                Text(
                    "Instale este APK por cima da versão anterior para manter seus boletos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = ComposeModifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("Atualizado — vamos lá")
            }
        }
    )
}
