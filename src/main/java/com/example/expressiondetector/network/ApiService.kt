package com.example.expressiondetector.network

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // Endpoint público e gratuito, sem necessidade de API key.
    // Documentação: https://github.com/lukePeavey/quotable
    @GET("random")
    suspend fun getRandomQuote(@Query("tags") tags: String? = null): QuoteResponse
}
