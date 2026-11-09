package com.boletodigital.app.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.boletodigital.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class RemoteUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String
)

object AppUpdate {
    private const val OWNER = "claudioresende2025"
    private const val REPO = "Boleto-Digital"
    private const val RELEASES_URL =
        "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    suspend fun verificarAtualizacao(): RemoteUpdate? = withContext(Dispatchers.IO) {
        val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BoletoDigital-Updater")
        }
        try {
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val tag = json.optString("tag_name").removePrefix("v")
            val releaseBody = json.optString("body")
            val versionCode = Regex("""VERSION_CODE\s*[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)
                .find(releaseBody)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?: tag.substringBefore('-').toIntOrNull()
                ?: return@withContext null

            if (versionCode <= BuildConfig.VERSION_CODE) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return@withContext null

            RemoteUpdate(
                versionCode = versionCode,
                versionName = tag.ifBlank { versionCode.toString() },
                apkUrl = apkUrl,
                notes = releaseBody
                    .lines()
                    .filterNot { it.contains("VERSION_CODE", ignoreCase = true) }
                    .joinToString("\n")
                    .trim()
                    .ifBlank { "Nova versão disponível com melhorias." }
            )
        } finally {
            conn.disconnect()
        }
    }

    suspend fun baixarApk(context: Context, update: RemoteUpdate, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "updates").apply { mkdirs() }
            val outFile = File(dir, "BoletoDigital-update.apk")
            if (outFile.exists()) outFile.delete()

            val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BoletoDigital-Updater")
                setRequestProperty("Accept", "application/octet-stream")
            }
            try {
                if (conn.responseCode !in 200..299) {
                    error("Falha ao baixar APK (${conn.responseCode})")
                }
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
                conn.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var lido = 0L
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            lido += n
                            if (total > 0) {
                                onProgress(((lido * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                        output.flush()
                    }
                }
                onProgress(100)
                outFile
            } finally {
                conn.disconnect()
            }
        }

    fun podeInstalarPacotes(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun abrirPermissaoInstalar(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        }
    }

    fun instalarApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
