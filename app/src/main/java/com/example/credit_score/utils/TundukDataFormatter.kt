package com.example.credit_score.utils

import com.example.credit_score.data.model.TundukData

/**
 * Утилитный класс для форматирования данных Тундук в удобочитаемый вид
 */
object TundukDataFormatter {
    
    /**
     * Форматирует данные Тундук в удобочитаемый текст
     * @param data Данные Тундук
     * @return Отформатированный текст
     */
    fun formatTundukDataToText(data: TundukData): String {
        val builder = StringBuilder()
        
        // Основная информация
        builder.append("ИНН: ${data.inn}\n")
        builder.append("ФИО: ${data.fullName}\n\n")
        
        // Информация о регистрации
        builder.append("=== СПРАВКА О МЕСТЕ РЕГИСТРАЦИИ ===\n")
        builder.append("Регион: ${data.registrationInfo.region}\n")
        builder.append("Город: ${data.registrationInfo.city}\n")
        builder.append("Адрес: ${data.registrationInfo.address}\n")
        builder.append("Дата регистрации: ${data.registrationInfo.registrationDate}\n\n")
        
        // Информация о налогах
        builder.append("=== СПРАВКА О НАЛОГАХ И СТРАХОВЫХ ВЗНОСАХ ===\n")
        if (data.taxInfo.hasTaxDebt) {
            builder.append("Имеется задолженность по налогам: ${data.taxInfo.taxDebtAmount} сом\n")
        } else {
            builder.append("Задолженность по налогам отсутствует\n")
        }
        
        if (data.taxInfo.hasInsuranceDebt) {
            builder.append("Имеется задолженность по страховым взносам: ${data.taxInfo.insuranceDebtAmount} сом\n\n")
        } else {
            builder.append("Задолженность по страховым взносам отсутствует\n\n")
        }
        
        // Статус занятости
        builder.append("=== СТАТУС ЗАНЯТОСТИ ===\n")
        if (data.employmentStatus.isUnemployed) {
            builder.append("Статус: Безработный\n")
            if (data.employmentStatus.unemploymentDuration != null) {
                builder.append("Продолжительность безработицы: ${data.employmentStatus.unemploymentDuration} месяцев\n")
            }
            if (data.employmentStatus.registrationDate != null) {
                builder.append("Дата регистрации в качестве безработного: ${data.employmentStatus.registrationDate}\n\n")
            } else {
                builder.append("Официально не зарегистрирован как безработный\n\n")
            }
        } else {
            builder.append("Статус: Трудоустроен\n\n")
        }
        
        // Информация о семье
        builder.append("=== ИНФОРМАЦИЯ О СОСТАВЕ СЕМЬИ ===\n")
        builder.append("Количество членов семьи: ${data.familyInfo.familySize}\n")
        if (data.familyInfo.familyMembers.isNotEmpty()) {
            builder.append("Члены семьи:\n")
            data.familyInfo.familyMembers.forEach { member ->
                builder.append("- ${member.fullName}, ${member.relationshipType}, ${member.birthDate}")
                if (member.inn != null) {
                    builder.append(", ИНН: ${member.inn}")
                }
                builder.append("\n")
            }
            builder.append("\n")
        }
        
        // Информация о трудоустройстве
        if (data.employmentInfo != null) {
            builder.append("=== СПРАВКА С МЕСТА РАБОТЫ ===\n")
            builder.append("Работодатель: ${data.employmentInfo.employerName}\n")
            builder.append("Должность: ${data.employmentInfo.position}\n")
            builder.append("Дата трудоустройства: ${data.employmentInfo.hireDate}\n")
            builder.append("Стаж работы: ${data.employmentInfo.workExperience} месяцев\n")
            builder.append("Тип контракта: ${data.employmentInfo.contractType}\n\n")
        }
        
        // Информация о зарплате
        if (data.salaryInfo != null) {
            builder.append("=== ИНФОРМАЦИЯ О ЗАРАБОТНОЙ ПЛАТЕ ===\n")
            builder.append("Средняя ежемесячная зарплата: ${data.salaryInfo.averageMonthlySalary} сом\n")
            if (data.salaryInfo.salaryHistory.isNotEmpty()) {
                builder.append("История зарплаты:\n")
                data.salaryInfo.salaryHistory.forEach { record ->
                    builder.append("- ${record.period}: ${record.amount} сом (страховые взносы: ${record.insuranceContribution} сом)\n")
                }
            }
        }
        
        return builder.toString()
    }
}
