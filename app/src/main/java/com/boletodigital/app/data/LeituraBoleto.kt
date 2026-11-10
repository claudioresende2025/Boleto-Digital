package com.boletodigital.app.data

import java.time.LocalDate

data class LeituraBoleto(
    val codigoPix: String,
    val valorDetectado: Double?,
    val dataReferencia: LocalDate,
    val tituloSugerido: String = "Mensalidade",
    val descontoSugerido: Double = 0.0
)
