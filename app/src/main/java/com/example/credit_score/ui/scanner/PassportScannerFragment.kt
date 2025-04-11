package com.example.credit_score.ui.scanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.credit_score.R
import com.example.credit_score.data.model.TundukData
import com.example.credit_score.services.TundukService
import com.example.credit_score.utils.PassportScannerFake
import com.example.credit_score.utils.TundukDataFormatter
import kotlinx.coroutines.launch

/**
 * Фрагмент для имитации сканирования паспорта и получения данных из Тундук
 */
class PassportScannerFragment : Fragment(), PassportScannerFake.PassportScanCallback,
    PassportScannerFake.FaceVerificationCallback, TundukService.TundukCallback {
    
    private lateinit var scanButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var resultTextView: TextView
    private lateinit var progressBar: ProgressBar
    
    private val tundukService = TundukService()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_passport_scanner, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализация UI компонентов
        scanButton = view.findViewById(R.id.scan_button)
        statusTextView = view.findViewById(R.id.status_text_view)
        resultTextView = view.findViewById(R.id.result_text_view)
        progressBar = view.findViewById(R.id.progress_bar)
        
        // Обработчик кнопки сканирования
        scanButton.setOnClickListener {
            startScanningProcess()
        }
    }
    
    /**
     * Запускает процесс сканирования паспорта и проверки лица
     */
    private fun startScanningProcess() {
        // Показываем индикатор загрузки и скрываем результаты
        progressBar.visibility = View.VISIBLE
        resultTextView.visibility = View.GONE
        scanButton.isEnabled = false
        
        // Обновляем статус
        statusTextView.text = "Сканирование паспорта..."
        statusTextView.visibility = View.VISIBLE
        
        // Запускаем процесс сканирования паспорта
        lifecycleScope.launch {
            PassportScannerFake.scanPassport(this@PassportScannerFragment)
        }
    }
    
    /**
     * Обработка успешного сканирования паспорта
     */
    override fun onScanComplete(data: PassportScannerFake.PassportData) {
        // Обновляем статус
        statusTextView.text = "Паспорт успешно отсканирован. Проверка лица..."
        
        // Запускаем процесс проверки лица
        lifecycleScope.launch {
            PassportScannerFake.verifyFace(this@PassportScannerFragment)
        }
    }
    
    /**
     * Обработка ошибки сканирования паспорта
     */
    override fun onScanError(errorMessage: String) {
        // Отображаем ошибку
        statusTextView.text = "Ошибка: $errorMessage"
        progressBar.visibility = View.GONE
        scanButton.isEnabled = true
    }
    
    /**
     * Обработка успешной проверки лица
     */
    override fun onVerificationComplete(success: Boolean, similarity: Float) {
        if (success) {
            // Если проверка лица успешна, обновляем статус
            statusTextView.text = "Лицо успешно проверено (сходство ${(similarity * 100).toInt()}%). Запрос данных в Тундук..."
            
            // Запускаем процесс получения данных из Тундук
            // Здесь мы используем один из предопределенных ИНН для демонстрации
            val inn = listOf(
                "20107200450055",
                "20409200500637",
                "21912200400369",
                "21904200500386"
            ).random()
            
            // Запрашиваем данные из Тундук по ИНН
            tundukService.requestDataByInn(inn, this)
        } else {
            // Если проверка лица не успешна, отображаем ошибку
            statusTextView.text = "Ошибка: Лицо не совпадает с фото в паспорте (сходство ${(similarity * 100).toInt()}%)"
            progressBar.visibility = View.GONE
            scanButton.isEnabled = true
        }
    }
    
    /**
     * Обработка ошибки проверки лица
     */
    override fun onVerificationError(errorMessage: String) {
        // Отображаем ошибку
        statusTextView.text = "Ошибка: $errorMessage"
        progressBar.visibility = View.GONE
        scanButton.isEnabled = true
    }
    
    /**
     * Обработка успешного получения данных из Тундук
     */
    override fun onSuccess(data: TundukData) {
        // Обновляем статус
        statusTextView.text = "Данные успешно получены из Тундук"
        
        // Форматируем и отображаем данные
        val formattedData = TundukDataFormatter.formatTundukDataToText(data)
        resultTextView.text = formattedData
        resultTextView.visibility = View.VISIBLE
        
        // Скрываем индикатор загрузки и делаем кнопку снова активной
        progressBar.visibility = View.GONE
        scanButton.isEnabled = true
    }
    
    /**
     * Обработка ошибки получения данных из Тундук
     */
    override fun onError(errorMessage: String) {
        // Отображаем ошибку
        statusTextView.text = "Ошибка получения данных из Тундук: $errorMessage"
        progressBar.visibility = View.GONE
        scanButton.isEnabled = true
    }
}
