package com.example.credit_score.data.repository

import com.example.credit_score.data.api.TundukApi
import com.example.credit_score.data.model.TundukData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с Тундук API
 */
class TundukRepository {
    
    private val tundukApi = TundukApi()
    
    /**
     * Получение данных клиента по ИНН
     * @param inn ИНН клиента
     * @return Результат запроса с данными клиента или ошибкой
     */
    suspend fun getClientDataByInn(inn: String): Result<TundukData> = withContext(Dispatchers.IO) {
        try {
            val data = tundukApi.getDataByInn(inn)
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("Клиент с ИНН $inn не найден в базе данных Тундук"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
