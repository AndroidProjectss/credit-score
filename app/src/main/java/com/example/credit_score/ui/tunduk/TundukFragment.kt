package com.example.credit_score.ui.tunduk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.credit_score.R
import com.example.credit_score.utils.TundukDataFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Фрагмент для демонстрации работы с Тундук API
 */
class TundukFragment : Fragment() {
    
    private lateinit var viewModel: TundukViewModel
    
    private lateinit var innEditText: TextInputEditText
    private lateinit var innInputLayout: TextInputLayout
    private lateinit var searchButton: MaterialButton
    private lateinit var resultTextView: TextView
    private lateinit var resultScrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var errorTextView: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tunduk, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализация ViewModel
        viewModel = ViewModelProvider(this)[TundukViewModel::class.java]
        
        // Инициализация UI компонентов
        innEditText = view.findViewById(R.id.inn_edit_text)
        innInputLayout = view.findViewById(R.id.inn_input_layout)
        searchButton = view.findViewById(R.id.search_button)
        resultTextView = view.findViewById(R.id.result_text_view)
        resultScrollView = view.findViewById(R.id.result_scroll_view)
        progressBar = view.findViewById(R.id.progress_bar)
        loadingAnimation = view.findViewById(R.id.loading_animation)
        errorTextView = view.findViewById(R.id.error_text_view)
        
        // Обработчик кнопки поиска
        searchButton.setOnClickListener {
            val inn = innEditText.text.toString().trim()
            if (inn.isNotEmpty()) {
                // Скрываем клавиатуру
                hideKeyboard()
                // Сбрасываем состояние ошибки поля ввода
                innInputLayout.error = null
                // Запрашиваем данные по ИНН
                viewModel.getClientDataByInn(inn)
            } else {
                // Показываем ошибку в поле ввода
                innInputLayout.error = "Введите ИНН"
            }
        }
        
        // Наблюдение за состоянием загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // Показываем анимацию загрузки
                loadingAnimation.visibility = View.VISIBLE
                // Скрываем результаты и ошибки
                resultScrollView.visibility = View.GONE
                errorTextView.visibility = View.GONE
                // Отключаем кнопку поиска
                searchButton.isEnabled = false
            } else {
                // Скрываем анимацию загрузки
                loadingAnimation.visibility = View.GONE
                // Включаем кнопку поиска
                searchButton.isEnabled = true
            }
        }
        
        // Наблюдение за данными клиента
        viewModel.clientData.observe(viewLifecycleOwner) { clientData ->
            if (clientData != null) {
                // Форматируем и отображаем данные
                val formattedData = TundukDataFormatter.formatTundukDataToText(clientData)
                resultTextView.text = formattedData
                // Показываем результаты
                resultScrollView.visibility = View.VISIBLE
                // Скрываем ошибки
                errorTextView.visibility = View.GONE
            } else {
                // Скрываем результаты если данных нет
                resultScrollView.visibility = View.GONE
            }
        }
        
        // Наблюдение за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                // Отображаем ошибку
                errorTextView.text = errorMessage
                errorTextView.visibility = View.VISIBLE
                // Скрываем результаты
                resultScrollView.visibility = View.GONE
            } else {
                // Скрываем ошибки если нет сообщения об ошибке
                errorTextView.visibility = View.GONE
            }
        }
    }
    
    /**
     * Скрывает клавиатуру
     */
    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
