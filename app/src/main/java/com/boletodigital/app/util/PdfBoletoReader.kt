package com.boletodigital.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object PdfBoletoReader {

    private val pixRegex = Regex("""000201[0-9A-Za-z. *\-_$/+%:]*BR\.GOV\.BCB\.PIX[0-9A-Za-z. *\-_$/+%:]*""", RegexOption.IGNORE_CASE)
    private val pixStartRegex = Regex("""00020126[0-9A-Za-z]+""")

    suspend fun extrairPix(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri).orEmpty()
        when {
            mime.startsWith("image/") -> extrairDeImagem(context, uri)
            mime == "application/pdf" || uri.toString().lowercase().endsWith(".pdf") -> {
                extrairTextoEmbutido(context, uri)
                    ?: extrairQrDoPdf(context, uri)
            }
            else -> {
                extrairTextoEmbutido(context, uri)
                    ?: extrairQrDoPdf(context, uri)
                    ?: extrairDeImagem(context, uri)
            }
        }
    }

    private fun extrairTextoEmbutido(context: Context, uri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val latin = runCatching { String(bytes, Charsets.ISO_8859_1) }.getOrNull().orEmpty()
        val utf8 = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull().orEmpty()
        return encontrarPixEmTexto(latin) ?: encontrarPixEmTexto(utf8)
    }

    fun encontrarPixEmTexto(texto: String): String? {
        pixRegex.find(texto)?.value?.let { candidato ->
            val limpo = limparPayload(candidato)
            if (PixParser.parecePix(limpo)) return limpo
        }
        // Fallback: trecho a partir de 000201 até ~500 chars imprimíveis
        val idx = texto.indexOf("000201")
        if (idx >= 0) {
            val bruto = texto.substring(idx, minOf(texto.length, idx + 600))
            val limpo = limparPayload(bruto)
            if (PixParser.parecePix(limpo) && limpo.length >= 50) return limpo
        }
        pixStartRegex.find(texto)?.value?.let { parcial ->
            val limpo = limparPayload(parcial)
            if (PixParser.parecePix(limpo)) return limpo
        }
        return null
    }

    private fun limparPayload(raw: String): String {
        // Mantém apenas caracteres típicos do EMV PIX
        return raw.filter { ch ->
            ch.code in 32..126
        }.trim()
            .let { s ->
                // Corta lixo após o CRC (últimos 4 hex depois do "6304")
                val crc = Regex("""6304[0-9A-Fa-f]{4}""").find(s)
                if (crc != null) s.substring(0, crc.range.last + 1) else s
            }
    }

    private suspend fun extrairQrDoPdf(context: Context, uri: Uri): String? {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val scale = 3
                        val bitmap = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val pix = lerQrNaBitmap(bitmap)
                        bitmap.recycle()
                        if (!pix.isNullOrBlank()) return pix
                    }
                }
            }
        }
        return null
    }

    private suspend fun extrairDeImagem(context: Context, uri: Uri): String? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)
        } ?: return null
        return try {
            lerQrNaBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun lerQrNaBitmap(bitmap: Bitmap): String? {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_DATA_MATRIX
                )
                .build()
        )
        return suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull { barcode ->
                        val v = barcode.rawValue.orEmpty()
                        PixParser.parecePix(v) || v.length > 40
                    }?.rawValue
                    cont.resume(raw?.let { limparPayload(it) })
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    }
}
