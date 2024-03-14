package com.z_company.domain.entities.route

object UtilsForEntities {
    fun Route.getWorkTime(): Long? {
        val timeEnd = this.basicData.timeEndWork
        val timeStart = this.basicData.timeStartWork
        return if (timeEnd != null && timeStart != null) {
            timeEnd - timeStart
        } else {
            null
        }
    }
}