package com.z_company.route.component.servicephase

import com.z_company.domain.util.RailwayByEsr
import com.z_company.repository.remote_rest.response.OsmStationCandidate

/** Дорога найденной станции («Октябрьская ЖД») по operator или коду ЕСР; null - неизвестна (подпись не показывается). */
internal fun OsmStationCandidate.railwayLabel(): String? = RailwayByEsr.label(esr, operator)
