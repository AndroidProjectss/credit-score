package com.example.credit_score.data.model

/**
 * Модель для представления результатов скоринга
 */
data class ScoringResult(
    val overallScore: Int, // 0-100 score
    val creditRiskLevel: String, // "Low", "Medium", "High", "Very High"
    val maxRecommendedAmount: Int, // Maximum recommended amount in KGS
    val maxRecommendedTerm: Int, // Maximum recommended term in months
    val recommendedInterestRate: Double, // Recommended interest rate
    val justification: String, // Explanation for the score
    val strengths: List<String>, // Applicant's strengths
    val weaknesses: List<String>, // Applicant's weaknesses
    val recommendations: List<String> // Recommendations for the bank
)
