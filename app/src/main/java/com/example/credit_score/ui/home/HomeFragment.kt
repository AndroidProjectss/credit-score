package com.example.credit_score.ui.home

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.credit_score.R
import com.example.credit_score.data.MockDatabase
import com.example.credit_score.data.UserData
import com.example.credit_score.databinding.FragmentHomeBinding
import com.example.credit_score.remote.gemini.SearchQuery
import com.example.creditscore.remote.gemini.CreditApplicationData
import com.example.creditscore.remote.gemini.GeminiApiClientScoring
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var scoringResult: SearchQuery? = null
    private lateinit var geminiApiClient: GeminiApiClientScoring
    private var currentUserData: UserData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Gemini API client with API key
        val apiKey = getString(R.string.gemini_api_key)
        geminiApiClient = GeminiApiClientScoring(apiKey)

        setupInputValidation()
        setupListeners()
    }
    
    private fun setupInputValidation() {
        // Валидация ИНН - должен быть 14 символов
        binding.innInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val inn = s.toString()
                if (inn.length == 14) {
                    // Проверяем, есть ли ИНН в базе данных
                    val userData = MockDatabase.getUserByInn(inn)
                    if (userData != null) {
                        currentUserData = userData
                        Toast.makeText(context, "Пользователь найден: ${userData.fullName}", Toast.LENGTH_SHORT).show()
                    } else {
                        currentUserData = null
                        Toast.makeText(context, "Пользователь с таким ИНН не найден", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    currentUserData = null
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnAnalyze.setOnClickListener {
            // Валидация введенных данных
            if (!validateInputs()) {
                return@setOnClickListener
            }

            // Показать состояние загрузки
            binding.progressBar.visibility = View.VISIBLE
            binding.btnAnalyze.isEnabled = false
            binding.inputCardView.visibility = View.GONE
            binding.resultsScrollView.visibility = View.GONE

            val userData = currentUserData ?: return@setOnClickListener
            
            // Получаем данные из формы
            val requestedAmount = binding.amountInput.text.toString().toIntOrNull() ?: 0
            val requestedTerm = binding.termInput.text.toString().toIntOrNull() ?: 0
            val purpose = binding.purposeInput.text.toString()

            val applicationData = createCreditApplicationFromUserData(userData, requestedAmount, requestedTerm, purpose)

            geminiApiClient.analyzeCreditApplication(
                applicationData,
                object : GeminiApiClientScoring.GeminiApiListener {
                    override fun onSuccess(searchQuery: SearchQuery) {
                        // Важно! Обновления UI должны быть в главном потоке
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            binding.inputCardView.visibility = View.GONE
                            
                            scoringResult = searchQuery
                            // Показываем ScrollView с результатами
                            binding.resultsScrollView.visibility = View.VISIBLE
                            setupUI(userData)
                        }
                    }

                    override fun onError(e: Exception) {
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            binding.btnAnalyze.isEnabled = true
                            binding.inputCardView.visibility = View.VISIBLE

                            Log.e("HomeFragment", "API Error", e)
                            Toast.makeText(
                                requireContext(),
                                "Ошибка анализа: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }
        
        binding.btnNewRequest.setOnClickListener {
            // Сбрасываем форму и показываем ее снова
            resetForm()
            binding.inputCardView.visibility = View.VISIBLE
            binding.resultsScrollView.visibility = View.GONE
            binding.btnAnalyze.isEnabled = true
            currentUserData = null
        }
    }
    
    private fun validateInputs(): Boolean {
        // Проверка ИНН
        if (currentUserData == null) {
            Toast.makeText(context, "Пользователь с таким ИНН не найден", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Проверка суммы
        val amount = binding.amountInput.text.toString().toIntOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Введите корректную сумму кредита", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Проверка срока
        val term = binding.termInput.text.toString().toIntOrNull()
        if (term == null || term <= 0 || term > 120) {
            Toast.makeText(context, "Введите корректный срок кредита (1-120 месяцев)", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Проверка цели
        if (binding.purposeInput.text.toString().isBlank()) {
            Toast.makeText(context, "Укажите цель кредита", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun resetForm() {
        binding.innInput.text?.clear()
        binding.amountInput.text?.clear()
        binding.termInput.text?.clear()
        binding.purposeInput.text?.clear()
    }

    private fun createCreditApplicationFromUserData(
        userData: UserData,
        requestedAmount: Int,
        requestedTerm: Int,
        purpose: String
    ): CreditApplicationData {
        return CreditApplicationData(
            fullName = userData.fullName,
            passportData = userData.passportData,
            registrationAddress = userData.registrationAddress,
            workExperience = userData.workExperience,
            monthlyIncome = userData.monthlyIncome,
            monthlyExpenses = userData.monthlyExpenses,
            hasTaxDebt = userData.hasTaxDebt,
            taxDebtAmount = userData.taxDebtAmount,
            employmentStatus = userData.employmentStatus,
            familyStatus = userData.familyStatus,
            requestedAmount = requestedAmount,
            requestedTerm = requestedTerm,
            purpose = purpose,
            creditHistory = userData.creditHistory
        )
    }

    private fun setupUI(userData: UserData) {
        scoringResult?.let { result ->
            // Добавляем доход и расходы пользователя в результат скоринга,
            // если они не были установлены в ответе API
            val updatedResult = if (result.monthlyIncome == null || result.monthlyExpenses == null) {
                result.copy(
                    monthlyIncome = userData.monthlyIncome,
                    monthlyExpenses = userData.monthlyExpenses
                )
            } else {
                result
            }
            
            // Сохраняем обновленный результат
            scoringResult = updatedResult
            
            // Настраиваем спидометр скорингового балла
            binding.scoreGauge.apply {
                maxSpeed = 100f
                speedTo(updatedResult.overallScore.toFloat(), 1000)
                speedTextSize = 60f
                unit = "балл"
                unitTextSize = 24f
            }

            // Устанавливаем уровень риска с соответствующим цветом
            binding.riskLevelText.text = updatedResult.creditRiskLevel
            binding.riskLevelText.setTextColor(getRiskColor(updatedResult.creditRiskLevel))

            // Заполняем основную информацию о заявителе
            binding.fullNameValue.text = userData.fullName
            binding.employmentStatusValue.text = userData.employmentStatus
            binding.incomeValue.text = "${formatNumber(userData.monthlyIncome)} сом"
            binding.creditHistoryValue.text = userData.creditHistory

            // Заполняем рекомендуемые условия кредита
            binding.maxAmountValue.text = "${formatNumber(updatedResult.maxRecommendedAmount)} сом"
            binding.maxTermValue.text = "${updatedResult.maxRecommendedTerm} месяцев"
            binding.interestRateValue.text = String.format(Locale.US, "%.1f%%", updatedResult.recommendedInterestRate)

            // Устанавливаем обоснование решения
            binding.justificationText.text = updatedResult.justification

            // Настраиваем списки сильных и слабых сторон, рекомендаций
            setupStrengthsAndWeaknesses(updatedResult)
            setupRecommendations(updatedResult)
            
            // Настраиваем диаграмму соотношения платежа к доходу
            setupDebtToIncomeChart(updatedResult)
        }
    }

    private fun setupStrengthsAndWeaknesses(result: SearchQuery) {
        binding.strengthsContainer.removeAllViews()
        binding.weaknessesContainer.removeAllViews()

        result.strengths.forEach { strength ->
            val strengthView = LayoutInflater.from(context).inflate(
                R.layout.item_strength,
                binding.strengthsContainer,
                false
            )
            strengthView.findViewById<TextView>(R.id.strengthText)?.text = strength
            binding.strengthsContainer.addView(strengthView)
        }

        result.weaknesses.forEach { weakness ->
            val weaknessView = LayoutInflater.from(context).inflate(
                R.layout.item_weakness,
                binding.weaknessesContainer,
                false
            )
            weaknessView.findViewById<TextView>(R.id.weaknessText)?.text = weakness
            binding.weaknessesContainer.addView(weaknessView)
        }
    }

    private fun setupRecommendations(result: SearchQuery) {
        binding.recommendationsContainer.removeAllViews()

        result.recommendations.forEach { recommendation ->
            val recommendationView = LayoutInflater.from(context).inflate(
                R.layout.item_recommendation,
                binding.recommendationsContainer,
                false
            )
            recommendationView.findViewById<TextView>(R.id.recommendationText)?.text = recommendation
            binding.recommendationsContainer.addView(recommendationView)
        }
    }

    private fun setupDebtToIncomeChart(result: SearchQuery) {
        val chart = binding.debtToIncomeChart

        // Всегда очищаем предыдущие данные в графике
        chart.clear()
        
        // Получаем значения дохода и расходов
        val income = result.monthlyIncome ?: currentUserData?.monthlyIncome ?: 0
        val expenses = result.monthlyExpenses ?: currentUserData?.monthlyExpenses ?: 0
        
        // Проверяем, что у нас есть данные для отображения
        if (income <= 0) {
            Log.w("HomeFragment", "Income is zero or negative, cannot calculate DTI chart.")
            chart.setNoDataText("Недостаточно данных")
            chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_text))
            chart.visibility = View.VISIBLE
            return
        }
        
        // Рассчитываем платеж по кредиту
        val estimatedMonthlyPayment = if (result.maxRecommendedAmount > 0 && result.maxRecommendedTerm > 0 && result.recommendedInterestRate >= 0) {
            calculateEstimatedMonthlyPayment(
                result.maxRecommendedAmount,
                result.recommendedInterestRate,
                result.maxRecommendedTerm
            )
        } else {
            0.0
        }
        
        // Конвертируем в float для использования в графике
        val paymentFloat = estimatedMonthlyPayment.toFloat()
        
        // Если платеж слишком мал, установим минимальное значение для видимости на графике
        val minDisplayValue = income * 0.01f  // 1% от дохода
        val displayPayment = if (paymentFloat < minDisplayValue && paymentFloat > 0) minDisplayValue else paymentFloat
        
        // Создаем данные для графика
        val entries = ArrayList<PieEntry>()
        
        // Добавляем платеж (если больше 0)
        if (paymentFloat > 0) {
            entries.add(PieEntry(displayPayment, "Платеж по кредиту"))
        }
        
        // Добавляем другие расходы (если больше 0)
        if (expenses > 0) {
            entries.add(PieEntry(expenses.toFloat(), "Расходы"))
        }
        
        // Рассчитываем оставшийся доход после платежа и расходов
        val remainingIncome = maxOf(0f, income.toFloat() - paymentFloat - expenses.toFloat())
        
        // Добавляем оставшийся доход (если больше 0)
        if (remainingIncome > 0) {
            entries.add(PieEntry(remainingIncome, "Свободный доход"))
        }
        
        // Если нет данных, показываем весь доход
        if (entries.isEmpty()) {
            entries.add(PieEntry(income.toFloat(), "Доход"))
        }
        
        // Создаем набор данных
        val dataSet = PieDataSet(entries, "")
        
        // Устанавливаем цвета для разных секторов
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.chart_debt),      // Платеж по кредиту - красный
            ContextCompat.getColor(requireContext(), R.color.chart_expenses),  // Расходы - оранжевый
            ContextCompat.getColor(requireContext(), R.color.chart_income)     // Свободный доход - зеленый
        )
        
        // Добавляем отступ между секторами для лучшей читаемости
        dataSet.sliceSpace = 2f
        
        // Создаем объект данных и устанавливаем форматтер для отображения процентов
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(chart))
        data.setValueTextSize(14f)
        data.setValueTextColor(Color.WHITE)
        
        // Настраиваем внешний вид графика
        chart.apply {
            this.data = data
            description.isEnabled = false
            
            // Настраиваем отверстие в центре и прозрачный круг
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f
            transparentCircleRadius = 55f
            
            // Настраиваем легенду
            legend.isEnabled = true
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textSize = 12f
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.primary_text)
            
            // Настраиваем подписи к секторам
            setDrawEntryLabels(false)  // Отключаем подписи на секторах для чистоты
            setUsePercentValues(true)  // Показываем проценты
            
            // Устанавливаем текст в центре
            setDrawCenterText(true)
            val dtiPercentage = if (income > 0) (paymentFloat / income.toFloat()) * 100 else 0f
            centerText = String.format(Locale.US, "DTI\n%.1f%%", dtiPercentage)
            setCenterTextSize(16f)
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text))
            
            // Добавляем анимацию для красивого отображения
            animateY(1400, Easing.EaseInOutQuad)
            
            // Отключаем взаимодействие с графиком для предотвращения ошибок
            setTouchEnabled(false)
            
            // Обновляем график
            invalidate()
        }
        
        // Показываем график
        chart.visibility = View.VISIBLE
    }

    private fun calculateEstimatedMonthlyPayment(
        principal: Int,
        annualInterestRate: Double,
        termInMonths: Int
    ): Double {
        if (principal <= 0 || termInMonths <= 0 || annualInterestRate < 0) {
            return 0.0
        }
        if (annualInterestRate == 0.0) {
            return principal.toDouble() / termInMonths
        }

        val monthlyRate = annualInterestRate / 100.0 / 12.0
        val powerTerm = Math.pow(1.0 + monthlyRate, termInMonths.toDouble())

        if (powerTerm - 1 == 0.0) return 0.0

        return principal * monthlyRate * powerTerm / (powerTerm - 1)
    }

    private fun getRiskColor(riskLevel: String?): Int {
        val colorResId = when (riskLevel?.lowercase(Locale.ROOT)) {
            "низкий" -> R.color.risk_low
            "средний" -> R.color.risk_medium
            "высокий" -> R.color.risk_high
            "очень высокий" -> R.color.risk_very_high
            else -> R.color.risk_medium
        }
        return ContextCompat.getColor(requireContext(), colorResId)
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale("ru", "RU")).format(number)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}