package com.example.credit_score.remote.gemini

import com.google.gson.annotations.SerializedName

data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<Candidate>? = null
)

data class Candidate(
    @SerializedName("content") val content: Content? = null
)

data class Content(
    @SerializedName("parts") val parts: List<Part>? = null,
    @SerializedName("role") val role: String? = null
)

data class Part(
    @SerializedName("text") val text: String? = null
)

// Модель данных для паспорта
data class PassportData(
    val inn: String = "",
    val fullName: String = "",
    val birthDate: String = "",
    val passportNumber: String = ""
)

