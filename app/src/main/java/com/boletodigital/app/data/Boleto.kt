package com.boletodigital.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Entity(tableName = "boletos")
data class Boleto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val vencimento: String,
    val valorCheio: Double = 160.0,
    val descontoPix: Double = 10.0,
    val codigoPix: String = "",
    val pago: Boolean = false,
    val pagoComDesconto: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis()
) {
    fun dataVencimento(): LocalDate = LocalDate.parse(vencimento)

    fun valorComDesconto(): Double = (valorCheio - descontoPix).coerceAtLeast(0.0)

    fun descontoAindaValido(hoje: LocalDate = LocalDate.now()): Boolean =
        !pago && hoje.isBefore(dataVencimento())

    fun valorAtual(hoje: LocalDate = LocalDate.now()): Double = when {
        pago && pagoComDesconto -> valorComDesconto()
        pago -> valorCheio
        descontoAindaValido(hoje) -> valorComDesconto()
        else -> valorCheio
    }

    fun mesReferencia(): String {
        val data = dataVencimento()
        val mes = data.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("pt", "BR"))
        return "${mes.replaceFirstChar { it.uppercase() }}/${data.year}"
    }

    companion object {
        fun proximoVencimentoDia10(hoje: LocalDate = LocalDate.now()): LocalDate {
            val esteMes = YearMonth.from(hoje).atDay(10)
            return if (!hoje.isAfter(esteMes)) esteMes else YearMonth.from(hoje).plusMonths(1).atDay(10)
        }
    }
}
