package com.boletodigital.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boletodigital.app.data.Boleto
import com.boletodigital.app.util.emReais
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    boletos: List<Boleto>,
    onLerBoleto: () -> Unit,
    onImportarPdf: () -> Unit,
    onAddManual: () -> Unit,
    onOpen: (Long) -> Unit,
    onExcluirSelecionados: (Set<Long>) -> Unit
) {
    val hoje = LocalDate.now()
    val pendentes = boletos.filter { !it.pago }
    val economiaPossivel = pendentes
        .filter { it.descontoAindaValido(hoje) }
        .sumOf { it.descontoPix }
    var mostrarOpcoes by remember { mutableStateOf(false) }
    var modoSelecao by remember { mutableStateOf(false) }
    var selecionados by remember { mutableStateOf(setOf<Long>()) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            if (modoSelecao) {
                TopAppBar(
                    title = {
                        Text(
                            if (selecionados.isEmpty()) "Selecionar boletos"
                            else "${selecionados.size} selecionado(s)"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            modoSelecao = false
                            selecionados = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar seleção")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selecionados = if (selecionados.size == boletos.size) {
                                emptySet()
                            } else {
                                boletos.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Selecionar todos")
                        }
                        IconButton(
                            onClick = { confirmarExclusao = true },
                            enabled = selecionados.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir selecionados")
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
        },
        floatingActionButton = {
            if (!modoSelecao) {
                Column(horizontalAlignment = Alignment.End) {
                    if (boletos.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                modoSelecao = true
                                selecionados = emptySet()
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Outlined.Checklist, contentDescription = "Selecionar para excluir")
                        }
                    }
                    FloatingActionButton(
                        onClick = { mostrarOpcoes = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Novo boleto")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (!modoSelecao) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Column {
                        Text(
                            text = "Boleto Digital",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Lista em ordem: do vencimento mais próximo ao mais distante.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        if (economiaPossivel > 0) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Desconto disponível agora: ${economiaPossivel.emReais()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            if (boletos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Nenhum boleto digital ainda",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Toque em + → Ler boleto, aponte para o QR PIX e escolha até qual mês gerar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(boletos, key = { it.id }) { boleto ->
                        BoletoCard(
                            boleto = boleto,
                            hoje = hoje,
                            modoSelecao = modoSelecao,
                            selecionado = boleto.id in selecionados,
                            onClick = {
                                if (modoSelecao) {
                                    selecionados = if (boleto.id in selecionados) {
                                        selecionados - boleto.id
                                    } else {
                                        selecionados + boleto.id
                                    }
                                } else {
                                    onOpen(boleto.id)
                                }
                            },
                            onLongClick = {
                                if (!modoSelecao) {
                                    modoSelecao = true
                                    selecionados = setOf(boleto.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarOpcoes) {
        ModalBottomSheet(
            onDismissRequest = { mostrarOpcoes = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    "Adicionar boletos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                OpcaoAdd(
                    icone = Icons.Default.DocumentScanner,
                    titulo = "Ler boleto",
                    descricao = "Abre a câmera, lê o QR PIX e gera os meses seguintes"
                ) {
                    mostrarOpcoes = false
                    onLerBoleto()
                }
                Spacer(Modifier.height(8.dp))
                OpcaoAdd(
                    icone = Icons.Default.PictureAsPdf,
                    titulo = "Importar PDF / fatura",
                    descricao = "Puxa o boleto ou fatura em PDF e extrai o PIX automaticamente"
                ) {
                    mostrarOpcoes = false
                    onImportarPdf()
                }
                Spacer(Modifier.height(8.dp))
                OpcaoAdd(
                    icone = Icons.Default.Edit,
                    titulo = "Cadastro manual",
                    descricao = "Digitar título, valor e PIX sem usar a câmera"
                ) {
                    mostrarOpcoes = false
                    onAddManual()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir ${selecionados.size} boleto(s)?") },
            text = { Text("Os boletos selecionados serão removidos deste aparelho.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ids = selecionados
                        confirmarExclusao = false
                        onExcluirSelecionados(ids)
                        modoSelecao = false
                        selecionados = emptySet()
                    }
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun OpcaoAdd(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descricao: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.size(14.dp))
        Column {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoletoCard(
    boleto: Boleto,
    hoje: LocalDate,
    modoSelecao: Boolean,
    selecionado: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    val dias = ChronoUnit.DAYS.between(hoje, boleto.dataVencimento())
    val status = when {
        boleto.pago -> "Pago"
        boleto.descontoAindaValido(hoje) -> "Desconto até o dia 10"
        dias < 0 -> "Vencido"
        else -> "Valor cheio"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selecionado) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (modoSelecao) {
                Checkbox(
                    checked = selecionado,
                    onCheckedChange = { onClick() }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    boleto.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    boleto.mesReferencia(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            if (!modoSelecao) {
                Icon(
                    imageVector = if (boleto.pago) Icons.Filled.CheckCircle else Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = if (boleto.pago) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Vencimento", style = MaterialTheme.typography.labelMedium)
                Text(boleto.dataVencimento().format(formatter), fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("A pagar", style = MaterialTheme.typography.labelMedium)
                Text(
                    boleto.valorAtual(hoje).emReais(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                boleto.pago -> MaterialTheme.colorScheme.tertiary
                boleto.descontoAindaValido(hoje) -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        )
        if (boleto.codigoPix.isBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "PIX ainda não cadastrado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
