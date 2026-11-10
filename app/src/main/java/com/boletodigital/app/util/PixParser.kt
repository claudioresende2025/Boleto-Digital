package com.boletodigital.app.util

/**
 * Parser de payload EMV PIX (QR / Copia e Cola).
 * Tag 54 = valor da transação.
 */
object PixParser {

    private val tag54Regex = Regex("""54(\d{2})([0-9]+(?:\.[0-9]+)?)""")

    fun parecePix(texto: String): Boolean {
        val t = texto.trim()
        return t.startsWith("000201") || t.contains("BR.GOV.BCB.PIX", ignoreCase = true)
    }

    fun extrairValor(payload: String): Double? {
        val limpo = payload.trim()
        if (limpo.isEmpty()) return null

        // 1) TLV no nível raiz
        lerTlv(limpo)?.get("54")?.let { bruto ->
            bruto.replace(',', '.').toDoubleOrNull()?.let { return it }
        }

        // 2) Busca recursiva em templates aninhados (26, 27, etc.)
        valorEmTlvRecursivo(limpo)?.let { return it }

        // 3) Regex direto no payload (fallback se o TLV estiver truncado/sujo)
        tag54Regex.find(limpo)?.let { match ->
            val len = match.groupValues[1].toIntOrNull() ?: return@let
            val bruto = match.groupValues[2]
            if (bruto.length >= len.coerceAtMost(bruto.length)) {
                bruto.take(len).replace(',', '.').toDoubleOrNull()?.let { return it }
            }
            bruto.replace(',', '.').toDoubleOrNull()?.let { return it }
        }

        return null
    }

    private fun valorEmTlvRecursivo(payload: String): Double? {
        val mapa = lerTlv(payload) ?: return null
        mapa["54"]?.replace(',', '.')?.toDoubleOrNull()?.let { return it }
        for ((tag, value) in mapa) {
            // Templates/merchant account costumam ser TLV aninhado
            if (tag.toIntOrNull() != null && value.length >= 6 && value.take(2).all { it.isDigit() }) {
                valorEmTlvRecursivo(value)?.let { return it }
            }
        }
        return null
    }

    fun lerTlv(payload: String): Map<String, String>? {
        if (payload.length < 4) return null
        val result = linkedMapOf<String, String>()
        var i = 0
        while (i + 4 <= payload.length) {
            val tag = payload.substring(i, i + 2)
            if (!tag.all { it.isDigit() }) break
            val len = payload.substring(i + 2, i + 4).toIntOrNull() ?: break
            i += 4
            if (len < 0 || i + len > payload.length) break
            val value = payload.substring(i, i + len)
            result[tag] = value
            i += len
        }
        return result.ifEmpty { null }
    }
}
