package com.zcompany.tracking.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Elevation_segments(
  public val id: String,
  public val from_station_id: String,
  public val from_station_name: String,
  public val to_station_id: String,
  public val to_station_name: String,
  public val total_distance_km: Double,
  public val min_elevation_meters: Double,
  public val max_elevation_meters: Double,
  public val contribution_count: Long,
  public val updated_at: Long,
  public val source: String,
  public val is_manually_verified: Long,
  public val auto_collection_mode: String,
)
