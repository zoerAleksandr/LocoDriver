package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Route

/**
 * Keeps the order already visible on this device when an older server response
 * contains the same sections in a different order. New sections retain the
 * order received from the server.
 */
internal fun Route.preserveSectionOrderFrom(local: Route?): Route {
    if (local == null) return this

    val localLocomotives = local.locomotives.associateBy { it.locoId }
    return copy(
        locomotives = locomotives.map { remoteLoco ->
            remoteLoco.preserveSectionOrderFrom(localLocomotives[remoteLoco.locoId])
        }.toMutableList()
    )
}

private fun Locomotive.preserveSectionOrderFrom(local: Locomotive?): Locomotive {
    if (local == null) return this

    return copy(
        electricSectionList = electricSectionList.preserveOrder(
            localIds = local.electricSectionList.map { it.sectionId },
            idOf = { it.sectionId },
        ).toMutableList(),
        dieselSectionList = dieselSectionList.preserveOrder(
            localIds = local.dieselSectionList.map { it.sectionId },
            idOf = { it.sectionId },
        ).toMutableList(),
    )
}

private fun <T> List<T>.preserveOrder(localIds: List<String>, idOf: (T) -> String): List<T> {
    val remoteById = associateBy(idOf)
    val knownIds = localIds.toHashSet()
    return buildList(size) {
        localIds.forEach { id -> remoteById[id]?.let(::add) }
        this@preserveOrder.forEach { item ->
            if (idOf(item) !in knownIds) add(item)
        }
    }
}
