package com.boletodigital.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BoletoDao {
    @Query("SELECT * FROM boletos ORDER BY vencimento DESC, id DESC")
    fun observarTodos(): Flow<List<Boleto>>

    @Query("SELECT * FROM boletos WHERE id = :id LIMIT 1")
    fun observarPorId(id: Long): Flow<Boleto?>

    @Query("SELECT * FROM boletos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): Boleto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(boleto: Boleto): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(boletos: List<Boleto>): List<Long>

    @Query("SELECT vencimento FROM boletos")
    suspend fun listarVencimentos(): List<String>

    @Update
    suspend fun atualizar(boleto: Boleto)

    @Delete
    suspend fun excluir(boleto: Boleto)
}
