package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.partner.Partner
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий справочника напарников (Настройки → Справочники → Напарники).
 * По образцу [StationNormRepository]/[LocomotiveSeriesRepository]: локальное
 * хранилище SQLDelight + full-replace синхронизация.
 */
interface PartnerRepository {
    fun getAllFlow(): Flow<List<Partner>>
    fun getAll(): List<Partner>
    fun getById(partnerId: String): Partner?

    /** Постоянный напарник (не больше одного в справочнике) или null. */
    fun getPermanent(): Partner?

    /**
     * Сделать напарника постоянным ([partnerId]) или снять отметку со всех (null).
     * Флаг у прежнего постоянного снимается в той же транзакции.
     */
    fun setPermanent(partnerId: String?): Flow<ResultState<Unit>>

    /** Полная замена справочника (используется при загрузке с сервера). */
    fun replaceAll(partners: List<Partner>): Flow<ResultState<Unit>>

    /**
     * Добавить/обновить одну запись справочника (редактор). Если у записи
     * [Partner.isPermanent] = true — флаг снимается с остальных записей.
     */
    fun upsert(partner: Partner): Flow<ResultState<Unit>>

    /** Удалить запись справочника по id. */
    fun delete(partnerId: String): Flow<ResultState<Unit>>
}
