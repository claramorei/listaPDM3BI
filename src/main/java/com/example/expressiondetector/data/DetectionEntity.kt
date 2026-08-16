package com.example.expressiondetector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa uma detecção de expressão salva no histórico.
 * Guardamos apenas rótulo da expressão e um valor de confiança (0..1),
 * sem qualquer dado biométrico bruto ou imagem — apenas o resultado
 * já classificado, para fins educacionais.
 */
@Entity(tableName = "detections")
data class DetectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
