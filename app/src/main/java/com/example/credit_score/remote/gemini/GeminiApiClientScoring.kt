package com.example.creditscore.remote.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.credit_score.remote.gemini.SearchQuery
import com.example.visualsearch.remote.gemini.GeminiResponse
import com.example.visualsearch.remote.gemini.GeminiService
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
import java.util.regex.Pattern

class GeminiApiClientScoring(private val apiKey: String) {
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
        fun onSuccess(searchQuery: SearchQuery)
        fun onError(e: Exception)
    }

    fun analyzeCreditApplication(applicationData: CreditApplicationData, listener: GeminiApiListener) {
        try {
            // Create the request body as JSON
            val requestJson = buildCreditScoringRequest(applicationData)
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            // Make API call
            service.generateContent(apiKey, requestBody).enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                            val searchQuery = parseGeminiResponse(text)
                            listener.onSuccess(searchQuery)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing request: ${e.message}")
            listener.onError(e)
        }
    }

    fun analyzeImage(bitmap: Bitmap, listener: GeminiApiListener) {
        try {
            // Convert bitmap to base64
            val base64Image = bitmapToBase64(bitmap)

            // Create the request body as JSON
            val requestJson = buildImageAnalysisRequest(base64Image)
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            // Make API call
            service.generateContent(apiKey, requestBody).enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                            val searchQuery = parseGeminiResponse(text)
                            listener.onSuccess(searchQuery)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing request: ${e.message}")
            listener.onError(e)
        }
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

    private fun buildImageAnalysisRequest(base64Image: String): JsonObject {
        val gson = Gson()

        // Create the text instruction part
        val textPart = JsonObject().apply {
            addProperty("text", buildAnalysisPrompt())
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

    private fun buildCreditScoringRequest(applicationData: CreditApplicationData): JsonObject {
        val gson = Gson()

        // Create the text instruction part with the prompt and application data
        val textPart = JsonObject().apply {
            addProperty("text", buildCreditScoringPrompt(applicationData))
        }

        // Create contents array with the text part
        val contentsParts = JsonObject()
        contentsParts.add("parts", gson.toJsonTree(listOf(textPart)))

        // Build the complete request
        val request = JsonObject()
        request.add("contents", gson.toJsonTree(listOf(contentsParts)))

        return request
    }

    private fun buildAnalysisPrompt(): String {
        return "".trimIndent()
    }

    private fun buildCreditScoringPrompt(data: CreditApplicationData): String {
        return """
        Ты - опытный кредитный аналитик с многолетним опытом работы в банковском секторе Кыргызстана. 
        Твоя задача - провести скоринговый анализ заявки на кредит и определить:
        
        1. Общий кредитный скоринговый балл (от 0 до 100)
        2. Уровень кредитного риска (Низкий, Средний, Высокий, Очень высокий)
        3. Максимальную рекомендуемую сумму кредита в сомах
        4. Максимальный рекомендуемый срок в месяцах
        5. Рекомендуемую процентную ставку
        6. Обоснование твоего решения
        7. Сильные стороны заявителя (не более 3)
        8. Слабые стороны заявителя (не более 3)
        9. Рекомендации для банка (не более 3)
        
        Данные кредитной заявки:
        
        ФИО: ${data.fullName}
        Паспортные данные: ${data.passportData}
        Адрес регистрации: ${data.registrationAddress}
        Стаж работы: ${data.workExperience} лет
        Ежемесячный доход: ${data.monthlyIncome} сомов
        Ежемесячные расходы: ${data.monthlyExpenses} сомов
        Наличие задолженности по налогам: ${if (data.hasTaxDebt) "Есть" else "Нет"}
        Размер задолженности по налогам (если есть): ${data.taxDebtAmount ?: 0} сомов
        Статус занятости: ${data.employmentStatus}
        Состав семьи: ${data.familyStatus}
        Запрашиваемая сумма кредита: ${data.requestedAmount} сомов
        Запрашиваемый срок кредита: ${data.requestedTerm} месяцев
        Цель кредита: ${data.purpose}
        Кредитная история: ${data.creditHistory}
        
        ВАЖНО: 
        1. Основывай свои выводы на специфике банковского сектора Кыргызстана, учитывая местные экономические условия.
        2. Формат ответа должен быть строго в JSON формате, содержащим все поля: overallScore, creditRiskLevel, maxRecommendedAmount, maxRecommendedTerm, recommendedInterestRate, justification, strengths (массив), weaknesses (массив), recommendations (массив).
        3. Используй реалистичные показатели для банковского сектора Кыргызстана: процентные ставки обычно в диапазоне 14-30%, зависят от риска.
        4. Учитывай платежеспособность (доход - расходы должны позволять выплачивать кредит).
        5. Соблюдай все банковские нормативы Кыргызстана по кредитованию физических лиц.
        
        Дай только JSON ответ, без дополнительных пояснений.
        """.trimIndent()
    }

    private fun parseGeminiResponse(text: String): SearchQuery {
        try {
            // Extract JSON from response if it's wrapped in any markdown or other text
            val jsonPattern = "\\{[\\s\\S]*\\}"
            val pattern = Pattern.compile(jsonPattern)
            val matcher = pattern.matcher(text)

            val jsonStr = if (matcher.find()) {
                matcher.group(0)
            } else {
                throw Exception("No JSON found in response")
            }

            // Parse JSON to SearchQuery
            val gson = Gson()
            return gson.fromJson(jsonStr, SearchQuery::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}")
            throw e
        }
    }
}

// Data class to represent the credit application data
data class CreditApplicationData(
    val fullName: String,
    val passportData: String,
    val registrationAddress: String,
    val workExperience: Int,
    val monthlyIncome: Int,
    val monthlyExpenses: Int,
    val hasTaxDebt: Boolean,
    val taxDebtAmount: Int? = null,
    val employmentStatus: String,
    val familyStatus: String,
    val requestedAmount: Int,
    val requestedTerm: Int,
    val purpose: String,
    val creditHistory: String
)