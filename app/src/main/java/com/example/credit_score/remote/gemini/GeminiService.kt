package com.example.credit_score.remote.gemini

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    fun generateContent(
        @Query("key") apiKey: String,
        @Body request: RequestBody
    ): Call<GeminiResponse>
}