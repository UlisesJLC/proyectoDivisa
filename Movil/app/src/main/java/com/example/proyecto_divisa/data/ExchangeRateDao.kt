package com.example.proyecto_divisa.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDAO {

    @Insert
    suspend fun insertar(exchangeRate: ExchangeRate)

    @Query("SELECT * FROM exchangerate")
    fun obtenerTodos(): Flow<List<ExchangeRate>> // Usamos Flow para observar cambios

    @Query("SELECT * FROM exchangerate WHERE id = :id")
    suspend fun obtenerPorId(id: Int): ExchangeRate?

    @Query("DELETE FROM exchangerate WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}