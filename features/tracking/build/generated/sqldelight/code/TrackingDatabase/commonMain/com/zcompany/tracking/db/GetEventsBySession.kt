package com.zcompany.tracking.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class GetEventsBySession(
  public val id: String,
  public val session_id: String,
  public val station_id: String,
  public val event_type: String,
  public val actual_time: Long,
  public val delay_minutes: Long,
  public val station_name: String,
  public val station_lat: Double,
  public val station_lon: Double,
  public val station_type: String,
  public val geofence_radius_meters: Double,
)
