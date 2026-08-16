package com.example.expressiondetector.network

/**
 * Modelo de resposta da API pública Quotable (https://api.quotable.io).
 * Usada para trazer uma frase relacionada ao "humor" identificado,
 * apenas como complemento lúdico/educacional — não é uma recomendação
 * terapêutica.
 */
data class QuoteResponse(
    val content: String,
    val author: String,
    val tags: List<String> = emptyList()
)
