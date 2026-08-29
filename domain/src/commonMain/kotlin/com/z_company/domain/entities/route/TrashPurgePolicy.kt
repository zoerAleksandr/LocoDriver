package com.z_company.domain.entities.route

/** Единое правило, запрещающее потерять серверный tombstone при очистке корзины. */
object TrashPurgePolicy {
    fun canPurgeManually(route: Route): Boolean = with(route.basicData) {
        isDeleted &&
            !remoteDeletionPending &&
            (remoteRouteId.isNullOrBlank() || remoteDeletedAt != null)
    }
}
