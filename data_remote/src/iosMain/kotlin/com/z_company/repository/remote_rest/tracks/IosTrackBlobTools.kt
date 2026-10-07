package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.TrackBlobTools

/**
 * iOS: заглушка. Отправка и история треков — Android-фича (этап 3б); на iOS «АВТО»/треков пока нет,
 * поэтому zlib и SHA-256 не используются. Когда появится запись треков на iOS — реализовать через
 * `platform.zlib` (deflate/inflate с zlib-обёрткой) и CommonCrypto (`CC_SHA256`), формат тот же `ld-track-v1`.
 */
class IosTrackBlobTools : TrackBlobTools {
    override fun deflate(data: ByteArray, level: Int): ByteArray = unsupported()
    override fun inflate(data: ByteArray, maxOutput: Int): ByteArray = unsupported()
    override fun sha256Hex(data: ByteArray): String = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Треки на iOS ещё не поддерживаются")
}
