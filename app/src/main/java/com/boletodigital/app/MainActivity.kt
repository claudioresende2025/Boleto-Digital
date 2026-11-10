package com.boletodigital.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.boletodigital.app.data.AppUpdatePrefs
import com.boletodigital.app.data.BoletoDatabase
import com.boletodigital.app.data.BoletoRepository
import com.boletodigital.app.ui.screens.DetailScreen
import com.boletodigital.app.ui.screens.EditScreen
import com.boletodigital.app.ui.screens.GenerateSeriesScreen
import com.boletodigital.app.ui.screens.HomeScreen
import com.boletodigital.app.ui.screens.ScanScreen
import com.boletodigital.app.ui.screens.UpdateAvailableDialog
import com.boletodigital.app.ui.screens.WhatsNewDialog
import com.boletodigital.app.ui.theme.BoletoDigitalTheme
import com.boletodigital.app.update.AppUpdate
import com.boletodigital.app.update.RemoteUpdate
import com.boletodigital.app.util.LeituraFactory
import com.boletodigital.app.util.PdfBoletoReader
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = BoletoRepository(BoletoDatabase.get(this).boletoDao())

        setContent {
            BoletoDigitalTheme {
                BoletoDigitalApp(repository, activity = this)
            }
        }
    }
}

@Composable
private fun BoletoDigitalApp(repository: BoletoRepository, activity: ComponentActivity) {
    val navController = rememberNavController()
    val viewModel: BoletoViewModel = viewModel(factory = BoletoViewModel.Factory(repository))
    val boletos by viewModel.boletos.collectAsState()
    val leitura by viewModel.leituraPendente.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importandoPdf by remember { mutableStateOf(false) }
    var mostrarNovidades by remember {
        mutableStateOf(AppUpdatePrefs.deveMostrarNovidades(context, BuildConfig.VERSION_CODE))
    }
    var updateDisponivel by remember { mutableStateOf<RemoteUpdate?>(null) }
    var baixandoUpdate by remember { mutableStateOf(false) }
    var progressoUpdate by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        runCatching { AppUpdate.verificarAtualizacao() }
            .onSuccess { remote -> updateDisponivel = remote }
    }

    fun processarArquivo(uri: Uri) {
        scope.launch {
            importandoPdf = true
            Toast.makeText(context, "Lendo PDF/fatura…", Toast.LENGTH_SHORT).show()
            val pix = runCatching { PdfBoletoReader.extrairPix(context, uri) }.getOrNull()
            importandoPdf = false
            if (pix.isNullOrBlank()) {
                Toast.makeText(
                    context,
                    "Não encontrei PIX neste arquivo. Tente outro PDF ou use a câmera.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                viewModel.definirLeitura(LeituraFactory.aPartirDoPix(pix, "Mensalidade"))
                navController.navigate("gerar")
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            processarArquivo(uri)
        }
    }

    // Prioridade: atualização remota (baixar/instalar). Novidades locais só se não houver update.
    val update = updateDisponivel
    if (update != null) {
        UpdateAvailableDialog(
            update = update,
            baixando = baixandoUpdate,
            progresso = progressoUpdate,
            onAtualizar = {
                scope.launch {
                    if (!AppUpdate.podeInstalarPacotes(context)) {
                        Toast.makeText(
                            context,
                            "Permita instalar apps deste app e toque em Atualizar de novo.",
                            Toast.LENGTH_LONG
                        ).show()
                        AppUpdate.abrirPermissaoInstalar(activity)
                        return@launch
                    }
                    baixandoUpdate = true
                    progressoUpdate = 0
                    runCatching {
                        val apk = AppUpdate.baixarApk(context, update) { progressoUpdate = it }
                        AppUpdate.instalarApk(context, apk)
                    }.onFailure {
                        Toast.makeText(
                            context,
                            "Falha na atualização: ${it.message ?: "erro desconhecido"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    baixandoUpdate = false
                }
            },
            onDepois = { updateDisponivel = null }
        )
    } else if (mostrarNovidades) {
        WhatsNewDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = {
                AppUpdatePrefs.marcarNovidadesVistas(context, BuildConfig.VERSION_CODE)
                mostrarNovidades = false
            }
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                boletos = boletos,
                onLerBoleto = { navController.navigate("scan") },
                onImportarPdf = {
                    if (importandoPdf) {
                        Toast.makeText(context, "Aguarde, ainda lendo o arquivo…", Toast.LENGTH_SHORT).show()
                    } else {
                        pdfLauncher.launch(arrayOf("application/pdf", "image/*"))
                    }
                },
                onAddManual = { navController.navigate("edit/0") },
                onOpen = { id -> navController.navigate("detail/$id") },
                onExcluirSelecionados = { ids ->
                    viewModel.excluirVarios(ids) { qtd ->
                        Toast.makeText(context, "$qtd boleto(s) excluído(s).", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        composable("scan") {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onLido = { lido ->
                    viewModel.definirLeitura(lido)
                    navController.navigate("gerar") {
                        popUpTo("scan") { inclusive = true }
                    }
                }
            )
        }
        composable("gerar") {
            val atual = leitura
            if (atual == null) {
                navController.popBackStack("home", inclusive = false)
                return@composable
            }
            GenerateSeriesScreen(
                leitura = atual,
                onBack = {
                    viewModel.limparLeitura()
                    navController.popBackStack()
                },
                onGerar = { titulo, inicio, fim, dia, valor, desconto, reutilizar ->
                    viewModel.gerarSerie(
                        titulo = titulo,
                        mesInicio = inicio,
                        mesFim = fim,
                        diaVencimento = dia,
                        valorCheio = valor,
                        descontoPix = desconto,
                        codigoPixLido = atual.codigoPix,
                        reutilizarPix = reutilizar
                    ) { qtd ->
                        Toast.makeText(
                            context,
                            if (qtd == 0) "Nenhum boleto novo (já existiam)."
                            else "$qtd boleto(s) gerado(s).",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val boletoFlow = remember(id) { viewModel.observar(id) }
            val boleto by boletoFlow.collectAsState(initial = null)
            val atual = boleto ?: return@composable

            DetailScreen(
                boleto = atual,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("edit/$id") },
                onTogglePago = { pago -> viewModel.marcarPago(atual, pago) },
                onDelete = {
                    viewModel.excluir(atual) {
                        navController.popBackStack("home", inclusive = false)
                    }
                }
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val existente = boletos.firstOrNull { it.id == id }
            EditScreen(
                boleto = existente,
                onBack = { navController.popBackStack() },
                onSave = { savedId, titulo, vencimento, valor, desconto, pix ->
                    viewModel.salvar(savedId, titulo, vencimento, valor, desconto, pix) { novoId ->
                        if (savedId == 0L) {
                            navController.navigate("detail/$novoId") {
                                popUpTo("home")
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}
