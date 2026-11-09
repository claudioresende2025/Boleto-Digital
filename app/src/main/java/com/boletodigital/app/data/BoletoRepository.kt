package com.boletodigital.app.data

import kotlinx.coroutines.flow.Flow

class BoletoRepository(private val dao: BoletoDao) {
    fun observarTodos(): Flow<List<Boleto>> = dao.observarTodos()
    fun observarPorId(id: Long): Flow<Boleto?> = dao.observarPorId(id)
    suspend fun buscarPorId(id: Long): Boleto? = dao.buscarPorId(id)
    suspend fun salvar(boleto: Boleto): Long = if (boleto.id == 0L) dao.inserir(boleto) else {
        dao.atualizar(boleto)
        boleto.id
    }
    suspend fun excluir(boleto: Boleto) = dao.excluir(boleto)
}
