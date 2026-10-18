package com.z_company.data_local.brake

import com.zcompany.datalocal.brake.db.BrakeCertificate
import com.z_company.data_local.brake.db.BrakeCertificateDatabase
import com.z_company.domain.brake.storage.BrakeCertificateRepository
import com.z_company.domain.brake.storage.StoredBrakeCertificate

class SqlDelightBrakeCertificateRepository(private val db: BrakeCertificateDatabase) : BrakeCertificateRepository {
    private val q get() = db.brakeCertificateQueries

    private fun BrakeCertificate.toDomain() = StoredBrakeCertificate(
        certificateId = certificateId, routeId = routeId, trainId = trainId,
        payload = payload, updatedAt = updatedAt, isDeleted = isDeleted != 0L,
    )

    override fun getByTrain(trainId: String) = q.getByTrain(trainId).executeAsOneOrNull()?.toDomain()

    override fun listByRoute(routeId: String, includeDeleted: Boolean) =
        q.listByRoute(routeId).executeAsList().map { it.toDomain() }.filter { includeDeleted || !it.isDeleted }

    override fun upsert(cert: StoredBrakeCertificate) =
        q.upsert(cert.certificateId, cert.routeId, cert.trainId, cert.payload, cert.updatedAt, if (cert.isDeleted) 1L else 0L)

    override fun getById(certificateId: String) = q.getById(certificateId).executeAsOneOrNull()?.toDomain()
    override fun listAllActive() = q.listAllActive().executeAsList().map { it.toDomain() }
    override fun markDeletedById(certificateId: String, now: Long) = q.markDeletedById(now, certificateId)

    override fun markDeletedByTrain(trainId: String, now: Long) = q.markDeletedByTrain(now, trainId)
    override fun deleteByRoute(routeId: String) = q.deleteByRoute(routeId)
    override fun purgeTombstones(routeId: String) = q.purgeTombstones(routeId)
}
