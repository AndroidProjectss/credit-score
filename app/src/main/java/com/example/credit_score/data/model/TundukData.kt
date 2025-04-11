package com.example.credit_score.data.model

/**
 * Модель данных для хранения информации, получаемой от Тундук API
 */
data class TundukData(
    val inn: String,
    val fullName: String,
    val registrationInfo: RegistrationInfo,
    val taxInfo: TaxInfo,
    val employmentStatus: EmploymentStatus,
    val familyInfo: FamilyInfo,
    val employmentInfo: EmploymentInfo?,
    val salaryInfo: SalaryInfo?
)

/**
 * Информация о месте регистрации граждан
 */
data class RegistrationInfo(
    val region: String,
    val city: String,
    val address: String,
    val registrationDate: String
)

/**
 * Справка об отсутствии/наличии задолженности по налогам и страховым взносам (STI-20)
 */
data class TaxInfo(
    val hasTaxDebt: Boolean,
    val taxDebtAmount: Double, // сумма задолженности в сомах
    val hasInsuranceDebt: Boolean,
    val insuranceDebtAmount: Double // сумма задолженности по страховым взносам в сомах
)

/**
 * Справка об официальном статусе безработного
 */
data class EmploymentStatus(
    val isUnemployed: Boolean,
    val unemploymentDuration: Int?, // продолжительность безработицы в месяцах
    val registrationDate: String? // дата регистрации в качестве безработного
)

/**
 * Информация о составе семьи
 */
data class FamilyInfo(
    val familySize: Int, // количество членов семьи
    val familyMembers: List<FamilyMember>
)

/**
 * Информация о члене семьи
 */
data class FamilyMember(
    val fullName: String,
    val relationshipType: String, // тип родственной связи
    val birthDate: String,
    val inn: String? // ИНН члена семьи (может отсутствовать)
)

/**
 * Справка с места работы («е-Kyzmat»)
 */
data class EmploymentInfo(
    val employerName: String,
    val position: String,
    val hireDate: String,
    val workExperience: Int, // стаж работы в месяцах
    val contractType: String // тип контракта (постоянный, временный и т.д.)
)

/**
 * Информация о начислениях страховых взносов застрахованного лица (информация о заработной плате)
 */
data class SalaryInfo(
    val averageMonthlySalary: Double, // средняя ежемесячная зарплата в сомах
    val salaryHistory: List<SalaryRecord> // история зарплаты
)

/**
 * Запись о зарплате за определенный период
 */
data class SalaryRecord(
    val period: String, // период (обычно месяц-год)
    val amount: Double, // сумма в сомах
    val insuranceContribution: Double // страховой взнос в сомах
)
