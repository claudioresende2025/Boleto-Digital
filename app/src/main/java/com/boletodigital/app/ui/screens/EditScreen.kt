package com.boletodigital.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boletodigital.app.data.Boleto
import com.boletodigital.app.util.paraDoubleOuPadrao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    boleto: Boleto?,
    onBack: () -> Unit,
    onSave: (
        id: Long,
        titulo: String,
        vencimento: LocalDate,
        valorCheio: Double,
        descontoPix: Double,
        codigoPix: String
    ) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    val inicial = boleto ?: Boleto(
        titulo = "Mensalidade",
        vencimento = Boleto.proximoVencimentoDia10().toString(),
        valorCheio = 160.0,
        descontoPix = 10.0
    )

    var titulo by remember(inicial.id) { mutableStateOf(inicial.titulo) }
    var vencimentoTexto by remember(inicial.id) {
        mutableStateOf(inicial.dataVencimento().format(formatter))
    }
    var valorCheio by remember(inicial.id) { mutableStateOf(inicial.valorCheio.toString().replace('.', ',')) }
    var desconto by remember(inicial.id) { mutableStateOf(inicial.descontoPix.toString().replace('.', ',')) }
    var codigoPix by remember(inicial.id) { mutableStateOf(inicial.codigoPix) }
    var erro by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inicial.id == 0L) "Novo boleto" else "Editar boleto") },
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
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Cole aqui o PIX Copia e Cola do boleto em papel. Depois é só abrir o app e copiar para o banco.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = vencimentoTexto,
                onValueChange = { vencimentoTexto = it },
                label = { Text("Vencimento (dd/MM/aaaa)") },
                supportingText = { Text("Padrão: dia 10 de cada mês") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = valorCheio,
                onValueChange = { valorCheio = it },
                label = { Text("Valor cheio (R$)") },
                supportingText = { Text("Ex.: 160") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = desconto,
                onValueChange = { desconto = it },
                label = { Text("Desconto PIX antecipado (R$)") },
                supportingText = { Text("Ex.: 10 → paga 150 antes do vencimento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = codigoPix,
                onValueChange = { codigoPix = it },
                label = { Text("Código PIX (Copia e Cola)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                minLines = 4
            )

            if (erro != null) {
                Text(erro!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val data = runCatching { LocalDate.parse(vencimentoTexto.trim(), formatter) }.getOrNull()
                    if (data == null) {
                        erro = "Data de vencimento inválida. Use dd/MM/aaaa."
                        return@Button
                    }
                    erro = null
                    onSave(
                        inicial.id,
                        titulo,
                        data,
                        valorCheio.paraDoubleOuPadrao(160.0),
                        desconto.paraDoubleOuPadrao(10.0),
                        codigoPix
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar boleto digital")
            }
        }
    }
}
