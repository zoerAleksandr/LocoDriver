package com.z_company.domain.entities.salary

/** Вид строки расчётного листа. Справочник локальный и не участвует в JSON/БД. */
enum class PayrollPaymentType { ACCRUAL, DEDUCTION }

/** Режим названия предусмотрен заранее; переключателя в настройках пока нет. */
enum class PayrollNameMode { PLAIN, PAYROLL_SHEET }

enum class SalaryPaymentId {
    TARIFF,
    NIGHT,
    PASSENGER,
    RESERVE,
    HOLIDAY,
    AVERAGE,
    UNDERWORK,
    DISABLED_CHILD_CARE,
    BUSINESS_TRIP,
    TECHNICAL_STUDY,
    ZONAL,
    QUALIFICATION_CLASS,
    LINEAR_MILEAGE,
    ONE_PERSON_FREIGHT,
    ONE_PERSON_PASSENGER,
    HARMFULNESS,
    DISTRICT,
    NORDIC,
    OTHER_SURCHARGE,
    EXCESS_REST,
    EXTENDED_SERVICE,
    HEAVY_TRAIN,
    LONG_TRAIN,
    HEAVY_LONG_DISTANCE,
    DOUBLED_TRAIN,
    OVERTIME_BASE,
    OVERTIME_HALF,
    OVERTIME_FULL,
    NDFL,
    UNION,
    OTHER_DEDUCTION,
    WELFARE,
    ALIMONY,
}

data class PayrollPaymentDefinition(
    val id: SalaryPaymentId,
    val codes: List<String>,
    val payrollSheetName: String,
    val plainName: String,
    val type: PayrollPaymentType,
) {
    val codeLabel: String get() = codes.joinToString("/").ifBlank { "—" }

    fun displayName(mode: PayrollNameMode): String = when (mode) {
        PayrollNameMode.PLAIN -> plainName
        PayrollNameMode.PAYROLL_SHEET -> payrollSheetName
    }
}

/**
 * Коды переписаны из предоставленной памятки и примеров расчётных листков.
 * Пустой список означает, что памятка не даёт однозначного кода: код может
 * зависеть от основания выплаты или настроек конкретного подразделения.
 */
object PayrollPaymentCatalog {
    val entries: List<PayrollPaymentDefinition> = listOf(
        accrual(SalaryPaymentId.TARIFF, "004L", "ПоврОплатаПоТарифСтавкам", "Оплата по тарифу"),
        accrual(SalaryPaymentId.NIGHT, "023L", "ДоплатаЗаРаботуНочноеВремя", "Ночные часы"),
        accrual(SalaryPaymentId.PASSENGER, "018L", "ОплЗаСледПассПредНорРабВ", "Следование пассажиром"),
        accrual(SalaryPaymentId.RESERVE, "052L", "ОплЗаРабНаОдиночСледЛоко", "Следование резервом"),
        accrual(SalaryPaymentId.HOLIDAY, listOf("035L", "076L"), "ОплВыхДнейСверхНорм / ДоплВыхПраздСверхНорм", "Праздничные"),
        accrual(SalaryPaymentId.AVERAGE, emptyList(), "Оплата по среднему заработку", "Оплата по среднему"),
        accrual(SalaryPaymentId.UNDERWORK, emptyList(), "Оплата недоработки", "Оплата недоработки"),
        accrual(SalaryPaymentId.DISABLED_CHILD_CARE, emptyList(), "Оплата дополнительных выходных по уходу за ребёнком-инвалидом", "Уход за ребёнком-инвалидом"),
        accrual(SalaryPaymentId.BUSINESS_TRIP, emptyList(), "Оплата командировки по среднему заработку", "Командировка"),
        accrual(SalaryPaymentId.TECHNICAL_STUDY, "049A", "ОплПоТарВрТехУчебВНерабВр", "Технические занятия"),
        accrual(SalaryPaymentId.ZONAL, "150A", "ЗоналНадб%ОтОтрабВремФак", "Зональная надбавка"),
        accrual(SalaryPaymentId.QUALIFICATION_CLASS, "025L", "НадбавкаЗаКлассКвалификации", "Надбавка за класс квалификации"),
        accrual(SalaryPaymentId.LINEAR_MILEAGE, emptyList(), "Доплата за линейный пробег", "Доплата за пробег"),
        accrual(SalaryPaymentId.ONE_PERSON_FREIGHT, "153L", "ДоплМашЛокомРабБезПомощн", "В одно лицо (грузовые)"),
        accrual(SalaryPaymentId.ONE_PERSON_PASSENGER, "153L", "ДоплМашЛокомРабБезПомощн", "В одно лицо (пассажирские)"),
        accrual(SalaryPaymentId.HARMFULNESS, "057L", "ДоплМастСпСлРабВредПылЛБ", "Вредность"),
        accrual(SalaryPaymentId.DISTRICT, emptyList(), "Районный коэффициент", "Районный коэффициент"),
        accrual(SalaryPaymentId.NORDIC, emptyList(), "Северная надбавка", "Северная надбавка"),
        accrual(SalaryPaymentId.OTHER_SURCHARGE, emptyList(), "Прочие доплаты и надбавки", "Прочие надбавки"),
        accrual(SalaryPaymentId.EXCESS_REST, emptyList(), "Оплата переотдыха", "Переотдых"),
        accrual(SalaryPaymentId.EXTENDED_SERVICE, listOf("151L", "151P"), "ДоплРабЛокомБрУдлинУчОбсл", "Удлинённое плечо"),
        accrual(SalaryPaymentId.HEAVY_TRAIN, "152P", "ДоплРабЛокомБрТяжДлинПоез", "Тяжёлые поезда"),
        accrual(SalaryPaymentId.LONG_TRAIN, "152P", "ДоплРабЛокомБрТяжДлинПоез", "Длинносоставные поезда"),
        accrual(SalaryPaymentId.HEAVY_LONG_DISTANCE, emptyList(), "Доплата за ПДМ", "Доплата за ПДМ"),
        accrual(SalaryPaymentId.DOUBLED_TRAIN, emptyList(), "Доплата за сдвоенные поезда", "Сдвоенные поезда"),
        accrual(SalaryPaymentId.OVERTIME_BASE, "072L", "ОплРабСверхУрочВр_ЛБ_004L", "Сверхурочные часы"),
        accrual(SalaryPaymentId.OVERTIME_HALF, "073L", "ДоплСверхУрочнВр0,5разме", "Доплата за сверхурочные (50%)"),
        accrual(SalaryPaymentId.OVERTIME_FULL, emptyList(), "Доплата за последующие часы сверхурочной работы", "Доплата за сверхурочные (100%)"),
        deduction(SalaryPaymentId.NDFL, "883A", "НалогНаДохФизЛицаУдер13", "НДФЛ"),
        deduction(SalaryPaymentId.UNION, "902A", "ПрофсоюзПервыйКредитор", "Профсоюз"),
        PayrollPaymentDefinition(
            SalaryPaymentId.OTHER_DEDUCTION,
            emptyList(),
            "Прочие удержания",
            "Прочие удержания",
            PayrollPaymentType.DEDUCTION,
        ),
        deduction(SalaryPaymentId.WELFARE, "932A", "УдержНегосПФ«Благосост.»", "Благосостояние"),
        deduction(SalaryPaymentId.ALIMONY, "889A", "УдержАлиментовНаДетей", "Алименты"),
    )

    private val byId = entries.associateBy(PayrollPaymentDefinition::id)

    operator fun get(id: SalaryPaymentId): PayrollPaymentDefinition =
        requireNotNull(byId[id]) { "Payroll payment is not registered: $id" }

    private fun accrual(id: SalaryPaymentId, code: String, payrollName: String, plainName: String) =
        accrual(id, listOf(code), payrollName, plainName)

    private fun accrual(id: SalaryPaymentId, codes: List<String>, payrollName: String, plainName: String) =
        PayrollPaymentDefinition(id, codes, payrollName, plainName, PayrollPaymentType.ACCRUAL)

    private fun deduction(id: SalaryPaymentId, code: String, payrollName: String, plainName: String) =
        PayrollPaymentDefinition(id, listOf(code), payrollName, plainName, PayrollPaymentType.DEDUCTION)
}
