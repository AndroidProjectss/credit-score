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

    private var isCapturingFrontSide = true // Track which side we're capturing
    private var frontSidePath: String? = null // Path to front side image

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
            // Initialize UI components
            viewFinder = findViewById(R.id.viewFinder)
            captureButton = findViewById(R.id.capture_button)
            progressBar = findViewById(R.id.progressBar)
            statusText = findViewById(R.id.status_text)
            instructionText = findViewById(R.id.instruction_text)

            // Initialize scanning line and animation
            scanLine = findViewById(R.id.scanLine)
            scanFrame = findViewById(R.id.scanFrame)
            scanAnimation = AnimationUtils.loadAnimation(this, R.anim.scan_line_animation)

            val pulseButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_capture_button)
            captureButton.startAnimation(pulseButtonAnimation)

            // Set initial instruction text
            instructionText.text = "Сфотографируйте ЛИЦЕВУЮ сторону паспорта"

            // Check permissions
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }

            // Set up capture button
            captureButton.setOnClickListener { takePhoto() }

            // Start scan animation
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
            // Set up preview
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            // Set up camera selector (use back camera)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Set up image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetResolution(android.util.Size(1920, 1080))
                .setTargetRotation(viewFinder.display.rotation) // Устанавливаем ориентацию на основе дисплея
                .build()

            // Remove all bindings before adding new ones
            cameraProvider.unbindAll()

            // Bind camera to lifecycle
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            statusText.text = "Camera ready"
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera: ", e)
            Toast.makeText(this, "Camera initialization error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, photoFile: File): Bitmap {
        // Читаем EXIF-данные для определения ориентации
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

        // Проверяем размеры: если высота больше ширины, поворачиваем на 90 градусов
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
            // Stop scanning animation
            scanLine?.let {
                it.clearAnimation()
                it.visibility = View.INVISIBLE
            }

            // Show progress
            progressBar.visibility = View.VISIBLE
            statusText.text = "Taking photo..."

            // Create directory if needed
            val directory = filesDir
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Create temporary file for photo with unique name
            val side = if (isCapturingFrontSide) "front" else "back"
            val photoFile = File(directory, "passport_${side}_${System.currentTimeMillis()}.jpg")
            Log.d(TAG, "Saving photo to: ${photoFile.absolutePath}")

            // Create output options
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Take photo
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            statusText.text = "Processing image..."

                            // Check if file exists
                            if (!photoFile.exists() || photoFile.length() == 0L) {
                                Log.e(TAG, "File was not created or is empty: ${photoFile.absolutePath}")
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@CameraActivity, "Error saving image", Toast.LENGTH_SHORT).show()
                                return
                            }

                            // Create Bitmap from file
                            var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap != null) {
                                // Поворачиваем изображение, если нужно
                                bitmap = rotateBitmapIfNeeded(bitmap, photoFile)

                                // Сохраняем повернутое изображение обратно в файл
                                photoFile.outputStream().use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                }

                                if (isCapturingFrontSide) {
                                    // Save the path to front side image
                                    frontSidePath = photoFile.absolutePath

                                    // Prepare for back side capture
                                    isCapturingFrontSide = false
                                    instructionText.text = "Сфотографируйте ОБРАТНУЮ сторону паспорта"

                                    // Reset UI for next photo
                                    progressBar.visibility = View.GONE
                                    statusText.text = "Ready for back side"

                                    // Restart animation
                                    scanLine?.let {
                                        it.visibility = View.VISIBLE
                                        startScanAnimation()
                                    }

                                    Toast.makeText(this@CameraActivity, "Теперь сфотографируйте обратную сторону", Toast.LENGTH_LONG).show()
                                } else {
                                    // We have both sides, return result
                                    returnBothSidesResult(frontSidePath!!, photoFile.absolutePath)
                                }
                            } else {
                                Log.e(TAG, "Failed to create bitmap from file")
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@CameraActivity, "Error processing image", Toast.LENGTH_SHORT).show()

                                // Restart animation in case of error
                                scanLine?.let {
                                    it.visibility = View.VISIBLE
                                    startScanAnimation()
                                }
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error after saving image: ", e)
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@CameraActivity, "Image processing error: ${e.message}", Toast.LENGTH_SHORT).show()

                            // Restart animation in case of error
                            scanLine?.let {
                                it.visibility = View.VISIBLE
                                startScanAnimation()
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        progressBar.visibility = View.GONE

                        // Restart animation in case of error
                        scanLine?.let {
                            it.visibility = View.VISIBLE
                            startScanAnimation()
                        }

                        Log.e(TAG, "Error saving photo: ", exception)
                        Toast.makeText(
                            this@CameraActivity,
                            "Error taking photo: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        statusText.text = "Ready to scan"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo: ", e)
            progressBar.visibility = View.GONE

            // Restart animation in case of error
            scanLine?.let {
                it.visibility = View.VISIBLE
                startScanAnimation()
            }

            Toast.makeText(this, "Error taking photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun returnBothSidesResult(frontPath: String, backPath: String) {
        // Запускаем FaceRecognitionActivity для биометрической проверки
        val intent = Intent(this, FaceRecognitionActivity::class.java)
        intent.putExtra("passport_face_path", frontPath) // Передаем путь к лицевой стороне
        intent.putExtra("back_image_path", backPath) // Передаем путь к обратной стороне для дальнейшего использования
        startActivityForResult(intent, REQUEST_CODE_FACE_RECOGNITION)
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
                val frontPath = intent.getStringExtra("front_image_path")
                val backPath = intent.getStringExtra("back_image_path")

                if (verificationSuccess) {
                    // Успешная верификация, возвращаем результат в DashboardFragment
                    val resultIntent = Intent()
                    resultIntent.putExtra("front_image_path", frontPath)
                    resultIntent.putExtra("back_image_path", backPath)
                    resultIntent.putExtra("face_verification_success", true)
                    resultIntent.putExtra("face_similarity_percent", similarityPercent)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    // Неуспешная верификация, показываем сообщение и остаемся в CameraActivity
                    Toast.makeText(this, "Верификация лица не удалась. Попробуйте снова.", Toast.LENGTH_LONG).show()
                    // Сбрасываем состояние для повторного сканирования
                    isCapturingFrontSide = true
                    frontSidePath = null
                    instructionText.text = "Сфотографируйте ЛИЦЕВУЮ сторону паспорта"
                    scanLine?.visibility = View.VISIBLE
                    startScanAnimation()
                }
            } else {
                // Ошибка или отмена, показываем сообщение
                Toast.makeText(this, "Ошибка верификации лица.", Toast.LENGTH_LONG).show()
                // Сбрасываем состояние
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