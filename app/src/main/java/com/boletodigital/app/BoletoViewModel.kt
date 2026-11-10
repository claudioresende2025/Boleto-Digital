package com.boletodigital.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.boletodigital.app.data.Boleto
import com.boletodigital.app.data.BoletoRepository
import com.boletodigital.app.data.LeituraBoleto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class BoletoViewModel(private val repository: BoletoRepository) : ViewModel() {

    val boletos: StateFlow<List<Boleto>> = repository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _leituraPendente = MutableStateFlow<LeituraBoleto?>(null)
    val leituraPendente = _leituraPendente.asStateFlow()

    fun observar(id: Long) = repository.observarPorId(id)

    fun definirLeitura(leitura: LeituraBoleto) {
        _leituraPendente.value = leitura
    }

    fun limparLeitura() {
        _leituraPendente.value = null
    }

    fun salvar(
        id: Long = 0,
        titulo: String,
        vencimento: LocalDate,
        valorCheio: Double,
        descontoPix: Double,
        codigoPix: String,
        onDone: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val atual = if (id > 0) repository.buscarPorId(id) else null
            val boleto = (atual ?: Boleto(
                titulo = titulo,
                vencimento = vencimento.toString()
            )).copy(
                id = id,
                titulo = titulo.trim().ifBlank { "Mensalidade" },
                vencimento = vencimento.toString(),
                valorCheio = valorCheio,
                descontoPix = descontoPix,
                codigoPix = codigoPix.trim()
            )
            val savedId = repository.salvar(boleto)
            onDone(savedId)
        }
    }

    fun gerarSerie(
        titulo: String,
        mesInicio: YearMonth,
        mesFim: YearMonth,
        diaVencimento: Int,
        valorCheio: Double,
        descontoPix: Double,
        codigoPixLido: String,
        reutilizarPix: Boolean,
        onDone: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val inicio = if (mesInicio <= mesFim) mesInicio else mesFim
            val fim = if (mesInicio <= mesFim) mesFim else mesInicio
            val existentes = repository.listarVencimentos().toHashSet()
            val novos = mutableListOf<Boleto>()
            var mes = inicio
            var indice = 0
            while (!mes.isAfter(fim)) {
                val dia = diaVencimento.coerceIn(1, mes.lengthOfMonth())
                val vencimento = mes.atDay(dia).toString()
                if (vencimento !in existentes) {
                    val pix = when {
                        indice == 0 -> codigoPixLido
                        reutilizarPix -> codigoPixLido
                        else -> ""
                    }
                    novos += Boleto(
                        titulo = titulo.trim().ifBlank { "Mensalidade" },
                        vencimento = vencimento,
                        valorCheio = valorCheio,
                        descontoPix = descontoPix,
                        codigoPix = pix
                    )
                }
                mes = mes.plusMonths(1)
                indice++
            }
            if (novos.isNotEmpty()) {
                repository.inserirTodos(novos)
            }
            limparLeitura()
            onDone(novos.size)
        }
    }

    fun marcarPago(boleto: Boleto, pago: Boolean) {
        viewModelScope.launch {
            val comDesconto = pago && boleto.descontoAindaValido()
            repository.salvar(
                boleto.copy(
                    pago = pago,
                    pagoComDesconto = if (pago) comDesconto else false
                )
            )
        }
    }

    fun excluir(boleto: Boleto, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.excluir(boleto)
            onDone()
        }
    }

    fun excluirVarios(ids: Collection<Long>, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val lista = ids.distinct()
            repository.excluirPorIds(lista)
            onDone(lista.size)
        }
    }

    class Factory(private val repository: BoletoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BoletoViewModel(repository) as T
        }
    }
}
