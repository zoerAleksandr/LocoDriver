package com.z_company.route.viewmodel

private fun RouteVia.key(): String = osmId?.toString() ?: name.trim().lowercase()

/**
 * Подпись «через …» для каждого варианта: первая станция по пути, которой нет ни в одном другом варианте
 * (иначе все чипы были бы «через Волховстрой»); у вариантов без отличий — первая станция пути; нет станций — null.
 */
fun variantViaLabels(variants: List<RouteVariant>): List<String?> = variants.mapIndexed { i, v ->
    val others = variants.filterIndexed { j, _ -> j != i }.flatMap { it.via }.map(RouteVia::key).toSet()
    (v.via.firstOrNull { it.key() !in others } ?: v.via.firstOrNull())?.name
}

/** Станции варианта, которые можно добавить в плечо (есть координаты), в порядке по пути. */
fun RouteVariant.addableStations(): List<RouteVia> =
    via.filter { it.latitude != null && it.longitude != null }.sortedBy { it.km }
