package com.example.credit_score.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.credit_score.CameraActivity
import com.example.credit_score.R
import com.example.credit_score.databinding.FragmentDashboardBinding
import com.example.credit_score.remote.gemini.GeminiApiClient
import com.example.credit_score.remote.gemini.PassportData
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import androidx.navigation.fragment.findNavController
import com.example.credit_score.ui.dashboard.DashboardFragmentDirections

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var geminiApiClient: GeminiApiClient
    private var isProcessing = false

    // Лаунчер для выбора изображения из галереи
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                    displayImage(bitmap)
                    processImage(bitmap)
                } catch (e: IOException) {
                    Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Ошибка загрузки изображения из галереи", e)
                }
            }
        }
    }

    // Лаунчер для получения изображения с камеры (модифицирован для обеих сторон)
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val frontImagePath = result.data?.getStringExtra("front_image_path")
            val backImagePath = result.data?.getStringExtra("back_image_path")
            val faceVerificationSuccess = result.data?.getBooleanExtra("face_verification_success", false) ?: false
            val faceSimilarityPercent = result.data?.getIntExtra("face_similarity_percent", 0) ?: 0

            if (frontImagePath != null && backImagePath != null) {
                try {
                    val frontBitmap = BitmapFactory.decodeFile(frontImagePath)
                    val backBitmap = BitmapFactory.decodeFile(backImagePath)

                    // Отображаем лицевую сторону в интерфейсе
                    displayImage(frontBitmap)

                    if (faceVerificationSuccess) {
                        // Лицо верифицировано, продолжаем анализ паспорта
                        Toast.makeText(
                            requireContext(),
                            "Верификация лица успешна: $faceSimilarityPercent%",
                            Toast.LENGTH_LONG
                        ).show()
                        processBothImages(frontBitmap, backBitmap)
                    } else {
                        // Лицо не верифицировано
                        Toast.makeText(
                            requireContext(),
                            "Верификация лица не удалась ($faceSimilarityPercent%)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка загрузки изображений: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Ошибка обработки результатов с камеры", e)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализируем клиент Gemini API
        geminiApiClient = GeminiApiClient(getString(R.string.gemini_api_key))

        // Настраиваем кнопки
        setupButtons()
        setupButtonAnimations()
    }

    private fun setupButtons() {
        // Кнопка выбора изображения из галереи
        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // Кнопка открытия камеры
        binding.btnScanWithCamera.setOnClickListener {
            openCameraScanner()
        }
    }

    private fun setupButtonAnimations() {
        // Загружаем анимацию пульсации
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale)

        // Добавляем слушатели касания для анимации кнопок
        binding.btnSelectImage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation)
            }
            false
        }

        binding.btnScanWithCamera.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation)
            }
            false
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        Dexter.withContext(requireContext())
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    openImagePicker()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    Toast.makeText(requireContext(), "Необходимо разрешение для выбора изображения", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun openCameraScanner() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun displayImage(bitmap: Bitmap) {
        binding.tvPlaceholder.visibility = View.GONE
        Glide.with(this).load(bitmap).into(binding.imageView)
    }

    // Новый метод для обработки обеих сторон паспорта
    private fun processBothImages(frontBitmap: Bitmap, backBitmap: Bitmap) {
        if (isProcessing) return

        isProcessing = true
        showLoading(true)
        hideResult()

        geminiApiClient.analyzePassportBothSides(frontBitmap, backBitmap, object : GeminiApiClient.GeminiApiListener {
            override fun onSuccess(passportData: PassportData) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    displayResult(passportData)
                }
            }

            override fun onError(e: Exception) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    Toast.makeText(requireContext(), "Ошибка анализа изображений: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Ошибка анализа изображений", e)
                }
            }
            override fun onFraudDetected(message: String) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    Toast.makeText(requireContext(), "Обнаружено мошенничество: $message", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Обнаружено мошенничество: $message")
                }
            }
        })
    }

    private fun processImage(bitmap: Bitmap) {
        if (isProcessing) return

        isProcessing = true
        showLoading(true)
        hideResult()

        geminiApiClient.analyzePassport(bitmap, object : GeminiApiClient.GeminiApiListener {
            override fun onSuccess(passportData: PassportData) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    displayResult(passportData)
                }
            }

            override fun onError(e: Exception) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    Toast.makeText(requireContext(), "Ошибка анализа изображения: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Ошибка анализа изображения", e)
                }
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        val fadeAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_out)

        if (isLoading) {
            binding.progressBar.startAnimation(fadeAnimation)
            binding.tvLoading.startAnimation(fadeAnimation)
            binding.tvPlaceholder.visibility = View.GONE
        }

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSelectImage.isEnabled = !isLoading
        binding.btnScanWithCamera.isEnabled = !isLoading
    }

    private fun hideResult() {
        binding.cardResult.visibility = View.GONE
    }

    private fun displayResult(passportData: PassportData) {
        val resultBuilder = StringBuilder()

        if (passportData.inn.isNotEmpty()) {
            resultBuilder.append("ИНН: ").append(passportData.inn).append("\n\n")
        }

        if (passportData.fullName.isNotEmpty()) {
            resultBuilder.append("ФИО: ").append(passportData.fullName).append("\n\n")
        }

        if (passportData.birthDate.isNotEmpty()) {
            resultBuilder.append("Дата рождения: ").append(passportData.birthDate).append("\n\n")
        }

        if (passportData.passportNumber.isNotEmpty()) {
            resultBuilder.append("Номер паспорта: ").append(passportData.passportNumber)
        }

        binding.tvGarbageType.text = "Анализ паспорта:"
        binding.tvInstructions.text = resultBuilder.toString().trim()

        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.cardResult.visibility = View.VISIBLE
        binding.cardResult.startAnimation(slideUpAnimation)

        Toast.makeText(
            requireContext(),
            "ИНН: ${passportData.inn}, ФИО: ${passportData.fullName}",
            Toast.LENGTH_LONG
        ).show()

        // Передача ИНН в HomeFragment
        if (passportData.inn.isNotEmpty()) {
            if (isAdded && !isDetached && view != null) {
                try {
                    val action = DashboardFragmentDirections.actionNavigationDashboardToNavigationHome(passportData.inn)
                    findNavController().navigate(action)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Navigation failed: Fragment not associated with NavController", e)
                    Toast.makeText(
                        requireContext(),
                        "Не удалось перейти на страницу с данными. Попробуйте снова.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.w(TAG, "Cannot navigate: Fragment is not in a valid state")
                Toast.makeText(
                    requireContext(),
                    "Фрагмент не готов для перехода. Попробуйте снова.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DashboardFragment"
    }
}