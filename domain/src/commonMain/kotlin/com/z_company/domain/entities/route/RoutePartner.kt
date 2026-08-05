package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

/**
 * «Напарник» в конкретном маршруте — человек, с которым выполнялась поездка.
 * Один из вложенных подразделов маршрута наряду с локомотивом, поездом,
 * следованием пассажиром и прочей работой. В маршруте можно указать несколько
 * напарников (список [Route.partners]).
 *
 * Данные хранятся КОПИЕЙ ([fullName], [tabNumber], [notes]) — маршрут
 * самодостаточен и остаётся корректным даже если запись справочника изменили
 * или удалили (важно для расшаривания маршрутов). [sourcePartnerId] —
 * необязательная ссылка на запись справочника [com.z_company.domain.entities.partner.Partner],
 * позволяющая из карточки перейти к редактированию в справочнике, если запись
 * ещё существует.
 *
 * Раздел информационный: в расчёте рабочего времени и зарплаты не участвует.
 *
 * Все поля с дефолтами — контракт full-replace + ignoreUnknownKeys,
 * обратная совместимость со старыми клиентами/сервером.
 */
@Serializable
data class RoutePartner(
    var routePartnerId: String = generateId(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    /** Ссылка на запись справочника, из которой был выбран напарник (может быть null). */
    var sourcePartnerId: String? = null,
    var fullName: String? = null,
    var tabNumber: String? = null,
    var notes: String? = null,
)
