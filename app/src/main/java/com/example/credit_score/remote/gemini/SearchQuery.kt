package com.example.credit_score.remote.gemini

data class SearchQuery(
    val overallScore: Int, // Общий скоринговый балл (0-100)
    val creditRiskLevel: String, // Уровень кредитного риска (Низкий, Средний, Высокий, Очень высокий)
    val maxRecommendedAmount: Int, // Максимальная рекомендуемая сумма в сомах
    val maxRecommendedTerm: Int, // Максимальный рекомендуемый срок в месяцах
    val recommendedInterestRate: Double, // Рекомендуемая процентная ставка
    val justification: String, // Обоснование решения
    val strengths: List<String>, // Сильные стороны заявителя
    val weaknesses: List<String>, // Слабые стороны заявителя
    val recommendations: List<String>, // Рекомендации для банка
    val monthlyIncome: Int?, // Ежемесячный доход
    val monthlyExpenses: Int? // Ежемесячные расходы
)