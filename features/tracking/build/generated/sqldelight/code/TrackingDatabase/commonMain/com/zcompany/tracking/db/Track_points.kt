package com.zcompany.tracking.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Track_points(
  public val id: Long,
  public val session_id: String,
  public val latitude: Double,
  public val longitude: Double,
  public val speed_kmh: Double,
  public val accuracy_meters: Double,
  public val timestamp: Long,
  public val altitude: Double,
  public val bearing_degrees: Double,
  public val barometer_altitude: Double?,
  public val vertical_accuracy_meters: Double?,
)
