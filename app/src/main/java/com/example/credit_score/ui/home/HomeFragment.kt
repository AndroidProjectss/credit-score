package com.example.credit_score.ui.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log // Добавим для отладки, если понадобится
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.credit_score.R
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
    // Предполагаем, что GeminiApiClientScoring и SearchQuery определены где-то
    private lateinit var geminiApiClient: GeminiApiClientScoring

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

        // Initialize Gemini API client with your API key
        // Убедись, что ключ R.string.gemini_api_key существует в strings.xml
        val apiKey = getString(R.string.gemini_api_key)
        geminiApiClient = GeminiApiClientScoring(apiKey)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnAnalyze.setOnClickListener {
            // Show loading state
            binding.progressBar.visibility = View.VISIBLE
            binding.btnAnalyze.isEnabled = false
            // Скрываем ScrollView с результатами
            binding.resultsScrollView.visibility = View.GONE

            val sampleData = createSampleCreditApplication()

            geminiApiClient.analyzeCreditApplication(
                sampleData,
                object : GeminiApiClientScoring.GeminiApiListener {
                    override fun onSuccess(searchQuery: SearchQuery) {
                        // Важно! Обновления UI должны быть в главном потоке
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            // Кнопку можно скрыть, т.к. результаты показаны
                            // binding.btnAnalyze.isEnabled = true // Уже не нужно, если кнопка скрывается
                            binding.btnAnalyze.visibility = View.GONE // Скрываем кнопку

                            scoringResult = searchQuery
                            // Показываем ScrollView с результатами
                            binding.resultsScrollView.visibility = View.VISIBLE
                            setupUI()
                        }
                    }

                    override fun onError(e: Exception) {
                        activity?.runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            binding.btnAnalyze.isEnabled = true // Оставляем кнопку доступной для повторной попытки
                            // Можно также показать btnAnalyze снова, если он был скрыт
                            // binding.btnAnalyze.visibility = View.VISIBLE

                            Log.e("HomeFragment", "API Error", e) // Логируем ошибку для отладки
                            Toast.makeText(
                                requireContext(),
                                "Ошибка анализа: ${e.message}", // Более информативное сообщение
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }
    }

    private fun createSampleCreditApplication(): CreditApplicationData {
        return CreditApplicationData(
            fullName = "Азамат Алиев",
            passportData = "AN 1234567",
            registrationAddress = "г. Бишкек, ул. Чуй, д. 123, кв. 45",
            workExperience = 5,
            monthlyIncome = 35000,
            monthlyExpenses = 15000,
            hasTaxDebt = false,
            employmentStatus = "Официально трудоустроен",
            familyStatus = "Женат, 2 детей",
            requestedAmount = 200000,
            requestedTerm = 24,
            purpose = "Бытовая техника",
            creditHistory = "Положительная, без просрочек"
        )
    }

    private fun setupUI() {
        scoringResult?.let { result ->
            // Set up score gauge using correct methods/properties
            binding.scoreGauge.apply {
                // Убедись, что overallScore - это число от 0 до 100 (или какой maxSpeed установлен)
                maxSpeed = 100f // Устанавливаем максимум шкалы
                speedTo(result.overallScore.toFloat(), 1000) // Устанавливаем значение с анимацией (1000ms)
                // setProgressTextFormat не существует, текст настраивается иначе
                speedTextSize = 60f // Размер текста значения скорости (балла)
                unit = "балл"      // Текст единицы измерения
                unitTextSize = 24f // Размер текста единицы измерения
                // Дополнительные настройки для красоты, если нужно
                // tickNumber = 11 // Количество делений
                // tickPadding = 10f
            }

            binding.riskLevelText.text = result.creditRiskLevel
            binding.riskLevelText.setTextColor(getRiskColor(result.creditRiskLevel))

            binding.maxAmountValue.text = "${formatNumber(result.maxRecommendedAmount)} сом"
            binding.maxTermValue.text = "${result.maxRecommendedTerm} месяцев"
            // Убедимся, что ставка форматируется корректно (например, 18.5)
            binding.interestRateValue.text = String.format(Locale.US, "%.1f%%", result.recommendedInterestRate)

            binding.justificationText.text = result.justification

            setupStrengthsAndWeaknesses(result)
            setupRecommendations(result)
            setupDebtToIncomeChart(result)
        }
    }

    private fun setupStrengthsAndWeaknesses(result: SearchQuery) {
        binding.strengthsContainer.removeAllViews()
        binding.weaknessesContainer.removeAllViews()

        // Используем R.layout.* для разметки элемента списка
        result.strengths.forEach { strength ->
            val strengthView = LayoutInflater.from(context).inflate(
                R.layout.item_strength, // ИСПРАВЛЕНО: R.layout вместо R.drawable
                binding.strengthsContainer,
                false
            )
            // Убедись, что ID R.id.strengthText существует в item_strength.xml
            strengthView.findViewById<TextView>(R.id.strengthText)?.text = strength
            binding.strengthsContainer.addView(strengthView)
        }

        result.weaknesses.forEach { weakness ->
            val weaknessView = LayoutInflater.from(context).inflate(
                R.layout.item_weakness, // ИСПРАВЛЕНО: R.layout вместо R.drawable
                binding.weaknessesContainer,
                false
            )
            // Убедись, что ID R.id.weaknessText существует в item_weakness.xml
            weaknessView.findViewById<TextView>(R.id.weaknessText)?.text = weakness
            binding.weaknessesContainer.addView(weaknessView)
        }
    }

    private fun setupRecommendations(result: SearchQuery) {
        binding.recommendationsContainer.removeAllViews()

        result.recommendations.forEach { recommendation ->
            val recommendationView = LayoutInflater.from(context).inflate(
                R.layout.item_recommendation, // ИСПРАВЛЕНО: R.layout вместо R.drawable
                binding.recommendationsContainer,
                false
            )
            // Убедись, что ID R.id.recommendationText существует в item_recommendation.xml
            recommendationView.findViewById<TextView>(R.id.recommendationText)?.text = recommendation
            binding.recommendationsContainer.addView(recommendationView)
        }
    }

    private fun setupDebtToIncomeChart(result: SearchQuery) {
        val chart = binding.debtToIncomeChart

        // Используем безопасный вызов и значения по умолчанию
        val income = result.monthlyIncome ?: 0
        val expenses = result.monthlyExpenses ?: 0
        val availableIncome = income - expenses

        // Проверяем, что доступный доход положительный перед расчетом
        if (availableIncome <= 0) {
            Log.w("HomeFragment", "Available income is zero or negative, cannot calculate DTI chart.")
            chart.visibility = View.GONE // Скрываем чарт, если данных недостаточно
            // Можно также показать сообщение пользователю
            // binding.dtiInfoText.text = "Недостаточно данных для расчета DTI"
            // binding.dtiInfoText.visibility = View.VISIBLE
            return
        }
        chart.visibility = View.VISIBLE // Показываем чарт, если скрывали

        // Рассчитываем платеж только если сумма и срок > 0 и ставка >= 0
        val estimatedMonthlyPayment = if (result.maxRecommendedAmount > 0 && result.maxRecommendedTerm > 0 && result.recommendedInterestRate >= 0) {
            calculateEstimatedMonthlyPayment(
                result.maxRecommendedAmount,
                result.recommendedInterestRate,
                result.maxRecommendedTerm
            )
        } else {
            0.0 // Не можем рассчитать платеж
        }

        val paymentFloat = estimatedMonthlyPayment.toFloat()
        // Оставшийся доход после платежа (не может быть меньше 0)
        val remainingAfterPaymentFloat = maxOf(0f, availableIncome.toFloat() - paymentFloat)

        val entries = ArrayList<PieEntry>()
        if (paymentFloat > 0) {
            entries.add(PieEntry(paymentFloat, "Платеж по кредиту"))
        }
        if (remainingAfterPaymentFloat > 0) {
            entries.add(PieEntry(remainingAfterPaymentFloat, "Доступный доход"))
        } else if (paymentFloat <= 0) {
            // Если и платеж 0, и остаток 0, покажем весь доступный доход
            entries.add(PieEntry(availableIncome.toFloat(), "Доступный доход"))
        }


        if (entries.isEmpty()) {
            Log.w("HomeFragment", "No entries for PieChart.")
            chart.visibility = View.GONE
            return
        }

        val dataSet = PieDataSet(entries, "") // Убираем label для dataSet, т.к. есть легенда
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.chart_debt),
            ContextCompat.getColor(requireContext(), R.color.chart_income)
        )
        dataSet.sliceSpace = 2f // Небольшой отступ между секторами

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(chart)) // Формат в процентах
        data.setValueTextSize(12f) // Уменьшим размер текста на секторах
        data.setValueTextColor(Color.BLACK) // Цвет текста на секторах

        chart.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f // Размер отверстия
            transparentCircleRadius = 55f // Размер прозрачного круга вокруг отверстия

            // Настройка легенды
            legend.isEnabled = true
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textSize = 12f
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.primary_text)


            setEntryLabelColor(Color.BLACK) // Цвет названий секторов (если они видны)
            setEntryLabelTextSize(10f)
            setUsePercentValues(true) // Говорим чарту использовать проценты

            setDrawCenterText(true)
            val dtiPercentage = if (income > 0) (paymentFloat / income) * 100 else 0f
            centerText = String.format(Locale.US, "DTI\n%.1f%%", dtiPercentage) // Текст в центре
            setCenterTextSize(16f) // Размер текста в центре
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text))


            animateY(1400, Easing.EaseInOutQuad)
            invalidate() // Обновляем чарт
        }
    }

    // Улучшенная функция расчета с проверками
    private fun calculateEstimatedMonthlyPayment(
        principal: Int,
        annualInterestRate: Double,
        termInMonths: Int
    ): Double {
        // Проверка входных данных
        if (principal <= 0 || termInMonths <= 0 || annualInterestRate < 0) {
            return 0.0
        }
        // Если ставка 0%, расчет упрощается
        if (annualInterestRate == 0.0) {
            return principal.toDouble() / termInMonths
        }

        val monthlyRate = annualInterestRate / 100.0 / 12.0
        val powerTerm = Math.pow(1.0 + monthlyRate, termInMonths.toDouble())

        // Проверка деления на ноль (маловероятно при rate > 0, но все же)
        if (powerTerm - 1 == 0.0) return 0.0

        return principal * monthlyRate * powerTerm / (powerTerm - 1)
    }

    private fun getRiskColor(riskLevel: String?): Int {
        // Добавляем проверку на null или неизвестное значение
        val colorResId = when (riskLevel?.lowercase(Locale.ROOT)) { // Сравниваем в нижнем регистре
            "низкий" -> R.color.risk_low
            "средний" -> R.color.risk_medium
            "высокий" -> R.color.risk_high
            "очень высокий" -> R.color.risk_very_high
            else -> R.color.risk_medium // Цвет по умолчанию для неизвестных значений
        }
        return ContextCompat.getColor(requireContext(), colorResId)
    }

    // Используем NumberFormat для лучшего форматирования чисел с разделителями
    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale("ru", "RU")).format(number)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Важно для избежания утечек памяти
    }
}