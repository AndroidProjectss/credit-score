package com.example.credit_score.data.api

import com.example.credit_score.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Имитация базы данных Тундук с предопределенными данными
 */
object TundukDatabase {
    
    private val clientData = HashMap<String, TundukData>()
    
    init {
        // Инициализация базы данных с предопределенными значениями
        initializeDatabase()
    }
    
    /**
     * Получает данные клиента по ИНН
     * @param inn ИНН клиента
     * @return Данные клиента или null, если клиент не найден
     */
    fun getClientDataByInn(inn: String): TundukData? {
        return clientData[inn]
    }
    
    /**
     * Инициализация базы данных с предопределенными значениями
     */
    private fun initializeDatabase() {
        // ----------------------
        // Амиров Бакир (Студент)
        // ----------------------
        val amirovInn = "20107200450055"
        clientData[amirovInn] = TundukData(
            inn = amirovInn,
            fullName = "Амиров Бакир Тимурович",
            registrationInfo = RegistrationInfo(
                region = "Чуйская область",
                city = "Бишкек",
                address = "ул. Токтогула, 125, кв. 12",
                registrationDate = "15.08.2020"
            ),
            taxInfo = TaxInfo(
                hasTaxDebt = false,
                taxDebtAmount = 0.0,
                hasInsuranceDebt = false,
                insuranceDebtAmount = 0.0
            ),
            employmentStatus = EmploymentStatus(
                isUnemployed = true,
                unemploymentDuration = null,
                registrationDate = null
            ),
            familyInfo = FamilyInfo(
                familySize = 4,
                familyMembers = listOf(
                    FamilyMember(
                        fullName = "Амиров Тимур Рахатович",
                        relationshipType = "Отец",
                        birthDate = "05.03.1975",
                        inn = "20503197500123"
                    ),
                    FamilyMember(
                        fullName = "Амирова Айсулу Маратовна",
                        relationshipType = "Мать",
                        birthDate = "12.11.1978",
                        inn = "21112197800456"
                    ),
                    FamilyMember(
                        fullName = "Амирова Алия Тимуровна",
                        relationshipType = "Сестра",
                        birthDate = "23.06.2005",
                        inn = "20623200500789"
                    )
                )
            ),
            employmentInfo = null, // студент, без официального трудоустройства
            salaryInfo = null // нет официальной зарплаты
        )
        
        // ----------------------
        // Иванов Алексей (ИП)
        // ----------------------
        val ivanovInn = "20409200500637"
        clientData[ivanovInn] = TundukData(
            inn = ivanovInn,
            fullName = "Иванов Алексей Петрович",
            registrationInfo = RegistrationInfo(
                region = "Чуйская область",
                city = "Бишкек",
                address = "ул. Киевская, 75, кв. 28",
                registrationDate = "02.04.2019"
            ),
            taxInfo = TaxInfo(
                hasTaxDebt = false,
                taxDebtAmount = 0.0,
                hasInsuranceDebt = true,
                insuranceDebtAmount = 2500.0
            ),
            employmentStatus = EmploymentStatus(
                isUnemployed = false,
                unemploymentDuration = null,
                registrationDate = null
            ),
            familyInfo = FamilyInfo(
                familySize = 3,
                familyMembers = listOf(
                    FamilyMember(
                        fullName = "Иванова Елена Сергеевна",
                        relationshipType = "Супруга",
                        birthDate = "17.05.1982",
                        inn = "21705198200321"
                    ),
                    FamilyMember(
                        fullName = "Иванов Дмитрий Алексеевич",
                        relationshipType = "Сын",
                        birthDate = "03.09.2015",
                        inn = null
                    )
                )
            ),
            employmentInfo = EmploymentInfo(
                employerName = "ИП Иванов А.П.",
                position = "Индивидуальный предприниматель",
                hireDate = "10.06.2018",
                workExperience = 70, // ~5.8 лет
                contractType = "Самозанятость"
            ),
            salaryInfo = SalaryInfo(
                averageMonthlySalary = 75000.0,
                salaryHistory = listOf(
                    SalaryRecord(
                        period = "03.2025",
                        amount = 80000.0,
                        insuranceContribution = 2400.0
                    ),
                    SalaryRecord(
                        period = "02.2025",
                        amount = 75000.0,
                        insuranceContribution = 2250.0
                    ),
                    SalaryRecord(
                        period = "01.2025",
                        amount = 70000.0,
                        insuranceContribution = 2100.0
                    )
                )
            )
        )
        
        // ----------------------
        // Кадырбеков Айдарбек (безработный)
        // ----------------------
        val kadyrbekovInn = "21912200400369"
        clientData[kadyrbekovInn] = TundukData(
            inn = kadyrbekovInn,
            fullName = "Кадырбеков Айдарбек Жанибекович",
            registrationInfo = RegistrationInfo(
                region = "Иссык-Кульская область",
                city = "Каракол",
                address = "ул. Абдрахманова, 124",
                registrationDate = "05.11.2018"
            ),
            taxInfo = TaxInfo(
                hasTaxDebt = true,
                taxDebtAmount = 15000.0,
                hasInsuranceDebt = true,
                insuranceDebtAmount = 8000.0
            ),
            employmentStatus = EmploymentStatus(
                isUnemployed = true,
                unemploymentDuration = 8, // 8 месяцев
                registrationDate = "15.08.2024"
            ),
            familyInfo = FamilyInfo(
                familySize = 5,
                familyMembers = listOf(
                    FamilyMember(
                        fullName = "Кадырбекова Айнура Талиповна",
                        relationshipType = "Супруга",
                        birthDate = "28.07.1985",
                        inn = "20728198500654"
                    ),
                    FamilyMember(
                        fullName = "Кадырбеков Нурлан Айдарбекович",
                        relationshipType = "Сын",
                        birthDate = "14.03.2010",
                        inn = null
                    ),
                    FamilyMember(
                        fullName = "Кадырбекова Жибек Айдарбековна",
                        relationshipType = "Дочь",
                        birthDate = "22.11.2012",
                        inn = null
                    ),
                    FamilyMember(
                        fullName = "Кадырбекова Айгуль Айдарбековна",
                        relationshipType = "Дочь",
                        birthDate = "30.05.2018",
                        inn = null
                    )
                )
            ),
            employmentInfo = null, // безработный
            salaryInfo = SalaryInfo(
                averageMonthlySalary = 0.0,
                salaryHistory = listOf(
                    SalaryRecord(
                        period = "08.2024",
                        amount = 32000.0,
                        insuranceContribution = 960.0
                    ),
                    SalaryRecord(
                        period = "07.2024",
                        amount = 32000.0,
                        insuranceContribution = 960.0
                    )
                )
            )
        )
        
        // ----------------------
        // Асанбеков Адис (офисный сотрудник)
        // ----------------------
        val asanbekovInn = "21904200500386"
        clientData[asanbekovInn] = TundukData(
            inn = asanbekovInn,
            fullName = "Асанбеков Адис Эрланович",
            registrationInfo = RegistrationInfo(
                region = "Чуйская область",
                city = "Бишкек",
                address = "ул. Исанова, 45, кв. 56",
                registrationDate = "12.03.2020"
            ),
            taxInfo = TaxInfo(
                hasTaxDebt = false,
                taxDebtAmount = 0.0,
                hasInsuranceDebt = false,
                insuranceDebtAmount = 0.0
            ),
            employmentStatus = EmploymentStatus(
                isUnemployed = false,
                unemploymentDuration = null,
                registrationDate = null
            ),
            familyInfo = FamilyInfo(
                familySize = 2,
                familyMembers = listOf(
                    FamilyMember(
                        fullName = "Асанбекова Динара Маратовна",
                        relationshipType = "Супруга",
                        birthDate = "03.12.1989",
                        inn = "21203198900123"
                    )
                )
            ),
            employmentInfo = EmploymentInfo(
                employerName = "ОсОО \"ТехноБизнес\"",
                position = "Менеджер по продажам",
                hireDate = "02.06.2022",
                workExperience = 34, // ~2.8 лет
                contractType = "Бессрочный"
            ),
            salaryInfo = SalaryInfo(
                averageMonthlySalary = 45000.0,
                salaryHistory = listOf(
                    SalaryRecord(
                        period = "03.2025",
                        amount = 45000.0,
                        insuranceContribution = 1350.0
                    ),
                    SalaryRecord(
                        period = "02.2025",
                        amount = 45000.0,
                        insuranceContribution = 1350.0
                    ),
                    SalaryRecord(
                        period = "01.2025",
                        amount = 45000.0,
                        insuranceContribution = 1350.0
                    ),
                    SalaryRecord(
                        period = "12.2024",
                        amount = 42000.0,
                        insuranceContribution = 1260.0
                    ),
                    SalaryRecord(
                        period = "11.2024",
                        amount = 42000.0,
                        insuranceContribution = 1260.0
                    ),
                    SalaryRecord(
                        period = "10.2024",
                        amount = 42000.0,
                        insuranceContribution = 1260.0
                    )
                )
            )
        )
    }
}
