package com.example.credit_score

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.random.Random

class FaceRecognitionActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var instructionText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        private const val TAG = "FaceRecognitionActivity"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_recognition)

        viewFinder = findViewById(R.id.viewFinder)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.status_text)
        instructionText = findViewById(R.id.instruction_text)

        val frontPath = intent.getStringExtra("front_image_path")
        val backPath = intent.getStringExtra("back_image_path")

        if (allPermissionsGranted()) {
            startCamera()
            startFakeVerification(frontPath, backPath)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                Log.d(TAG, "Camera started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera: ${e.message}")
                Toast.makeText(this, "Ошибка камеры: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFakeVerification(frontPath: String?, backPath: String?) {
        progressBar.visibility = View.VISIBLE

        // Этап 1: Начало проверки (2 секунды)
        statusText.text = "Инициализация проверки лица..."
        instructionText.text = "Пожалуйста, подождите"
        handler.postDelayed({
            stageOne(frontPath, backPath)
        }, 2000)
    }

    private fun stageOne(frontPath: String?, backPath: String?) {
        val initialPercent = Random.nextInt(20, 31)
        statusText.text = "Проверка лица..."
        instructionText.text = "Схожесть: $initialPercent%"
        handler.postDelayed({
            stageTwo(frontPath, backPath)
        }, 3000)
    }

    private fun stageTwo(frontPath: String?, backPath: String?) {
        val shouldFail = Random.nextBoolean()
        if (shouldFail) {
            statusText.text = "Ошибка распознавания"
            instructionText.text = "Повторите еще раз"
            handler.postDelayed({
                stageThree(frontPath, backPath)
            }, 3000)
        } else {
            val midPercent = Random.nextInt(40, 60)
            statusText.text = "Проверка лица..."
            instructionText.text = "Схожесть: $midPercent%"
            handler.postDelayed({
                stageThree(frontPath, backPath)
            }, 3000)
        }
    }

    private fun stageThree(frontPath: String?, backPath: String?) {
        val improvedPercent = Random.nextInt(60, 80)
        statusText.text = "Повторная проверка..."
        instructionText.text = "Схожесть: $improvedPercent%"
        handler.postDelayed({
            stageFour(frontPath, backPath)
        }, 4000)
    }

    private fun stageFour(frontPath: String?, backPath: String?) {
        val finalPercent = Random.nextInt(87, 101)
        statusText.text = "Верификация успешна!"
        instructionText.text = "Схожесть: $finalPercent%"
        progressBar.visibility = View.GONE

        handler.postDelayed({
            returnResult(true, finalPercent, frontPath, backPath)
        }, 2000)
    }

    private fun returnResult(success: Boolean, similarityPercent: Int, frontPath: String?, backPath: String?) {
        val resultIntent = Intent()
        resultIntent.putExtra("verification_success", success)
        resultIntent.putExtra("similarity_percent", similarityPercent)
        resultIntent.putExtra("front_image_path", frontPath)
        resultIntent.putExtra("back_image_path", backPath)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
        Log.d(TAG, "Returning result: success=$success, percent=$similarityPercent")
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
                val frontPath = intent.getStringExtra("front_image_path")
                val backPath = intent.getStringExtra("back_image_path")
                startFakeVerification(frontPath, backPath)
            } else {
                Toast.makeText(this, "Разрешение на камеру не предоставлено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        executor.shutdown()
    }
}