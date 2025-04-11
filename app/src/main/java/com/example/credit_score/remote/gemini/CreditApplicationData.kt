package com.example.credit_score.remote.gemini

/**
 * Класс данных для представления кредитной заявки
 */
data class CreditApplicationData(
    val fullName: String,
    val passportData: String,
    val registrationAddress: String,
    val workExperience: Int,
    val monthlyIncome: Int,
    val monthlyExpenses: Int,
    val hasTaxDebt: Boolean,
    val taxDebtAmount: Int? = null,
    val employmentStatus: String,
    val familyStatus: String,
    val requestedAmount: Int,
    val requestedTerm: Int,
    val purpose: String,
    val creditHistory: String
)
