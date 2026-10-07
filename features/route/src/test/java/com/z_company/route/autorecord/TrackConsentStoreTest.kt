package com.z_company.route.autorecord

import android.content.SharedPreferences
import com.z_company.domain.track.TrackInfo
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackConsentStoreTest {

    private class MemPrefs : SharedPreferences {
        val map = mutableMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?) = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String?, defValue: Int) = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long) = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float) = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean) = map[key] as? Boolean ?: defValue
        override fun contains(key: String?) = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            val pending = mutableMapOf<String, Any?>()
            val removed = mutableSetOf<String>()
            override fun putString(k: String, v: String?) = apply { pending[k] = v }
            override fun putStringSet(k: String, v: MutableSet<String>?) = apply { pending[k] = v }
            override fun putInt(k: String, v: Int) = apply { pending[k] = v }
            override fun putLong(k: String, v: Long) = apply { pending[k] = v }
            override fun putFloat(k: String, v: Float) = apply { pending[k] = v }
            override fun putBoolean(k: String, v: Boolean) = apply { pending[k] = v }
            override fun remove(k: String) = apply { removed += k }
            override fun clear() = apply { map.clear() }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                removed.forEach { map.remove(it) }
                map.putAll(pending)
            }
        }
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private class Repo : TrackRepository {
        var deletedAll = 0
        override fun append(trackId: String, trainId: String, point: TrackPoint) {}
        override fun points(trackId: String) = emptyList<TrackPoint>()
        override fun pointCount(trackId: String) = 0L
        override fun tracks() = emptyList<TrackInfo>()
        override fun tracksForTrain(trainId: String) = emptyList<TrackInfo>()
        override fun setFavorite(trackId: String, favorite: Boolean) {}
        override fun deleteByTrain(trainId: String) {}
        override fun deleteAll() { deletedAll++ }
        override fun purgeExpired(nowMs: Long) = 0
    }

    @Test
    fun defaultIsOffAndUndecided() = runTest {
        val store = TrackConsentStore(MemPrefs(), Repo(), TestScope(StandardTestDispatcher(testScheduler)))
        assertFalse(store.saveTracks.value)
        assertFalse(store.hasDecided())
        assertEquals(0L, store.consentAtMs())
    }

    @Test
    fun grantStoresStateAndTime() = runTest {
        val store = TrackConsentStore(MemPrefs(), Repo(), TestScope(StandardTestDispatcher(testScheduler)), nowMs = { 12345L })
        store.setConsent(true)
        assertTrue(store.saveTracks.value)
        assertTrue(store.hasDecided())
        assertEquals(12345L, store.consentAtMs())
    }

    @Test
    fun revokeDeletesLocalTracks() = runTest {
        val repo = Repo()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = TrackConsentStore(MemPrefs(), repo, CoroutineScope(dispatcher + Job()))
        store.setConsent(true)
        store.setConsent(false)
        advanceUntilIdle()
        assertFalse(store.saveTracks.value)
        assertEquals(0L, store.consentAtMs())
        // удаление идёт на Dispatchers.Default; дожидаемся
        Thread.sleep(200)
        assertEquals(1, repo.deletedAll)
    }

    @Test
    fun noConsentWarningShownOnce() = runTest {
        val store = TrackConsentStore(MemPrefs(), Repo(), TestScope(StandardTestDispatcher(testScheduler)))
        assertTrue(store.takeNoConsentWarning())
        assertFalse(store.takeNoConsentWarning())
        // после согласия и нового отзыва предупредим снова
        store.setConsent(true)
        assertFalse(store.takeNoConsentWarning())
        store.setConsent(false)
        assertTrue(store.takeNoConsentWarning())
    }

    @Test
    fun persistsAcrossInstances() = runTest {
        val prefs = MemPrefs()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        TrackConsentStore(prefs, Repo(), scope).setConsent(true)
        assertTrue(TrackConsentStore(prefs, Repo(), scope).saveTracks.value)
    }
}
