package com.example.credit_score

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.credit_score.remote.gemini.GeminiApiClient
import com.example.credit_score.remote.gemini.PassportData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var instructionText: TextView
    private lateinit var captureButton: FloatingActionButton
    private val executor: Executor = Executors.newSingleThreadExecutor()

    private var scanLine: View? = null
    private var scanFrame: View? = null
    private var scanAnimation: Animation? = null

    private var isCapturingFrontSide = true
    private var frontSidePath: String? = null

    private fun startScanAnimation() {
        scanLine?.let { line ->
            scanAnimation?.let { animation ->
                line.startAnimation(animation)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        try {
            viewFinder = findViewById(R.id.viewFinder)
            captureButton = findViewById(R.id.capture_button)
            progressBar = findViewById(R.id.progressBar)
            statusText = findViewById(R.id.status_text)
            instructionText = findViewById(R.id.instruction_text)

            scanLine = findViewById(R.id.scanLine)
            scanFrame = findViewById(R.id.scanFrame)
            scanAnimation = AnimationUtils.loadAnimation(this, R.anim.scan_line_animation)

            val pulseButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_capture_button)
            captureButton.startAnimation(pulseButtonAnimation)

            instructionText.text = "Сфотографируйте ЛИЦЕВУЮ сторону паспорта"

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }

            captureButton.setOnClickListener { takePhoto() }

            startScanAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization: ", e)
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting camera: ", e)
                    Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera: ", e)
            Toast.makeText(this, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        try {
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetResolution(android.util.Size(1920, 1080))
                .setTargetRotation(viewFinder.display.rotation)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            statusText.text = "Camera ready"
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera: ", e)
            Toast.makeText(this, "Camera initialization error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, photoFile: File): Bitmap {
        val exif = androidx.exifinterface.media.ExifInterface(photoFile)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = android.graphics.Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        if (bitmap.height > bitmap.width) {
            matrix.postRotate(90f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            scanLine?.let {
                it.clearAnimation()
                it.visibility = View.INVISIBLE
            }

            progressBar.visibility = View.VISIBLE
            statusText.text = "Taking photo..."

            val directory = filesDir
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val side = if (isCapturingFrontSide) "front" else "back"
            val photoFile = File(directory, "passport_${side}_${System.currentTimeMillis()}.jpg")
            Log.d(TAG, "Saving photo to: ${photoFile.absolutePath}")

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            statusText.text = "Processing image..."

                            if (!photoFile.exists() || photoFile.length() == 0L) {
                                Log.e(TAG, "File was not created or is empty: ${photoFile.absolutePath}")
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@CameraActivity, "Error saving image", Toast.LENGTH_SHORT).show()
                                return
                            }

                            var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap != null) {
                                bitmap = rotateBitmapIfNeeded(bitmap, photoFile)

                                photoFile.outputStream().use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                }

                                if (isCapturingFrontSide) {
                                    frontSidePath = photoFile.absolutePath
                                    isCapturingFrontSide = false
                                    instructionText.text = "Сфотографируйте ОБРАТНУЮ сторону паспорта"
                                    progressBar.visibility = View.GONE
                                    statusText.text = "Ready for back side"

                                    scanLine?.let {
                                        it.visibility = View.VISIBLE
                                        startScanAnimation()
                                    }

                                    Toast.makeText(this@CameraActivity, "Теперь сфотографируйте обратную сторону", Toast.LENGTH_LONG).show()
                                } else {
                                    processWithGemini(frontSidePath!!, photoFile.absolutePath)
                                }
                            } else {
                                Log.e(TAG, "Failed to create bitmap from file")
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@CameraActivity, "Error processing image", Toast.LENGTH_SHORT).show()
                                scanLine?.let {
                                    it.visibility = View.VISIBLE
                                    startScanAnimation()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error after saving image: ", e)
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@CameraActivity, "Image processing error: ${e.message}", Toast.LENGTH_SHORT).show()
                            scanLine?.let {
                                it.visibility = View.VISIBLE
                                startScanAnimation()
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        progressBar.visibility = View.GONE
                        scanLine?.let {
                            it.visibility = View.VISIBLE
                            startScanAnimation()
                        }
                        Log.e(TAG, "Error saving photo: ", exception)
                        Toast.makeText(this@CameraActivity, "Error taking photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                        statusText.text = "Ready to scan"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo: ", e)
            progressBar.visibility = View.GONE
            scanLine?.let {
                it.visibility = View.VISIBLE
                startScanAnimation()
            }
            Toast.makeText(this, "Error taking photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processWithGemini(frontPath: String, backPath: String) {
        statusText.text = "Processing with Gemini API..."
        progressBar.visibility = View.VISIBLE

        val frontBitmap = BitmapFactory.decodeFile(frontPath)
        val backBitmap = BitmapFactory.decodeFile(backPath)

        if (frontBitmap == null || backBitmap == null) {
            Log.e(TAG, "Failed to load bitmaps: front=$frontPath, back=$backPath")
            Toast.makeText(this, "Error loading images", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            scanLine?.let {
                it.visibility = View.VISIBLE
                startScanAnimation()
            }
            return
        }

        val apiKey = "AIzaSyA5k5m8WY4ziIcZS6yzmXTP2ewOXM2CsdU" // Замените на реальный ключ
        val geminiClient = GeminiApiClient(apiKey)

        geminiClient.analyzePassportBothSides(frontBitmap, backBitmap, object : GeminiApiClient.GeminiApiListener {
            override fun onSuccess(passportData: PassportData) {
                // Сохраняем ИНН в SharedPreferences
                val prefs = getSharedPreferences("PassportPrefs", Context.MODE_PRIVATE)
                with(prefs.edit()) {
                    putString("inn", passportData.inn)
                    apply()
                }
                Log.d(TAG, "Extracted and saved INN: ${passportData.inn}")

                // Переходим в FaceRecognitionActivity
                val intent = Intent(this@CameraActivity, FaceRecognitionActivity::class.java)
                intent.putExtra("front_image_path", frontPath)
                intent.putExtra("back_image_path", backPath)
                startActivityForResult(intent, REQUEST_CODE_FACE_RECOGNITION)
            }

            override fun onError(e: Exception) {
                Log.e(TAG, "Gemini API error: ${e.message}")
                Toast.makeText(this@CameraActivity, "Error processing passport: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                scanLine?.let {
                    it.visibility = View.VISIBLE
                    startScanAnimation()
                }
                isCapturingFrontSide = true
                frontSidePath = null
                instructionText.text = "Сфотографируйте ЛИЦЕВУЮ сторону паспорта"
            }

            override fun onFraudDetected(message: String) {
                Log.w(TAG, "Fraud detection bypassed: $message")
                onSuccess(PassportData("", "", "", ""))
            }
        })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FACE_RECOGNITION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val verificationSuccess = data.getBooleanExtra("verification_success", false)
                val similarityPercent = data.getIntExtra("similarity_percent", 0)
                val frontPath = data.getStringExtra("front_image_path")
                val backPath = data.getStringExtra("back_image_path")

                if (verificationSuccess) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("front_image_path", frontPath)
                    resultIntent.putExtra("back_image_path", backPath)
                    resultIntent.putExtra("face_verification_success", true)
                    resultIntent.putExtra("face_similarity_percent", similarityPercent)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this, "Верификация лица не удалась. Попробуйте снова.", Toast.LENGTH_LONG).show()
                    isCapturingFrontSide = true
                    frontSidePath = null
                    instructionText.text = "Сфотографируйте ЛИЦЕВУЮ сторону паспорта"
                    scanLine?.visibility = View.VISIBLE
                    startScanAnimation()
                }
            } else {
                Toast.makeText(this, "Ошибка верификации лица.", Toast.LENGTH_LONG).show()
                isCapturingFrontSide = true
                frontSidePath = null
                instructionText.text = "Сфотографируйте ЛИЦЕВУЮ сторону паспорта"
                scanLine?.visibility = View.VISIBLE
                startScanAnimation()
            }
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_CODE_FACE_RECOGNITION = 11
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}