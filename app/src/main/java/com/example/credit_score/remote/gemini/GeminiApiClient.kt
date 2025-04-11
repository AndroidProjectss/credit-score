package com.example.credit_score.remote.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val apiKey: String) {
    private val service: GeminiService

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(GeminiService::class.java)
    }

    interface GeminiApiListener {
        fun onSuccess(passportData: PassportData)
        fun onError(e: Exception)
        // Метод с реализацией по умолчанию для обратной совместимости
        fun onFraudDetected(message: String) {
            // По умолчанию просто логируем сообщение о мошенничестве
            Log.w(TAG, "Fraud detected: $message")
            // И обрабатываем как обычную ошибку
            onError(Exception("Fraud detected: $message"))
        }
    }

    // Метод для анализа обеих сторон паспорта с проверкой на мошенничество
    fun analyzePassportBothSides(frontBitmap: Bitmap, backBitmap: Bitmap, listener: GeminiApiListener) {
        try {
            // Convert bitmaps to base64
            val base64FrontImage = bitmapToBase64(frontBitmap)
            val base64BackImage = bitmapToBase64(backBitmap)

            // Сначала проверяем изображения на признаки мошенничества
            checkForFraud(frontBitmap, backBitmap) { isFraud, fraudMessage ->
                if (isFraud) {
                    listener.onFraudDetected(fraudMessage)
                    return@checkForFraud
                }

                // Если проверка пройдена, продолжаем обычный анализ
                // Create the request body as JSON
                val requestJson = buildPassportAnalysisRequestBothSides(base64FrontImage, base64BackImage)
                val requestBody = requestJson.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                // Make API call
                service.generateContent(apiKey, requestBody).enqueue(object : Callback<GeminiResponse> {
                    override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            try {
                                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                                val passportData = parseGeminiResponse(text)
                                listener.onSuccess(passportData)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing Gemini response: ${e.message}")
                                listener.onError(e)
                            }
                        } else {
                            try {
                                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                Log.e(TAG, "API Error: $errorBody")
                                listener.onError(IOException("API Error: $errorBody"))
                            } catch (e: IOException) {
                                listener.onError(e)
                            }
                        }
                    }

                    override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                        Log.e(TAG, "Connection error: ${t.message}")
                        listener.onError(Exception(t))
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing request: ${e.message}")
            listener.onError(e)
        }
    }

    // Обновляем оригинальный метод для совместимости, добавляя проверку на мошенничество
    fun analyzePassport(bitmap: Bitmap, listener: GeminiApiListener) {
        try {
            // Сначала проверяем изображение на признаки мошенничества
            checkForFraud(bitmap, null) { isFraud, fraudMessage ->
                if (isFraud) {
                    listener.onFraudDetected(fraudMessage)
                    return@checkForFraud
                }

                // Если проверка пройдена, продолжаем обычный анализ
                val base64Image = bitmapToBase64(bitmap)

                // Create the request body as JSON
                val requestJson = buildPassportAnalysisRequest(base64Image)
                val requestBody = requestJson.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                // Make API call
                service.generateContent(apiKey, requestBody).enqueue(object : Callback<GeminiResponse> {
                    override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            try {
                                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                                val passportData = parseGeminiResponse(text)
                                listener.onSuccess(passportData)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing Gemini response: ${e.message}")
                                listener.onError(e)
                            }
                        } else {
                            try {
                                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                Log.e(TAG, "API Error: $errorBody")
                                listener.onError(IOException("API Error: $errorBody"))
                            } catch (e: IOException) {
                                listener.onError(e)
                            }
                        }
                    }

                    override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                        Log.e(TAG, "Connection error: ${t.message}")
                        listener.onError(Exception(t))
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing request: ${e.message}")
            listener.onError(e)
        }
    }

    // Метод для проверки на мошенничество перед отправкой в API - СИЛЬНО УПРОЩЕННЫЙ
    private fun checkForFraud(
        frontBitmap: Bitmap,
        backBitmap: Bitmap?,
        callback: (Boolean, String) -> Unit
    ) {
        try {
            // Convert bitmaps to base64
            val base64FrontImage = bitmapToBase64(frontBitmap)
            val base64BackImage = backBitmap?.let { bitmapToBase64(it) }

            // Создаем запрос на проверку мошенничества
            val requestJson = buildFraudDetectionRequest(base64FrontImage, base64BackImage)
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            // Make API call to check for fraud
            service.generateContent(apiKey, requestBody).enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                            val isFraud = text.contains("МОШЕННИЧЕСТВО: ДА", ignoreCase = true)
                            val fraudMessage = if (isFraud) {
                                text.substringAfter("ПРИЧИНА:", "Обнаружены признаки мошенничества").trim()
                            } else {
                                ""
                            }
                            callback(isFraud, fraudMessage)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing fraud detection response: ${e.message}")
                            callback(false, "")
                        }
                    } else {
                        Log.e(TAG, "Fraud detection API error")
                        callback(false, "")
                    }
                }

                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    Log.e(TAG, "Fraud detection connection error: ${t.message}")
                    callback(false, "")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in fraud detection: ${e.message}")
            callback(false, "")
        }
    }

    private fun buildFraudDetectionRequest(base64FrontImage: String, base64BackImage: String?): JsonObject {
        val gson = Gson()

        // Create the text instruction part
        val textPart = JsonObject().apply {
            addProperty("text", buildFraudDetectionPrompt())
        }

        // Create the front image part with inline_data
        val frontImagePart = JsonObject().apply {
            val inlineData = JsonObject().apply {
                addProperty("mime_type", "image/jpeg")
                addProperty("data", base64FrontImage)
            }
            add("inline_data", inlineData)
        }

        // Create contents array with parts
        val contentsParts = JsonObject()
        val partsList = mutableListOf(textPart, frontImagePart)

        // Add back image if available
        if (base64BackImage != null) {
            val backImagePart = JsonObject().apply {
                val inlineData = JsonObject().apply {
                    addProperty("mime_type", "image/jpeg")
                    addProperty("data", base64BackImage)
                }
                add("inline_data", inlineData)
            }
            partsList.add(backImagePart)
        }

        contentsParts.add("parts", gson.toJsonTree(partsList))

        // Build the complete request
        val request = JsonObject()
        request.add("contents", gson.toJsonTree(listOf(contentsParts)))

        return request
    }

    // УПРОЩЕННЫЙ ПРОМПТ для проверки мошенничества
    private fun buildFraudDetectionPrompt(): String {
        return """
            Проанализируй предоставленное изображение и определи только, является ли это фотографией реального паспорта Кыргызской Республики или это фотография экрана устройства, на котором отображается паспорт.
            
            Проверь только следующий признак мошенничества:
            - Видны ли края экрана устройства (телефона, планшета, компьютера) или явные признаки того, что это фотография экрана с изображением паспорта
            
            Ответь строго в следующем формате:
            
            МОШЕННИЧЕСТВО: [ДА/НЕТ]
            ПРИЧИНА: [если ДА, укажи что это фотография экрана]
            
            Если мошенничество не обнаружено, просто ответь "МОШЕННИЧЕСТВО: НЕТ" без указания причины.
            Будь очень лояльным в своих проверках - считай изображение подлинным, если нет очевидных признаков, что это фотография экрана.
        """.trimIndent()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val resizedBitmap = resizeBitmapIfNeeded(bitmap)
        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        // Resize bitmap if it's too large to avoid API limits
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun buildPassportAnalysisRequestBothSides(base64FrontImage: String, base64BackImage: String): JsonObject {
        val gson = Gson()

        // Упрощенные инструкции - убраны строгие проверки мошенничества
        val textPart = JsonObject().apply {
            addProperty("text", buildSimplifiedPassportAnalysisPromptBothSides())
        }

        // Create the front image part with inline_data
        val frontImagePart = JsonObject().apply {
            val inlineData = JsonObject().apply {
                addProperty("mime_type", "image/jpeg")
                addProperty("data", base64FrontImage)
            }
            add("inline_data", inlineData)
        }

        // Create the back image part with inline_data
        val backImagePart = JsonObject().apply {
            val inlineData = JsonObject().apply {
                addProperty("mime_type", "image/jpeg")
                addProperty("data", base64BackImage)
            }
            add("inline_data", inlineData)
        }

        // Create contents array with all parts
        val contentsParts = JsonObject()
        contentsParts.add("parts", gson.toJsonTree(listOf(textPart, frontImagePart, backImagePart)))

        // Build the complete request
        val request = JsonObject()
        request.add("contents", gson.toJsonTree(listOf(contentsParts)))

        return request
    }

    private fun buildPassportAnalysisRequest(base64Image: String): JsonObject {
        val gson = Gson()

        // Упрощенные инструкции - убраны строгие проверки мошенничества
        val textPart = JsonObject().apply {
            addProperty("text", buildSimplifiedPassportAnalysisPrompt())
        }

        // Create the image part with inline_data
        val imagePart = JsonObject().apply {
            val inlineData = JsonObject().apply {
                addProperty("mime_type", "image/jpeg")
                addProperty("data", base64Image)
            }
            add("inline_data", inlineData)
        }

        // Create contents array with both parts
        val contentsParts = JsonObject()
        contentsParts.add("parts", gson.toJsonTree(listOf(textPart, imagePart)))

        // Build the complete request
        val request = JsonObject()
        request.add("contents", gson.toJsonTree(listOf(contentsParts)))

        return request
    }

    // УПРОЩЕННЫЕ ПРОМПТЫ без лишних проверок
    private fun buildSimplifiedPassportAnalysisPromptBothSides(): String {
        return """
            Проанализируй эти два изображения паспорта Кыргызской Республики (лицевая и обратная стороны) и определи следующие данные:
            1. Идентификационный номер (ИНН) - 14 цифр
            2. ФИО владельца
            3. Дата рождения
            4. Номер паспорта
            
            Дополнительные указания:
            1. ИНН должен состоять из 14 цифр
            2. ИНН обычно указан в паспорте как "ID:" или "ИНН:" или просто серия цифр
            3. Первое изображение - лицевая сторона, второе - обратная сторона паспорта
            
            Ответь в следующем формате без дополнительных комментариев:
            
            ИНН: [14 цифр]
            ФИО: [фамилия имя отчество]
            Дата рождения: [дд.мм.гггг]
            Номер паспорта: [серия и номер]
        """.trimIndent()
    }

    private fun buildSimplifiedPassportAnalysisPrompt(): String {
        return """
            Проанализируй это изображение паспорта Кыргызской Республики и определи следующие данные:
            1. Идентификационный номер (ИНН) - 14 цифр
            2. ФИО владельца
            3. Дата рождения
            4. Номер паспорта
            
            Дополнительные указания:
            1. ИНН должен состоять из 14 цифр
            2. ИНН обычно указан в паспорте как "ID:" или "ИНН:" или просто серия цифр
            
            Ответь в следующем формате без дополнительных комментариев:
            
            ИНН: [14 цифр]
            ФИО: [фамилия имя отчество]
            Дата рождения: [дд.мм.гггг]
            Номер паспорта: [серия и номер]
        """.trimIndent()
    }

    private fun parseGeminiResponse(text: String): PassportData {
        var inn = ""
        var fullName = ""
        var birthDate = ""
        var passportNumber = ""

        val lines = text.split("\n")
        for (line in lines) {
            when {
                line.startsWith("ИНН:") ->
                    inn = line.substringAfter("ИНН:").trim()
                line.startsWith("ФИО:") ->
                    fullName = line.substringAfter("ФИО:").trim()
                line.startsWith("Дата рождения:") ->
                    birthDate = line.substringAfter("Дата рождения:").trim()
                line.startsWith("Номер паспорта:") ->
                    passportNumber = line.substringAfter("Номер паспорта:").trim()
            }
        }

        return PassportData(inn, fullName, birthDate, passportNumber)
    }
}

// Класс для хранения метаданных об устройстве для дополнительной проверки
data class DeviceMetadata(
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String,
    val timeZone: String,
    val isEmulator: Boolean
)