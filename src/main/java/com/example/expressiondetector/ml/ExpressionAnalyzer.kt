package com.example.expressiondetector.ml

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

/**
 * ===========================================================================
 *  COMPONENTE DE MACHINE LEARNING (ML Kit - Google)
 * ===========================================================================
 * Usa o modelo on-device de detecção facial do ML Kit para extrair:
 *  - probabilidade de sorriso (smilingProbability)
 *  - probabilidade dos olhos abertos
 *  - contornos da boca e das sobrancelhas (landmarks geométricos)
 *
 * A partir dessas saídas do modelo de ML, aplicamos uma REGRA HEURÍSTICA
 * simples para mapear em 5 categorias de expressão. Isso é proposital:
 * o ML Kit não fornece um classificador de emoções pronto, então
 * combinamos a saída do modelo de ML com geometria facial para fins
 * EDUCACIONAIS. Este projeto NÃO deve ser usado para qualquer tipo de
 * avaliação psicológica, médica ou de estado emocional real de alguém.
 * ===========================================================================
 */
class ExpressionAnalyzer(
    private val onResult: (ExpressionResult?) -> Unit
) {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onResult(null)
                } else {
                    onResult(classify(faces[0]))
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Regra heurística de classificação em 5 categorias, combinando:
     *  - smilingProbability (saída direta do modelo de ML)
     *  - abertura da boca (distância vertical entre lábio superior e inferior,
     *    normalizada pela altura do rosto)
     *  - altura das sobrancelhas em relação aos olhos (normalizada)
     */
    private fun classify(face: Face): ExpressionResult {
        val smileProb = face.smilingProbability ?: 0.5f
        val faceHeight = face.boundingBox.height().toFloat().coerceAtLeast(1f)

        val mouthOpenRatio = mouthOpenness(face, faceHeight)
        val eyebrowRaiseRatio = eyebrowRaise(face, faceHeight)

        return when {
            smileProb > 0.65f ->
                ExpressionResult("Feliz", smileProb)

            mouthOpenRatio > 0.09f && eyebrowRaiseRatio > 0.11f ->
                ExpressionResult("Surpreso", ((mouthOpenRatio + eyebrowRaiseRatio) / 2).coerceIn(0f, 1f))

            smileProb < 0.12f && eyebrowRaiseRatio < 0.06f ->
                ExpressionResult("Bravo", (1 - smileProb).coerceIn(0f, 1f))

            smileProb < 0.20f && mouthOpenRatio < 0.03f ->
                ExpressionResult("Triste", (1 - smileProb).coerceIn(0f, 1f))

            else ->
                ExpressionResult("Neutro", 0.5f)
        }
    }

    private fun mouthOpenness(face: Face, faceHeight: Float): Float {
        val upperLip = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points?.firstOrNull()
        val lowerLip = face.getContour(FaceContour.LOWER_LIP_TOP)?.points?.firstOrNull()
        if (upperLip == null || lowerLip == null) return 0f
        return abs(lowerLip.y - upperLip.y) / faceHeight
    }

    private fun eyebrowRaise(face: Face, faceHeight: Float): Float {
        val browLeft = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points?.firstOrNull()
        val eyeLeft = face.getContour(FaceContour.LEFT_EYE)?.points?.firstOrNull()
        if (browLeft == null || eyeLeft == null) return 0f
        return abs(eyeLeft.y - browLeft.y) / faceHeight
    }

    fun close() {
        detector.close()
    }
}
