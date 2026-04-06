package com.zcompany.tracking.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Elevation_points(
  public val id: Long,
  public val segment_id: String,
  public val distance_from_start_km: Double,
  public val elevation_meters: Double,
  public val gradient_permille: Double,
  public val contribution_count: Long,
)
