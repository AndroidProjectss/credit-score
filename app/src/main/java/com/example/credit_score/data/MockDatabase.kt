package com.example.credit_score.data

object MockDatabase {
    // Фейковая база данных пользователей
    val users = mapOf(
        "20409200500637" to UserData(
            id = "20409200500637",
            fullName = "Иванов Алексей Петрович",
            passportData = "AN 1234567",
            registrationAddress = "г. Бишкек, ул. Киевская, д. 95, кв. 12",
            employmentStatus = "Студент, неполная занятость",
            workExperience = 1,
            monthlyIncome = 15000,
            monthlyExpenses = 10000,
            hasTaxDebt = false,
            taxDebtAmount = 0,
            familyStatus = "Холост, детей нет",
            creditHistory = "Кредитов не брал"
        ),
        "20107200450055" to UserData(
            id = "20107200450055",
            fullName = "Бакиров Амир Рустамович",
            passportData = "AN 7654321",
            registrationAddress = "г. Бишкек, ул. Тоголок Молдо, д. 54, кв. 31",
            employmentStatus = "Программист, полная занятость",
            workExperience = 5,
            monthlyIncome = 85000,
            monthlyExpenses = 30000,
            hasTaxDebt = false,
            taxDebtAmount = 0,
            familyStatus = "Женат, 1 ребенок",
            creditHistory = "Положительная, 2 закрытых кредита без просрочек"
        ),
        "21904200500386" to UserData(
            id = "21904200500386",
            fullName = "Асанбеков Адис Талантбекович",
            passportData = "AN 3456789",
            registrationAddress = "г. Бишкек, ул. Ахунбаева, д. 110, кв. 45",
            employmentStatus = "Офисный сотрудник, полная занятость",
            workExperience = 3,
            monthlyIncome = 35000,
            monthlyExpenses = 20000,
            hasTaxDebt = true,
            taxDebtAmount = 5000,
            familyStatus = "Женат, детей нет",
            creditHistory = "Удовлетворительная, 1 закрытый кредит с несколькими просрочками"
        ),
        "21912200400369" to UserData(
            id = "21912200400369",
            fullName = "Кадырбеков Айдарбек Нурланович",
            passportData = "AN 9876543",
            registrationAddress = "г. Бишкек, ул. Чуй, д. 150, кв. 78",
            employmentStatus = "Безработный, студент",
            workExperience = 0,
            monthlyIncome = 8000,
            monthlyExpenses = 7000,
            hasTaxDebt = false,
            taxDebtAmount = 0,
            familyStatus = "Холост, детей нет",
            creditHistory = "Отсутствует"
        )
    )

    // Получение данных пользователя по ИНН
    fun getUserByInn(inn: String): UserData? {
        return users[inn]
    }
}

// Класс для хранения данных пользователя
data class UserData(
    val id: String,
    val fullName: String,
    val passportData: String,
    val registrationAddress: String,
    val employmentStatus: String,
    val workExperience: Int,
    val monthlyIncome: Int,
    val monthlyExpenses: Int,
    val hasTaxDebt: Boolean,
    val taxDebtAmount: Int,
    val familyStatus: String,
    val creditHistory: String
)
