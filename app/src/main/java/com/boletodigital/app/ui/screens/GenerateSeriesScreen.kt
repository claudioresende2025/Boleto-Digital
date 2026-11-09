package com.boletodigital.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boletodigital.app.data.LeituraBoleto
import com.boletodigital.app.util.emReais
import com.boletodigital.app.util.paraDoubleOuPadrao
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.Modifier as ComposeModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateSeriesScreen(
    leitura: LeituraBoleto,
    onBack: () -> Unit,
    onGerar: (
        titulo: String,
        mesInicio: YearMonth,
        mesFim: YearMonth,
        diaVencimento: Int,
        valorCheio: Double,
        descontoPix: Double,
        reutilizarPix: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val locale = Locale("pt", "BR")
    val mesFmt = DateTimeFormatter.ofPattern("MM/yyyy", locale)
    val mesInicioPadrao = YearMonth.from(leitura.dataReferencia)
    val mesFimPadrao = mesInicioPadrao.plusYears(1)

    var titulo by remember { mutableStateOf(leitura.tituloSugerido) }
    var inicioTexto by remember { mutableStateOf(mesInicioPadrao.format(mesFmt)) }
    var fimTexto by remember { mutableStateOf(mesFimPadrao.format(mesFmt)) }
    var diaTexto by remember { mutableStateOf("10") }
    var valorTexto by remember {
        mutableStateOf((leitura.valorDetectado ?: 160.0).toString().replace('.', ','))
    }
    var descontoTexto by remember { mutableStateOf("10") }
    var reutilizarPix by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val inicioParsed = runCatching { YearMonth.parse(inicioTexto.trim(), mesFmt) }.getOrNull()
    val fimParsed = runCatching { YearMonth.parse(fimTexto.trim(), mesFmt) }.getOrNull()
    val quantidade = if (inicioParsed != null && fimParsed != null) {
        val a = if (inicioParsed <= fimParsed) inicioParsed else fimParsed
        val b = if (inicioParsed <= fimParsed) fimParsed else inicioParsed
        (b.year - a.year) * 12 + (b.monthValue - a.monthValue) + 1
    } else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerar boletos futuros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = ComposeModifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Boleto lido com sucesso. Defina até qual mês deseja gerar automaticamente.",
                style = MaterialTheme.typography.bodyMedium
            )

            val refMes = leitura.dataReferencia.month.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase() }
            Text(
                "Referência da leitura: $refMes/${leitura.dataReferencia.year}",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            if (leitura.valorDetectado != null) {
                Text("Valor identificado no PIX: ${leitura.valorDetectado.emReais()}")
            }
            Text(
                "PIX capturado (${leitura.codigoPix.length} caracteres).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                modifier = ComposeModifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = inicioTexto,
                onValueChange = { inicioTexto = it },
                label = { Text("Mês inicial (MM/aaaa)") },
                supportingText = { Text("Ex.: 11/2026 — mês do boleto lido") },
                modifier = ComposeModifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = fimTexto,
                onValueChange = { fimTexto = it },
                label = { Text("Gerar até o mês (MM/aaaa)") },
                supportingText = { Text("Ex.: 11/2027 — cria todos os meses até esta data") },
                modifier = ComposeModifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = diaTexto,
                onValueChange = { diaTexto = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("Dia do vencimento") },
                supportingText = { Text("Padrão: dia 10") },
                modifier = ComposeModifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = valorTexto,
                onValueChange = { valorTexto = it },
                label = { Text("Valor cheio (R$)") },
                modifier = ComposeModifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = descontoTexto,
                onValueChange = { descontoTexto = it },
                label = { Text("Desconto PIX antecipado (R$)") },
                modifier = ComposeModifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = reutilizarPix, onCheckedChange = { reutilizarPix = it })
                Text(
                    "Reutilizar o mesmo PIX nos meses seguintes (só se for PIX estático)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!reutilizarPix) {
                Text(
                    "O PIX lido fica no mês inicial. Nos meses seguintes você atualiza o código quando receber o boleto em papel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            }

            if (quantidade > 0) {
                Text(
                    "Serão criados até $quantidade boleto(s).",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (erro != null) {
                Text(erro!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(ComposeModifier.height(8.dp))
            Button(
                onClick = {
                    val inicio = runCatching { YearMonth.parse(inicioTexto.trim(), mesFmt) }.getOrNull()
                    val fim = runCatching { YearMonth.parse(fimTexto.trim(), mesFmt) }.getOrNull()
                    if (inicio == null || fim == null) {
                        erro = "Informe o mês inicial e final no formato MM/aaaa."
                        return@Button
                    }
                    val dia = diaTexto.toIntOrNull() ?: 10
                    if (dia !in 1..31) {
                        erro = "Dia de vencimento inválido."
                        return@Button
                    }
                    erro = null
                    onGerar(
                        titulo,
                        inicio,
                        fim,
                        dia,
                        valorTexto.paraDoubleOuPadrao(160.0),
                        descontoTexto.paraDoubleOuPadrao(10.0),
                        reutilizarPix
                    )
                    Toast.makeText(context, "Gerando boletos…", Toast.LENGTH_SHORT).show()
                },
                modifier = ComposeModifier.fillMaxWidth()
            ) {
                Text("Gerar boletos automaticamente")
            }
        }
    }
}
