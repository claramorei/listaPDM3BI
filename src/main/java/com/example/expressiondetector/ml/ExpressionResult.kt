package com.example.expressiondetector.ml

/**
 * Resultado simplificado da classificação de expressão.
 * @param label rótulo em português: Feliz, Triste, Bravo, Surpreso ou Neutro
 * @param confidence valor heurístico entre 0 e 1 (não é uma probabilidade
 *        estatisticamente calibrada — serve apenas para fins didáticos)
 */
data class ExpressionResult(
    val label: String,
    val confidence: Float
)
