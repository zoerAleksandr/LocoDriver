package com.z_company.domain.util

import com.z_company.domain.entities.norma_time.StationNorm
import com.z_company.domain.entities.norma_time.StationTombstone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StationNormMergeTest {
    private fun st(id: String, ts: Long, name: String = id, norm: Int? = null) =
        StationNorm(stationId = id, name = name, appearanceToStartMin = norm, updatedAt = ts)

    private fun tomb(id: String, ts: Long) = StationTombstone(id, id, ts)

    @Test
    fun unionOfStationsFromBothSides() {
        val r = StationNormMerge.merge(listOf(st("a", 1)), emptyList(), listOf(st("b", 1)), emptyList())
        assertEquals(setOf("a", "b"), r.stations.map { it.stationId }.toSet())
        assertTrue(r.tombstones.isEmpty())
    }

    @Test
    fun newerEditWins() {
        val r = StationNormMerge.merge(listOf(st("a", 1, norm = 10)), emptyList(), listOf(st("a", 5, norm = 20)), emptyList())
        assertEquals(20, r.stations.single().appearanceToStartMin)
    }

    @Test
    fun remoteTombstoneRemovesOlderLocalStation() {
        val r = StationNormMerge.merge(listOf(st("a", 1)), emptyList(), emptyList(), listOf(tomb("a", 5)))
        assertTrue(r.stations.isEmpty())
        assertEquals(listOf("a"), r.tombstones.map { it.stationId })
    }

    @Test
    fun localTombstoneBlocksOlderRemoteStation() {
        val r = StationNormMerge.merge(emptyList(), listOf(tomb("a", 5)), listOf(st("a", 3)), emptyList())
        assertTrue(r.stations.isEmpty())
        assertEquals(1, r.tombstones.size)
    }

    @Test
    fun editAfterDeletionRevivesStation() {
        val r = StationNormMerge.merge(listOf(st("a", 9)), emptyList(), emptyList(), listOf(tomb("a", 5)))
        assertEquals(listOf("a"), r.stations.map { it.stationId })
        assertTrue(r.tombstones.isEmpty())
    }

    @Test
    fun tieGoesToDeletion() {
        val r = StationNormMerge.merge(listOf(st("a", 5)), emptyList(), emptyList(), listOf(tomb("a", 5)))
        assertTrue(r.stations.isEmpty())
    }
}
