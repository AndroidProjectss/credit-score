package com.example.credit_score

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.credit_score.utils.FaceComparator
import com.example.credit_score.utils.FaceExtractor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class FaceRecognitionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FaceRecognitionActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var viewFinder: PreviewView
    private lateinit var faceOvalOverlay: View
    private lateinit var statusText: TextView
    private lateinit var instructionText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector

    private val faceComparator = FaceComparator()
    private val faceExtractor = FaceExtractor()

    private var imageCapture: ImageCapture? = null
    private var passportFaceBitmap: Bitmap? = null
    private var isFaceDetected = false
    private var processingFace = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_recognition)

        val passportImagePath = intent.getStringExtra("passport_face_path")
        if (passportImagePath != null) {
            passportFaceBitmap = BitmapFactory.decodeFile(passportImagePath)
            if (passportFaceBitmap == null) {
                showError("Не удалось загрузить фото с паспорта")
                return
            }
        } else {
            showError("Фото паспорта не найдено")
            return
        }

        viewFinder = findViewById(R.id.viewFinder)
        faceOvalOverlay = findViewById(R.id.faceOvalOverlay)
        statusText = findViewById(R.id.status_text)
        instructionText = findViewById(R.id.instruction_text)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        faceDetector = FaceDetection.getClient(options)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        startOvalAnimation()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startOvalAnimation() {
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_scale)
        faceOvalOverlay.startAnimation(pulseAnimation)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(viewFinder.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, FaceAnalyzer())
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer, imageCapture
                )

                statusText.text = "Подготовка к сканированию..."
                instructionText.text = "Поместите лицо в овал"

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка запуска камеры: ", e)
                showError("Ошибка запуска камеры: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            if (processingFace) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                processingFace = true
                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        processingFace = false
                        processFaceDetectionResult(faces, imageProxy)
                    }
                    .addOnFailureListener { e ->
                        processingFace = false
                        Log.e(TAG, "Ошибка обнаружения лица: ", e)
                        imageProxy.close()
                    }
                    .addOnCompleteListener {
                        if (!it.isSuccessful) {
                            imageProxy.close()
                        }
                    }
            } else {
                imageProxy.close()
            }
        }

        private fun processFaceDetectionResult(faces: List<Face>, imageProxy: ImageProxy) {
            if (faces.isEmpty()) {
                runOnUiThread {
                    statusText.text = "Лицо не обнаружено"
                    instructionText.text = "Поместите лицо в овал"
                    isFaceDetected = false
                }
                imageProxy.close()
                return
            }

            val face = faces[0]
            val rightEyeOpenProbability = face.rightEyeOpenProbability ?: 0f
            val leftEyeOpenProbability = face.leftEyeOpenProbability ?: 0f
            val headEulerAngleY = face.headEulerAngleY
            val headEulerAngleZ = face.headEulerAngleZ

            runOnUiThread {
                if (rightEyeOpenProbability < 0.5f || leftEyeOpenProbability < 0.5f) {
                    statusText.text = "Откройте глаза"
                    isFaceDetected = false
                } else if (abs(headEulerAngleY) > 15) {
                    statusText.text = "Смотрите прямо в камеру"
                    isFaceDetected = false
                } else if (abs(headEulerAngleZ) > 15) {
                    statusText.text = "Не наклоняйте голову"
                    isFaceDetected = false
                } else {
                    statusText.text = "Лицо обнаружено!"
                    instructionText.text = "Оставайтесь неподвижны"
                    isFaceDetected = true

                    if (isFaceDetected && !processingFace) {
                        captureImage()
                    }
                }
            }

            imageProxy.close()
        }
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        processingFace = true
        statusText.text = "Делаем снимок..."

        val outputDir = cacheDir
        val outputFile = File.createTempFile("face_", ".jpg", outputDir)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        val selfieBitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                        if (selfieBitmap != null) {
                            CoroutineScope(Dispatchers.Main).launch {
                                processFaceComparison(selfieBitmap)
                            }
                        } else {
                            showError("Ошибка создания изображения")
                            processingFace = false
                        }
                    } catch (e: Exception) {
                        showError("Ошибка обработки изображения: ${e.message}")
                        processingFace = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    showError("Ошибка съемки: ${exception.message}")
                    processingFace = false
                }
            }
        )
    }

    private suspend fun processFaceComparison(selfieBitmap: Bitmap) {
        try {
            statusText.text = "Обработка фотографии..."
            instructionText.text = "Пожалуйста, подождите..."

            val selfieFace = withContext(Dispatchers.IO) {
                faceExtractor.extractFaceFromPassport(selfieBitmap)
            }

            if (selfieFace == null) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Не удалось обнаружить лицо"
                    instructionText.text = "Повторите попытку"
                    processingFace = false
                }
                return
            }

            saveImageToFile(selfieFace, "selfie_face.jpg")

            val comparisonResult = withContext(Dispatchers.IO) {
                faceComparator.compareFaces(passportFaceBitmap!!, selfieFace)
            }

            withContext(Dispatchers.Main) {
                val similarityPercent = (comparisonResult.similarityScore * 100).toInt()

                if (comparisonResult.isMatch) {
                    statusText.text = "Верификация успешна!"
                    instructionText.text = "Схожесть: $similarityPercent%"

                    viewFinder.postDelayed({
                        returnResult(true, similarityPercent)
                    }, 2000)
                } else {
                    val errorMsg = comparisonResult.errorMessage
                    if (errorMsg != null) {
                        statusText.text = errorMsg
                    } else {
                        statusText.text = "Верификация не удалась"
                    }
                    instructionText.text = "Схожесть: $similarityPercent% (мин. 85%)"

                    viewFinder.postDelayed({
                        processingFace = false
                        statusText.text = "Повторите попытку"
                        instructionText.text = "Убедитесь, что лицо хорошо освещено"
                    }, 3000)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showError("Ошибка при сравнении лиц: ${e.message}")
                processingFace = false
            }
        }
    }

    private fun saveImageToFile(bitmap: Bitmap, filename: String) {
        try {
            val file = File(cacheDir, filename)
            val outputStream = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Log.d(TAG, "Сохранено изображение: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении изображения: ", e)
        }
    }

    private fun returnResult(success: Boolean, similarityPercent: Int) {
        val resultIntent = Intent()
        resultIntent.putExtra("verification_success", success)
        resultIntent.putExtra("similarity_percent", similarityPercent)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        val resultIntent = Intent()
        resultIntent.putExtra("verification_success", false)
        resultIntent.putExtra("error_message", message)
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                showError("Требуется разрешение на использование камеры")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceComparator.close()
        faceExtractor.close()
    }
}