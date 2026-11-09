package com.boletodigital.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.boletodigital.app.data.Boleto
import com.boletodigital.app.data.BoletoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class BoletoViewModel(private val repository: BoletoRepository) : ViewModel() {

    val boletos: StateFlow<List<Boleto>> = repository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observar(id: Long) = repository.observarPorId(id)

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

    class Factory(private val repository: BoletoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BoletoViewModel(repository) as T
        }
    }
}
