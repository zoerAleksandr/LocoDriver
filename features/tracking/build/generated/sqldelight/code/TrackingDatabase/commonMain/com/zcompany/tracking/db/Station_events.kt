package com.zcompany.tracking.db

import kotlin.Long
import kotlin.String

public data class Station_events(
  public val id: String,
  public val session_id: String,
  public val station_id: String,
  public val event_type: String,
  public val actual_time: Long,
  public val delay_minutes: Long,
)
