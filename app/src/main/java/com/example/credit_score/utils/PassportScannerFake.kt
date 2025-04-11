package com.example.credit_score.utils

import kotlinx.coroutines.delay

/**
 * Имитация сканирования паспорта и лица
 */
object PassportScannerFake {
    
    // Интерфейс для обратного вызова после сканирования паспорта
    interface PassportScanCallback {
        fun onScanComplete(data: PassportData)
        fun onScanError(errorMessage: String)
    }
    
    // Данные паспорта
    data class PassportData(
        val fullName: String,
        val inn: String,
        val birthDate: String,
        val passportNumber: String,
        val issuedBy: String,
        val issuedDate: String,
        val expiryDate: String
    )
    
    // Интерфейс для обратного вызова после проверки лица
    interface FaceVerificationCallback {
        fun onVerificationComplete(success: Boolean, similarity: Float)
        fun onVerificationError(errorMessage: String)
    }
    
    // Имитация сканирования паспорта
    suspend fun scanPassport(callback: PassportScanCallback) {
        // Имитация задержки сканирования
        delay(2000)
        
        // Вероятность ошибки сканирования 20%
        if (Math.random() < 0.2) {
            callback.onScanError("Ошибка сканирования паспорта. Убедитесь, что паспорт находится в рамке.")
            return
        }
        
        // Выбор случайного заранее определенного паспорта
        val passports = listOf(
            PassportData(
                fullName = "Амиров Бакир Тимурович",
                inn = "20107200450055",
                birthDate = "07.01.2004",
                passportNumber = "AN1234567",
                issuedBy = "МКК 50-55",
                issuedDate = "15.01.2020",
                expiryDate = "07.01.2030"
            ),
            PassportData(
                fullName = "Иванов Алексей Петрович",
                inn = "20409200500637",
                birthDate = "09.04.2005",
                passportNumber = "AN7654321",
                issuedBy = "МКК 50-55",
                issuedDate = "20.04.2021",
                expiryDate = "09.04.2031"
            ),
            PassportData(
                fullName = "Кадырбеков Айдарбек Жанибекович",
                inn = "21912200400369",
                birthDate = "12.12.2004",
                passportNumber = "AN9876543",
                issuedBy = "МКК 50-55",
                issuedDate = "20.12.2020",
                expiryDate = "12.12.2030"
            ),
            PassportData(
                fullName = "Асанбеков Адис Эрланович",
                inn = "21904200500386",
                birthDate = "04.04.2005",
                passportNumber = "AN3456789",
                issuedBy = "МКК 50-55",
                issuedDate = "10.04.2021",
                expiryDate = "04.04.2031"
            )
        )
        
        val randomPassport = passports.random()
        callback.onScanComplete(randomPassport)
    }
    
    // Имитация проверки лица
    suspend fun verifyFace(callback: FaceVerificationCallback) {
        // Имитация задержки проверки лица
        delay(1500)
        
        // Вероятность ошибки проверки лица 30%
        if (Math.random() < 0.3) {
            callback.onVerificationError("Ошибка проверки лица. Пожалуйста, убедитесь в хорошем освещении и снимите очки.")
            return
        }
        
        // Генерация случайного показателя схожести (0.7 - 1.0)
        val similarity = (Math.random() * 0.3 + 0.7).toFloat()
        
        // Считаем проверку успешной, если схожесть >= 0.8
        val success = similarity >= 0.8
        
        callback.onVerificationComplete(success, similarity)
    }
}
