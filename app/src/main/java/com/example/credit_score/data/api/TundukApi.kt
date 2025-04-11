package com.example.credit_score.data.api

import com.example.credit_score.data.model.TundukData
import kotlin.random.Random

/**
 * Интерфейс для Тундук API
 */
interface TundukApiInterface {
    /**
     * Получает данные по ИНН
     * @param inn ИНН клиента
     * @return Данные клиента или null, если клиент не найден
     */
    suspend fun getDataByInn(inn: String): TundukData?
}

/**
 * Имитация API Тундук, которая возвращает предопределенные данные по ИНН
 */
class TundukApi : TundukApiInterface {
    
    companion object {
        // Время задержки для имитации сетевого запроса (мс)
        private const val NETWORK_DELAY_MIN = 500L
        private const val NETWORK_DELAY_MAX = 1500L
        
        // Вероятность сетевой ошибки (0.0 - никогда, 1.0 - всегда)
        private const val NETWORK_ERROR_PROBABILITY = 0.1
    }
    
    /**
     * Получает данные по ИНН с имитацией задержки сети
     */
    override suspend fun getDataByInn(inn: String): TundukData? {
        // Имитация сетевой задержки
        simulateNetworkDelay()
        
        // Имитация случайной сетевой ошибки
        if (shouldSimulateNetworkError()) {
            throw Exception("Ошибка соединения с Тундук API")
        }
        
        // Поиск данных в "базе данных"
        return TundukDatabase.getClientDataByInn(inn)
    }
    
    /**
     * Имитирует задержку сети
     */
    private suspend fun simulateNetworkDelay() {
        val delayTime = Random.nextLong(NETWORK_DELAY_MIN, NETWORK_DELAY_MAX)
        kotlinx.coroutines.delay(delayTime)
    }
    
    /**
     * Определяет, нужно ли имитировать сетевую ошибку
     */
    private fun shouldSimulateNetworkError(): Boolean {
        return Random.nextDouble() < NETWORK_ERROR_PROBABILITY
    }
}
