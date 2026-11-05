package com.z_company.iosapp.viewmodel

import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.PartnerUseCase
import com.z_company.domain.use_cases.RoutePartnerUseCase
import com.z_company.domain.util.generateId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Напарники (SCREEN_SPECS §8.5): справочник MANAGE/SELECT и редактор записи.

/** Инициалы аватара: первые буквы первых двух слов ФИО, uppercase; пусто — «?» (`partnerInitials`). */
internal fun partnerInitialsOf(fullName: String?): String {
    val words = fullName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/** Короткое ФИО: «Иванов Иван Иванович» → «Иванов И. И.» (`partnerShortName`). */
internal fun partnerShortNameOf(fullName: String?): String {
    val words = fullName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    val initials = words.drop(1).joinToString(" ") { "${it.take(1).uppercase()}." }
    return if (initials.isBlank()) words[0] else "${words[0]} $initials"
}

data class PartnerRowUi(
    val id: String,
    val fullName: String,
    val initials: String,
    val shortName: String,
    val tabNumber: String?,
    val notes: String?,
    val isPermanent: Boolean,
    val isSelected: Boolean,
)

/** Запрос подтверждения замены постоянного напарника (§8.5.4). */
data class PartnerReplaceUi(val currentShort: String, val candidateShort: String)

data class PartnersScreenState(
    val rows: List<PartnerRowUi> = emptyList(),
    /** Всего записей в справочнике (до поиска). */
    val totalCount: Int = 0,
    val query: String = "",
    val isSelectMode: Boolean = false,
    /** Кандидат переключателя «Постоянный напарник» (SELECT, §8.5.1); null — переключатель неактивен. */
    val permanentCandidateShort: String? = null,
    val permanentCandidateIsPermanent: Boolean = false,
    val replaceRequest: PartnerReplaceUi? = null,
)

/** Фильтр поиска (`filterPartners`): каждое слово запроса должно быть в ФИО/табельном/примечании. */
private fun filterPartnerList(partners: List<Partner>, query: String): List<Partner> {
    val terms = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (terms.isEmpty()) return partners
    return partners.filter { p ->
        val text = buildString {
            append(p.fullName)
            p.tabNumber?.let { append(' ').append(it) }
            p.notes?.let { append(' ').append(it) }
        }
        terms.all { text.contains(it, ignoreCase = true) }
    }
}

/**
 * Список напарников: `basicId == null` — MANAGE (из Настроек), иначе SELECT (из формы маршрута).
 * Аналог `PartnerListViewModel` + `PartnerPickerViewModel`.
 */
class PartnersScreenIosViewModel(
    private val partnerUseCase: PartnerUseCase,
    private val routePartnerUseCase: RoutePartnerUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(PartnersScreenState())

    private var basicId: String? = null
    private var started = false
    private var partners: List<Partner> = emptyList()
    private var selected: Set<String> = emptySet()
    private var lastSelectedId: String? = null
    private var query = ""
    private var replace: Pair<Partner, Partner>? = null

    fun watchState(callback: (PartnersScreenState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    fun start(basicId: String?) {
        if (started) return
        started = true
        this.basicId = basicId
        if (basicId != null) {
            // Изначально отмечены напарники, уже добавленные в маршрут (по sourcePartnerId).
            selected = routePartnerUseCase.getPartnerListByBasicId(basicId).mapNotNull { it.sourcePartnerId }.toSet()
        }
        publish()
        scope.launch {
            partnerUseCase.getAllFlow().collect {
                partners = it
                publish()
            }
        }
    }

    fun setQuery(value: String) {
        query = value
        publish()
    }

    /** Тап по строке в SELECT: выбор/снятие. */
    fun toggle(partnerId: String) {
        val was = partnerId in selected
        selected = if (was) selected - partnerId else selected + partnerId
        if (was) {
            if (lastSelectedId == partnerId) lastSelectedId = null
        } else {
            lastSelectedId = partnerId
        }
        publish()
    }

    /** Свайп-удаление записи справочника (в SELECT также снимает выделение). */
    fun deletePartner(partnerId: String) {
        selected = selected - partnerId
        if (lastSelectedId == partnerId) lastSelectedId = null
        sharedPrefs.setSettingsSyncPending(true)
        scope.launch { partnerUseCase.delete(partnerId).collect {} }
    }

    private fun candidate(): Partner? {
        val last = lastSelectedId?.takeIf { it in selected }?.let { id -> partners.find { it.partnerId == id } }
        return last ?: partners.find { it.isPermanent && it.partnerId in selected }
    }

    /** Переключатель «Постоянный напарник» (SELECT). */
    fun setPermanentForCandidate(value: Boolean) {
        val target = candidate() ?: return
        if (value) makePermanent(target) else unsetPermanent(target)
    }

    private fun makePermanent(p: Partner) {
        if (p.isPermanent) return
        val current = partners.firstOrNull { it.isPermanent && it.partnerId != p.partnerId }
        if (current != null) {
            replace = current to p
            publish()
        } else {
            writePermanent(p.partnerId)
        }
    }

    private fun unsetPermanent(p: Partner) {
        if (!p.isPermanent) return
        writePermanent(null)
    }

    fun confirmReplacePermanent() {
        val r = replace ?: return
        replace = null
        publish()
        writePermanent(r.second.partnerId)
    }

    fun dismissReplacePermanent() {
        replace = null
        publish()
    }

    /** Флаг пишется в справочник сразу, независимо от «Готово»/Back (§8.5.1). */
    private fun writePermanent(id: String?) {
        sharedPrefs.setSettingsSyncPending(true)
        scope.launch { partnerUseCase.setPermanent(id).collect {} }
    }

    /** «Готово» в SELECT: сверка копий в маршруте, затем [onComplete]. */
    fun confirm(onComplete: () -> Unit) {
        val id = basicId ?: return
        scope.launch {
            val existing = routePartnerUseCase.getPartnerListByBasicId(id)
            val existingBySource = existing.mapNotNull { rp -> rp.sourcePartnerId?.let { it to rp } }.toMap()
            for (rp in existing) {
                val src = rp.sourcePartnerId
                if (src != null && src !in selected) routePartnerUseCase.removePartner(rp).collect {}
            }
            for (partnerId in selected) {
                if (!existingBySource.containsKey(partnerId)) {
                    val partner = partners.find { it.partnerId == partnerId } ?: continue
                    routePartnerUseCase.savePartner(
                        RoutePartner(
                            basicId = id,
                            sourcePartnerId = partner.partnerId,
                            fullName = partner.fullName,
                            tabNumber = partner.tabNumber,
                            notes = partner.notes,
                        )
                    ).collect {}
                }
            }
            onComplete()
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun publish() {
        val isSelect = basicId != null
        val filtered = filterPartnerList(partners, query)
        val cand = if (isSelect) candidate() else null
        _state.value = PartnersScreenState(
            rows = filtered.map {
                PartnerRowUi(
                    id = it.partnerId,
                    fullName = it.fullName,
                    initials = partnerInitialsOf(it.fullName),
                    shortName = partnerShortNameOf(it.fullName),
                    tabNumber = it.tabNumber?.takeIf { t -> t.isNotBlank() },
                    notes = it.notes?.takeIf { n -> n.isNotBlank() },
                    isPermanent = it.isPermanent,
                    isSelected = it.partnerId in selected,
                )
            },
            totalCount = partners.size,
            query = query,
            isSelectMode = isSelect,
            permanentCandidateShort = cand?.let { partnerShortNameOf(it.fullName) },
            permanentCandidateIsPermanent = cand?.isPermanent == true,
            replaceRequest = replace?.let {
                PartnerReplaceUi(
                    currentShort = partnerShortNameOf(it.first.fullName),
                    candidateShort = partnerShortNameOf(it.second.fullName).ifBlank { "Новый напарник" },
                )
            },
        )
    }
}

data class PartnerEditorScreenState(
    val isNew: Boolean = true,
    val fullName: String = "",
    val tabNumber: String = "",
    val notes: String = "",
    val isPermanent: Boolean = false,
    val replaceRequest: PartnerReplaceUi? = null,
    val deleted: Boolean = false,
)

/** Редактор записи справочника (§8.5.2): автосейв 500 мс, `persistentId` один раз. */
class PartnerEditorIosViewModel(
    private val partnerUseCase: PartnerUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val partnerId: String?,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val persistentId: String = partnerId ?: generateId()
    private val _state = MutableStateFlow(PartnerEditorScreenState(isNew = partnerId == null))
    private var replaceCurrent: Partner? = null
    private var saveJob: Job? = null
    private var deleted = false

    init {
        if (partnerId != null) {
            scope.launch {
                partnerUseCase.getById(partnerId)?.let { p ->
                    _state.value = _state.value.copy(
                        fullName = p.fullName,
                        tabNumber = p.tabNumber.orEmpty(),
                        notes = p.notes.orEmpty(),
                        isPermanent = p.isPermanent,
                    )
                }
            }
        }
    }

    fun watchState(callback: (PartnerEditorScreenState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    fun setFullName(value: String) { _state.value = _state.value.copy(fullName = value); scheduleSave() }
    fun setTabNumber(value: String) { _state.value = _state.value.copy(tabNumber = value); scheduleSave() }
    fun setNotes(value: String) { _state.value = _state.value.copy(notes = value); scheduleSave() }

    /** Включение при другом постоянном — через подтверждение замены (§8.5.4). */
    fun setPermanent(value: Boolean) {
        if (value) {
            val current = partnerUseCase.getPermanent()?.takeIf { it.partnerId != persistentId }
            if (current != null) {
                replaceCurrent = current
                publishReplace()
                return
            }
        }
        _state.value = _state.value.copy(isPermanent = value)
        saveNow()
    }

    fun confirmReplacePermanent() {
        replaceCurrent = null
        _state.value = _state.value.copy(isPermanent = true, replaceRequest = null)
        saveNow()
    }

    fun dismissReplacePermanent() {
        replaceCurrent = null
        _state.value = _state.value.copy(replaceRequest = null)
    }

    private fun publishReplace() {
        val cur = replaceCurrent ?: return
        _state.value = _state.value.copy(
            replaceRequest = PartnerReplaceUi(
                currentShort = partnerShortNameOf(cur.fullName),
                candidateShort = partnerShortNameOf(_state.value.fullName).ifBlank { "Новый напарник" },
            )
        )
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(500)
            write()
        }
    }

    /** Выход с экрана: сохранить немедленно (не дожидаясь debounce). */
    fun saveNow() {
        saveJob?.cancel()
        // Вне scope: выход с экрана сразу вызывает dispose() и отменил бы запись.
        CoroutineScope(NonCancellable + Dispatchers.Default).launch { write() }
    }

    /** Запись с пустым ФИО не создаётся; после удаления не пересоздаётся. */
    private suspend fun write() {
        val s = _state.value
        if (deleted || s.fullName.isBlank()) return
        sharedPrefs.setSettingsSyncPending(true)
        partnerUseCase.upsert(
            Partner(
                partnerId = persistentId,
                fullName = s.fullName.trim(),
                tabNumber = s.tabNumber.trim().ifBlank { null },
                notes = s.notes.trim().ifBlank { null },
                isPermanent = s.isPermanent,
            )
        ).first { it !is com.z_company.core.ResultState.Loading }
    }

    /** BUG-30 (паритет с Android): удаление без подтверждения. */
    fun delete() {
        saveJob?.cancel()
        deleted = true
        sharedPrefs.setSettingsSyncPending(true)
        scope.launch {
            partnerUseCase.delete(persistentId).collect {}
            _state.value = _state.value.copy(deleted = true)
        }
    }

    fun dispose() {
        saveJob?.cancel()
        if (!deleted && _state.value.fullName.isNotBlank()) {
            CoroutineScope(NonCancellable + Dispatchers.Default).launch { write() }
        }
        scope.cancel()
    }
}
