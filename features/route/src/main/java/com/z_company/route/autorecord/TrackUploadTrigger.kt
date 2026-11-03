package com.z_company.route.autorecord

/** Запросить фоновую отправку треков (WorkManager-воркер живёт в app). Идемпотентно и дёшево. */
fun interface TrackUploadTrigger {
    fun trigger()
}
