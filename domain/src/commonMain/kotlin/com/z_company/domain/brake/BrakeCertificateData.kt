package com.z_company.domain.brake

import kotlinx.serialization.Serializable

/**
 * Одно состояние справки ВУ-45 - всё, что вводит и получает пользователь. Единственный носитель данных экрана:
 * сейчас живёт во ViewModel (в БД и на сервер не пишется), позже сериализуется целиком (JSON/SQLDelight).
 * Только «чистые» значения: строки, числа, перечисления по имени; без идущих секундомеров и UI-состояния.
 * Проект хранения - docs/VU45_STORAGE_DESIGN.md.
 */
@Serializable
data class BrakeCertificateData(
    /** UUID справки (клиент генерирует сам). */
    val id: String = "",
    /** Рейс (BasicData.id), к которому относится справка; null - справка без привязки. */
    val routeId: String? = null,
    val trainId: String? = null,
    // Параметры расчёта
    val categoryId: String = "FREIGHT_LOADED",
    /** Категория выбрана вручную (автоподбор по номеру и нагрузке на ось её не меняет). null - справка сохранена до появления поля: считается ручной. */
    val categoryManual: Boolean? = null,
    val grade: GradeClass = GradeClass.UP_TO_006,
    val autoblock3: Boolean = false,
    /** Ручное переопределение нормы ручных осей; null - «Авто» по уклону. */
    val manualNorm: BrakeNorms.ManualBrakeNorm? = null,
    val slope: SlopeStep = SlopeStep.S006,
    // Шапка бланка
    val station: String = "",
    val issueTime: String = "",
    val date: String = "",
    val locoSeries: String = "",
    val locoNumber: String = "",
    val trainNumber: String = "",
    val weight: String = "",
    val totalAxles: String = "",
    // Нажатие
    val avgPressure: String = "",
    val rowsMode: Boolean = false,
    val rows: List<AxleRowData> = emptyList(),
    // Локомотив в подсчёте
    val locoEnabled: Boolean = false,
    val locoWeight: String = "",
    val locoAxles: String = "",
    val locoPressure: String = "",
    // Другие данные
    val compositeShare: CompositeShare = CompositeShare.NONE,
    val flags: List<String> = emptyList(),
    val tcpv: String = "",
    val vstr: String = "",
    val dpv: String = "",
    val vo2hv: String = "",
    // Нижняя часть бланка
    val manualHave: String = "",
    val feedDensity: String = "",
    val tailVoltage: String = "",
    val tailCar: String = "",
    /** Фамилия осмотрщика (поле «Подпись» бланка). Необязательное: старые справки читаются без него. */
    val inspectorName: String = "",
    // Плотность
    val chargePressure: String = "",
    val densityGroup: DensityLocoGroup? = null,
    /** Свой объём главных резервуаров, л (текст, дробные допустимы); пусто - не задан. Необязательное поле (старые сохранённые справки читаются без него). */
    val customVolume: String = "",
    val locoCount: String = "1",
    val measurements: List<DensityMeasurementData> = emptyList(),
    // Оборот
    val turnaround: List<TurnaroundEntry> = emptyList(),
    /** Время последнего изменения, мс (LWW при синхронизации). */
    val updatedAt: Long = 0L,
)

@Serializable
data class AxleRowData(val pressure: String = "", val axles: String = "")

/** Результат замера плотности: вид (II / IV / ROUTE), время (с) и введённые давления. */
@Serializable
data class DensityMeasurementData(
    val kind: String,
    val seconds: Double? = null,
    /** Давление в начале и в конце замера, кгс/см2 (текстом; ввод в шторке плотности бланка). Необязательные поля, у старых справок пусто. */
    val grStart: String = "",
    val grEnd: String = "",
    val drop: String = "",
    /** Значение введено вручную (не замером секундомером); у старых записей false. */
    val manual: Boolean = false,
)
