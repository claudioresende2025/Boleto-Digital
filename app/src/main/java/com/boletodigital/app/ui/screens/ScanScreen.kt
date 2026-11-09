package com.boletodigital.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.boletodigital.app.data.LeituraBoleto
import com.boletodigital.app.util.PixParser
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.ui.Modifier as ComposeModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onLido: (LeituraBoleto) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pixManual by remember { mutableStateOf("") }
    var processando by remember { mutableStateOf(false) }
    val jaLeu = remember { AtomicBoolean(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamera = granted
        if (!granted) {
            Toast.makeText(context, "Permissão da câmera necessária para ler o boleto.", Toast.LENGTH_LONG).show()
        }
    }

    fun concluirComPayload(payload: String) {
        if (!jaLeu.compareAndSet(false, true)) return
        processando = true
        val limpo = payload.trim()
        val valor = PixParser.extrairValor(limpo)
        val valorCheio = when {
            valor == null -> 160.0
            valor <= 150.0 -> valor + 10.0
            else -> valor
        }
        val referencia = YearMonth.from(LocalDate.now()).atDay(10)
        onLido(
            LeituraBoleto(
                codigoPix = limpo,
                valorDetectado = valorCheio,
                dataReferencia = referencia,
                tituloSugerido = "Mensalidade"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ler boleto") },
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
        ) {
            Text(
                text = "Aponte a câmera para o QR Code PIX do boleto em papel.",
                modifier = ComposeModifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            Box(
                modifier = ComposeModifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasCamera) {
                    AndroidView(
                        modifier = ComposeModifier.fillMaxSize(),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val analyzerExecutor = Executors.newSingleThreadExecutor()
                            val scanner = BarcodeScanning.getClient(
                                BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(
                                        Barcode.FORMAT_QR_CODE,
                                        Barcode.FORMAT_AZTEC,
                                        Barcode.FORMAT_DATA_MATRIX
                                    )
                                    .build()
                            )

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setResolutionSelector(
                                        ResolutionSelector.Builder()
                                            .setResolutionStrategy(
                                                ResolutionStrategy(
                                                    Size(1280, 720),
                                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                                )
                                            )
                                            .build()
                                    )
                                    .build()
                                    .also { imageAnalysis ->
                                        imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage == null || jaLeu.get()) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    val raw = barcodes.firstOrNull { barcode ->
                                                        val v = barcode.rawValue.orEmpty()
                                                        PixParser.parecePix(v) || v.length > 40
                                                    }?.rawValue
                                                    if (!raw.isNullOrBlank()) {
                                                        concluirComPayload(raw)
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        }
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(ctx, "Não foi possível abrir a câmera.", Toast.LENGTH_LONG).show()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("A câmera ainda não foi liberada.", fontWeight = FontWeight.SemiBold)
                        Spacer(ComposeModifier.height(12.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Permitir câmera")
                        }
                    }
                }
            }

            Column(modifier = ComposeModifier.padding(16.dp)) {
                Text(
                    "Se o QR não ler, cole o PIX Copia e Cola abaixo:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(ComposeModifier.height(8.dp))
                OutlinedTextField(
                    value = pixManual,
                    onValueChange = { pixManual = it },
                    modifier = ComposeModifier
                        .fillMaxWidth()
                        .height(120.dp),
                    minLines = 3,
                    label = { Text("PIX Copia e Cola") }
                )
                Spacer(ComposeModifier.height(12.dp))
                Button(
                    onClick = {
                        if (pixManual.isBlank()) {
                            Toast.makeText(context, "Cole o código PIX ou leia o QR.", Toast.LENGTH_SHORT).show()
                        } else {
                            concluirComPayload(pixManual)
                        }
                    },
                    enabled = !processando,
                    modifier = ComposeModifier.fillMaxWidth()
                ) {
                    Text("Usar este PIX e continuar")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        if (!hasCamera) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onDispose { }
    }
}
