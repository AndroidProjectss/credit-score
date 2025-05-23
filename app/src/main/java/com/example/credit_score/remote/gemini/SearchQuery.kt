package com.example.credit_score.remote.gemini

data class SearchQuery(
    val overallScore: Int, // 0-100 score
    val creditRiskLevel: String, // "Low", "Medium", "High", "Very High"
    val maxRecommendedAmount: Int, // Maximum recommended amount in KGS
    val maxRecommendedTerm: Int, // Maximum recommended term in months
    val recommendedInterestRate: Double, // Recommended interest rate
    val justification: String, // Explanation for the score
    val strengths: List<String>, // Applicant's strengths
    val weaknesses: List<String>, // Applicant's weaknesses
    val recommendations: List<String>, // Recommendations for the bank
    val monthlyIncome: Int?,      // Ежемесячный доход (Int? означает, что может быть null)
    val monthlyExpenses: Int?
)