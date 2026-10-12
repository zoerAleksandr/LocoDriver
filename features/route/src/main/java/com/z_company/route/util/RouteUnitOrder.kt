package com.z_company.route.util

import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train

/**
 * Порядок локомотивов и поездов маршрута — тот же, что в форме маршрута и быстром просмотре:
 * по возрастанию времени (приёмка локомотива / отправление первой станции поезда),
 * без времени — в конце, в порядке добавления. Порядок из БД/сети не гарантирован,
 * поэтому «последний» (плитка на главной) определяем по этому порядку, а не по позиции в списке.
 */
fun Route.locomotivesInOrder(): List<Locomotive> =
    locomotives.filter { it.timeStartOfAcceptance != null }.sortedBy { it.timeStartOfAcceptance } +
        locomotives.filter { it.timeStartOfAcceptance == null }

fun Route.trainsInOrder(): List<Train> =
    trains.filter { it.stations.firstOrNull()?.timeDeparture != null }
        .sortedBy { it.stations.firstOrNull()?.timeDeparture } +
        trains.filter { it.stations.firstOrNull()?.timeDeparture == null }
