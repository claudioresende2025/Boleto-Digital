package com.boletodigital.app.util

import com.boletodigital.app.data.LeituraBoleto
import java.time.LocalDate
import java.time.YearMonth

object LeituraFactory {
    fun aPartirDoPix(payload: String, titulo: String = "Mensalidade"): LeituraBoleto {
        val limpo = payload.trim()
        val valorPix = PixParser.extrairValor(limpo)
        return LeituraBoleto(
            codigoPix = limpo,
            // Usa exatamente o valor lido no boleto/PIX (sem forçar 160)
            valorDetectado = valorPix,
            descontoSugerido = 0.0,
            dataReferencia = YearMonth.from(LocalDate.now()).atDay(10),
            tituloSugerido = titulo
        )
    }
}
