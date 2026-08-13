package com.z_company.domain.repositories

import com.z_company.domain.entities.SchedulePattern

interface SharedPreferencesRepositories {
    fun setLastSyncTimestamp(time: Long)
    fun getLastSyncTimestamp(): Long

    /**
     * Паттерны графика мастера «Заполнить месяц» (локально, без синхронизации).
     * Возвращает null, если ещё не сохранялись (мастер засидит дефолты).
     */
    fun getSchedulePatterns(): List<SchedulePattern>?
    fun setSchedulePatterns(patterns: List<SchedulePattern>)

    fun getOPKeyRobokassa(): String?
    fun setOPKeyRobokassa(opKey: String?)
    fun isMigrated(): Boolean
    fun setIsMigrated(value: Boolean)
    fun isShowUpdatePresentation(): Boolean
    fun enableShowingUpdatePresentation()
    fun getSubscriptionExpiration(): Long
    fun tokenIsChangesHave(): Boolean
    fun tokenIsFirstAppEntry(): Boolean
    fun setSubscriptionExpiration(value: Long)
    fun setTokenIsChangeHave(value: Boolean)
    fun setTokenIsFirstAppEntry(value: Boolean)
    fun tokenIsLoadStationAndLocomotiveSeries(): Boolean
    fun setTokenIsLoadStationAndLocomotiveSeries(value: Boolean)
    fun toggleStationsSortOrder(value: Boolean)
    fun isReversedSortStationList(): Boolean
    fun toggleInputDieselInKilo(value: Boolean)
    fun isInputDieselInKilo(): Boolean
    fun getSortOption(): String?
    fun setSortOption(value: String)
    fun getSelectedFilters(): Set<String>?
    fun setSelectedFilters(values: Set<String>)
    fun isExpandedView(): Boolean
    fun setIsExpandedView(value: Boolean)
    // Показывать ли объединение смежных плеч «туда → отдых в ПО → обратно» на
    // экране «Все маршруты» (трей с коннектором). По умолчанию включено.
    fun isShowTurnaroundRest(): Boolean
    fun setShowTurnaroundRest(value: Boolean)
    fun toggleShowTravelTime(value: Boolean)
    fun isShowTravelTime(): Boolean
    fun isShowLocoFormUpdateHint(): Boolean
    fun setLocoFormUpdateHintShown()

    /**
     * Инфо-окно про оплату недоработки (когда норма не выработана, а средний час
     * не задан). Один раз нажал «Понятно» — больше не показываем.
     */
    fun isUnderworkInfoDismissed(): Boolean
    fun setUnderworkInfoDismissed()
    fun isLocoSectionTimeExpanded(): Boolean
    fun setLocoSectionTimeExpanded(value: Boolean)
    fun isLocoSectionHeatingExpanded(): Boolean
    fun setLocoSectionHeatingExpanded(value: Boolean)
    fun isLocoSectionAuxiliaryExpanded(): Boolean
    fun setLocoSectionAuxiliaryExpanded(value: Boolean)
    fun isLocoSectionStatisticsExpanded(): Boolean
    fun setLocoSectionStatisticsExpanded(value: Boolean)
    fun isShowLocoHeating(): Boolean
    fun setShowLocoHeating(value: Boolean)
    fun isShowLocoAuxiliary(): Boolean
    fun setShowLocoAuxiliary(value: Boolean)
    fun isShowLocoStatistics(): Boolean
    fun setShowLocoStatistics(value: Boolean)
    fun isShowLocoNorma(): Boolean
    fun setShowLocoNorma(value: Boolean)
    fun isShowOtherCurrent(): Boolean
    fun setShowOtherCurrent(value: Boolean)
    fun isLocoSectionNormaExpanded(): Boolean
    fun setLocoSectionNormaExpanded(value: Boolean)
    fun isPassenger12hDontAskAgain(): Boolean
    fun setPassenger12hDontAskAgain(value: Boolean)

    /**
     * Выбор нормы приёмки/сдачи в шторке времени: false — «После отстоя без бригады»
     * (по умолчанию), true — «Из рук в руки». Запоминается локально между открытиями.
     */
    fun isLocoNormHandToHand(): Boolean
    fun setLocoNormHandToHand(value: Boolean)
    fun isPassenger12hAutoAccepted(): Boolean
    fun setPassenger12hAutoAccepted(value: Boolean)

    fun getRecentTimes(key: String): List<Long>
    fun addRecentTime(key: String, timeMillis: Long)

    /** Недавние коэффициенты (последние 5), для подсказок в шторке. */
    fun getRecentCoefficients(): List<String>
    fun addRecentCoefficient(value: String)

    /**
     * Последний введённый коэффициент экипировки — подставляется по умолчанию
     * в новую секцию тепловоза. По умолчанию "0.83".
     */
    fun getLastRefuelCoefficient(): String
    fun setLastRefuelCoefficient(value: String)

    /** true = пользователь предпочитает ввод с клавиатуры в системном пикере времени */
    fun isTimePickerKeyboardInput(): Boolean
    fun setTimePickerKeyboardInput(value: Boolean)

    fun isTimezoneMigrationDone(): Boolean
    fun setTimezoneMigrationDone()

    /** Флаг одноразовой миграции отвлечений из MonthOfYear.days → таблицу ReleaseDay */
    fun isReleaseDayMigrationDone(): Boolean
    fun setReleaseDayMigrationDone()

    /** Флаг одноразовой миграции тегов дней из MonthOfYear → ProductionCalendarDay */
    fun isProductionCalendarMigrationDone(): Boolean
    fun setProductionCalendarMigrationDone()

    /** Последний выбранный тип «прочей работы» — подставляется по умолчанию в новую запись. */
    fun getLastOtherWorkType(): String?
    fun setLastOtherWorkType(value: String?)

    /**
     * Номер последнего показанного пользователю сообщения-«новости при запуске»
     * (см. [com.z_company.domain.use_cases.AnnouncementUseCase]).
     * Возвращает [com.z_company.domain.use_cases.AnnouncementUseCase.NOT_SEEN] (-1),
     * если ещё ничего не показывалось (первый запуск/переустановка).
     */
    fun getLastSeenAnnouncementNumber(): Int
    fun setLastSeenAnnouncementNumber(value: Int)
}