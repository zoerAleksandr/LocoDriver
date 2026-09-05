package com.z_company.domain.entities.route

/** Причина физического удаления, обязательная для будущего purge-сервиса. */
enum class PhysicalDeletionReason {
    TRASH_RETENTION_EXPIRED,
    USER_EMPTIED_TRASH,
    SHARED_PREVIEW_DISCARDED,
    DIAGNOSTIC_ROLLBACK_CLEANUP,
    TEST_FIXTURE_CLEANUP
}
