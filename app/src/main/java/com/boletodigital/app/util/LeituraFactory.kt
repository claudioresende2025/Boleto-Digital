package com.boletodigital.app.util

import com.boletodigital.app.data.LeituraBoleto
import java.time.LocalDate
import java.time.YearMonth

object LeituraFactory {
    fun aPartirDoPix(payload: String, titulo: String = "Mensalidade"): LeituraBoleto {
        val limpo = payload.trim()
        val valor = PixParser.extrairValor(limpo)
        val valorCheio = when {
            valor == null -> 160.0
            valor <= 150.0 -> valor + 10.0
            else -> valor
        }
        return LeituraBoleto(
            codigoPix = limpo,
            valorDetectado = valorCheio,
            dataReferencia = YearMonth.from(LocalDate.now()).atDay(10),
            tituloSugerido = titulo
        )
    }
}
