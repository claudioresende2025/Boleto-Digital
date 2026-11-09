package com.boletodigital.app.util

/**
 * Parser simples de payload EMV PIX (QR / Copia e Cola).
 * Tag 54 = valor; demais campos ficam disponíveis se precisarmos depois.
 */
object PixParser {

    fun parecePix(texto: String): Boolean {
        val t = texto.trim()
        return t.startsWith("000201") || t.contains("BR.GOV.BCB.PIX", ignoreCase = true)
    }

    fun extrairValor(payload: String): Double? {
        val mapa = lerTlv(payload.trim()) ?: return null
        val bruto = mapa["54"] ?: return null
        return bruto.replace(',', '.').toDoubleOrNull()
    }

    fun lerTlv(payload: String): Map<String, String>? {
        if (payload.length < 4) return null
        val result = linkedMapOf<String, String>()
        var i = 0
        while (i + 4 <= payload.length) {
            val tag = payload.substring(i, i + 2)
            val len = payload.substring(i + 2, i + 4).toIntOrNull() ?: return result.ifEmpty { null }
            i += 4
            if (i + len > payload.length) return result.ifEmpty { null }
            val value = payload.substring(i, i + len)
            result[tag] = value
            i += len
        }
        return result
    }
}
