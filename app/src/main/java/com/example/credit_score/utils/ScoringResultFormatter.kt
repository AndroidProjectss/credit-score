package com.example.credit_score.utils

import com.example.credit_score.data.model.ScoringResult

/**
 * Утилита для форматирования результатов скоринга
 */
object ScoringResultFormatter {
    
    /**
     * Форматирует результаты скоринга в читаемый текст
     */
    fun formatScoringResult(scoringResult: ScoringResult): String {
        return buildString {
            append("=== РЕЗУЛЬТАТЫ СКОРИНГА ===\n\n")
            
            append("Скоринговый балл: ${scoringResult.overallScore}/100\n")
            append("Уровень риска: ${translateRiskLevel(scoringResult.creditRiskLevel)}\n")
            append("Максимальная сумма: ${formatAmount(scoringResult.maxRecommendedAmount)} сом\n")
            append("Максимальный срок: ${scoringResult.maxRecommendedTerm} месяцев\n")
            append("Рекомендуемая ставка: ${String.format("%.2f", scoringResult.recommendedInterestRate)}%\n\n")
            
            append("Обоснование решения:\n${scoringResult.justification}\n\n")
            
            append("Сильные стороны заявителя:\n")
            scoringResult.strengths.forEachIndexed { index, strength ->
                append("${index + 1}. $strength\n")
            }
            append("\n")
            
            append("Слабые стороны заявителя:\n")
            scoringResult.weaknesses.forEachIndexed { index, weakness ->
                append("${index + 1}. $weakness\n")
            }
            append("\n")
            
            append("Рекомендации для банка:\n")
            scoringResult.recommendations.forEachIndexed { index, recommendation ->
                append("${index + 1}. $recommendation\n")
            }
        }
    }
    
    /**
     * Переводит уровень риска на русский язык
     */
    private fun translateRiskLevel(riskLevel: String): String {
        return when (riskLevel.lowercase()) {
            "low" -> "Низкий"
            "medium" -> "Средний"
            "high" -> "Высокий"
            "very high" -> "Очень высокий"
            else -> riskLevel
        }
    }
    
    /**
     * Форматирует сумму для лучшей читаемости
     */
    private fun formatAmount(amount: Int): String {
        return amount.toString()
            .reversed()
            .chunked(3)
            .joinToString(" ")
            .reversed()
    }
}
