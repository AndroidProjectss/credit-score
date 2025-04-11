package com.example.credit_score.services

import com.example.credit_score.data.model.TundukData
import com.example.credit_score.data.repository.TundukRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Сервис для взаимодействия между сканером паспорта и Тундук API
 */
class TundukService {
    
    private val repository = TundukRepository()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Интерфейс обратного вызова для получения результатов запроса к Тундук
     */
    interface TundukCallback {
        fun onSuccess(data: TundukData)
        fun onError(errorMessage: String)
    }
    
    /**
     * Выполняет запрос к Тундук по ИНН
     * @param inn ИНН клиента
     * @param callback Обратный вызов для получения результата
     */
    fun requestDataByInn(inn: String, callback: TundukCallback) {
        serviceScope.launch {
            try {
                val result = repository.getClientDataByInn(inn)
                
                withContext(Dispatchers.Main) {
                    result.onSuccess { data ->
                        callback.onSuccess(data)
                    }.onFailure { exception ->
                        callback.onError(exception.message ?: "Неизвестная ошибка")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError("Ошибка запроса: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Выполняет запрос к Тундук по тексту паспорта
     * Имитация работы OCR + Извлечение ИНН из текста паспорта
     * @param passportText Текст, извлеченный из паспорта OCR
     * @param callback Обратный вызов для получения результата
     */
    fun requestDataByPassportText(passportText: String, callback: TundukCallback) {
        // Здесь будет логика извлечения ИНН из текста паспорта
        // В реальном приложении это делается с помощью ML Kit или другой OCR технологии
        
        // Для демонстрации просто проверим, содержит ли текст паспорта один из наших ИНН
        val knownInns = listOf(
            "20107200450055",
            "20409200500637",
            "21912200400369",
            "21904200500386"
        )
        
        // Поиск ИНН в тексте паспорта
        val foundInn = knownInns.firstOrNull { passportText.contains(it) }
        
        if (foundInn != null) {
            // Если ИНН найден, делаем запрос к Тундук
            requestDataByInn(foundInn, callback)
        } else {
            // Если ИНН не найден, возвращаем ошибку
            callback.onError("ИНН не найден в тексте паспорта")
        }
    }
}
