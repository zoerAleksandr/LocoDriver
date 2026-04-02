package com.zcompany.tracking.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Trip_sessions(
  public val id: String,
  public val start_time: Long,
  public val end_time: Long?,
  public val route_id: String?,
  public val total_distance_km: Double,
  public val max_speed_kmh: Double,
  public val avg_speed_kmh: Double,
)
