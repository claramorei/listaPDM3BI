package com.example.expressiondetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expressiondetector.data.AppDatabase
import com.example.expressiondetector.data.DetectionEntity
import com.example.expressiondetector.databinding.ActivityMainBinding
import com.example.expressiondetector.ml.ExpressionAnalyzer
import com.example.expressiondetector.ml.ExpressionResult
import com.example.expressiondetector.network.RetrofitClient
import com.example.expressiondetector.ui.HistoryAdapter
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var expressionAnalyzer: ExpressionAnalyzer
    private val historyAdapter = HistoryAdapter()

    private var lastResult: ExpressionResult? = null

    private val db by lazy { AppDatabase.getInstance(applicationContext) }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else {
                binding.tvExpression.text = "Permissão de câmera negada"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupHistoryList()
        setupButtons()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupHistoryList() {
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter

        lifecycleScope.launch {
            db.detectionDao().getAll().collect { list ->
                historyAdapter.submitList(list)
            }
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            val result = lastResult
            if (result != null) {
                lifecycleScope.launch {
                    db.detectionDao().insert(
                        DetectionEntity(expression = result.label, confidence = result.confidence)
                    )
                }
                fetchQuoteFor(result.label)
            }
        }

        binding.btnClear.setOnClickListener {
            lifecycleScope.launch {
                db.detectionDao().clearAll()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            expressionAnalyzer = ExpressionAnalyzer { result ->
                runOnUiThread { updateUi(result) }
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        expressionAnalyzer.analyze(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                binding.tvExpression.text = "Erro ao iniciar câmera: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateUi(result: ExpressionResult?) {
        lastResult = result
        binding.tvExpression.text = if (result != null) {
            "${result.label}  (${(result.confidence * 100).toInt()}%)"
        } else {
            getString(R.string.expression_default)
        }
    }

    /** Consumo de API pública: busca uma frase relacionada ao humor detectado. */
    private fun fetchQuoteFor(expressionLabel: String) {
        val tag = when (expressionLabel) {
            "Feliz" -> "happiness"
            "Triste" -> "sadness"
            "Bravo" -> "anger"
            "Surpreso" -> "wisdom"
            else -> null
        }

        binding.tvQuote.text = getString(R.string.quote_loading)

        lifecycleScope.launch {
            try {
                val quote = RetrofitClient.apiService.getRandomQuote(tag)
                binding.tvQuote.text = "\"${quote.content}\" — ${quote.author}"
            } catch (e: Exception) {
                binding.tvQuote.text = "Não foi possível buscar a frase agora."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::expressionAnalyzer.isInitialized) expressionAnalyzer.close()
        cameraExecutor.shutdown()
    }
}
