package com.zcompany.tracking.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Stations(
  public val id: String,
  public val name: String,
  public val latitude: Double,
  public val longitude: Double,
  public val type: String,
  public val scheduled_arrival: Long?,
  public val scheduled_departure: Long?,
  public val geofence_radius_meters: Double,
)
