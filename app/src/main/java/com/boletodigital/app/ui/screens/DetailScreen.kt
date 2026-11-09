package com.boletodigital.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boletodigital.app.data.Boleto
import com.boletodigital.app.util.emReais
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    boleto: Boleto,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTogglePago: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val hoje = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    var confirmarExclusao by remember { mutableStateOf(false) }
    val dias = ChronoUnit.DAYS.between(hoje, boleto.dataVencimento())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(boleto.titulo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { confirmarExclusao = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(boleto.mesReferencia(), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    boleto.valorAtual(hoje).emReais(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text("Vencimento: ${boleto.dataVencimento().format(formatter)}")
                Text("Valor cheio: ${boleto.valorCheio.emReais()}")
                Text("Com PIX antecipado: ${boleto.valorComDesconto().emReais()} (−${boleto.descontoPix.emReais()})")
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        boleto.pago && boleto.pagoComDesconto -> "Pago com desconto"
                        boleto.pago -> "Pago no valor cheio"
                        boleto.descontoAindaValido(hoje) ->
                            if (dias == 0L) "Último dia de desconto: pague hoje via PIX"
                            else "Desconto válido por mais ${dias} dia(s)"
                        dias < 0 -> "Vencido — valor cheio ${boleto.valorCheio.emReais()}"
                        else -> "Desconto encerrado — valor cheio ${boleto.valorCheio.emReais()}"
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        boleto.pago -> MaterialTheme.colorScheme.tertiary
                        boleto.descontoAindaValido(hoje) -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text("PIX Copia e Cola", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (boleto.codigoPix.isBlank()) {
                    Text(
                        "Cadastre o código PIX deste boleto para copiar e pagar no app do banco.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                        Text("Adicionar código PIX")
                    }
                } else {
                    Text(
                        text = boleto.codigoPix,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            copiarPix(context, boleto.codigoPix)
                            Toast.makeText(
                                context,
                                "PIX copiado! Abra o app do banco e cole no pagamento PIX.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text(
                            text = "  Copiar PIX e pagar no banco"
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!boleto.pago) {
                    Button(
                        onClick = { onTogglePago(true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Marcar como pago")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onTogglePago(false) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Desmarcar pagamento")
                    }
                }
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir boleto?") },
            text = { Text("Essa ação remove o boleto digital e o código PIX salvo neste aparelho.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarExclusao = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun copiarPix(context: Context, pix: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("PIX", pix))
}
