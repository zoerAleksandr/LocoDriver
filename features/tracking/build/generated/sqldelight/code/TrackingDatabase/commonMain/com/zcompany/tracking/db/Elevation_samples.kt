package com.zcompany.tracking.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Elevation_samples(
  public val id: Long,
  public val session_id: String,
  public val latitude: Double,
  public val longitude: Double,
  public val gps_altitude: Double,
  public val barometer_altitude: Double?,
  public val horizontal_accuracy_meters: Double,
  public val vertical_accuracy_meters: Double?,
  public val timestamp: Long,
  public val distance_from_session_start_km: Double,
  public val is_synced: Long,
)
