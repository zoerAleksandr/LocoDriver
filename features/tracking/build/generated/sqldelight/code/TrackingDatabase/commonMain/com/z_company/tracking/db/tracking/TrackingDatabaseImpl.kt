package com.z_company.tracking.db.tracking

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.z_company.tracking.db.TrackingDatabase
import com.zcompany.tracking.db.Tracking_databaseQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<TrackingDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = TrackingDatabaseImpl.Schema

internal fun KClass<TrackingDatabase>.newInstance(driver: SqlDriver): TrackingDatabase =
    TrackingDatabaseImpl(driver)

private class TrackingDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), TrackingDatabase {
  override val tracking_databaseQueries: Tracking_databaseQueries = Tracking_databaseQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS stations (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    name TEXT NOT NULL,
          |    latitude REAL NOT NULL,
          |    longitude REAL NOT NULL,
          |    type TEXT NOT NULL DEFAULT 'INTERMEDIATE',
          |    scheduled_arrival INTEGER,           -- epoch millis
          |    scheduled_departure INTEGER,         -- epoch millis
          |    geofence_radius_meters REAL NOT NULL DEFAULT 500.0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS trip_sessions (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    start_time INTEGER NOT NULL,         -- epoch millis
          |    end_time INTEGER,                    -- epoch millis, NULL while active
          |    route_id TEXT,
          |    total_distance_km REAL NOT NULL DEFAULT 0.0,
          |    max_speed_kmh REAL NOT NULL DEFAULT 0.0,
          |    avg_speed_kmh REAL NOT NULL DEFAULT 0.0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS station_events (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    session_id TEXT NOT NULL,
          |    station_id TEXT NOT NULL,
          |    event_type TEXT NOT NULL,            -- ARRIVAL | DEPARTURE | PASS_THROUGH
          |    actual_time INTEGER NOT NULL,        -- epoch millis
          |    delay_minutes INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY (session_id) REFERENCES trip_sessions(id) ON DELETE CASCADE,
          |    FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS track_points (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    session_id TEXT NOT NULL,
          |    latitude REAL NOT NULL,
          |    longitude REAL NOT NULL,
          |    speed_kmh REAL NOT NULL DEFAULT 0.0,
          |    accuracy_meters REAL NOT NULL DEFAULT 999.0,
          |    timestamp INTEGER NOT NULL,          -- epoch millis
          |    altitude REAL NOT NULL DEFAULT 0.0,
          |    bearing_degrees REAL NOT NULL DEFAULT 0.0,
          |    barometer_altitude REAL,             -- from device pressure sensor, NULL if unavailable
          |    vertical_accuracy_meters REAL,       -- GPS vertical accuracy, NULL if unavailable
          |    FOREIGN KEY (session_id) REFERENCES trip_sessions(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS elevation_segments (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    from_station_id TEXT NOT NULL,
          |    from_station_name TEXT NOT NULL,
          |    to_station_id TEXT NOT NULL,
          |    to_station_name TEXT NOT NULL,
          |    total_distance_km REAL NOT NULL DEFAULT 0.0,
          |    min_elevation_meters REAL NOT NULL DEFAULT 0.0,
          |    max_elevation_meters REAL NOT NULL DEFAULT 0.0,
          |    contribution_count INTEGER NOT NULL DEFAULT 0,
          |    updated_at INTEGER NOT NULL DEFAULT 0,  -- epoch millis
          |    source TEXT NOT NULL DEFAULT 'CROWDSOURCED',       -- CROWDSOURCED | MANUAL | MANUAL_PLUS_AUTO
          |    is_manually_verified INTEGER NOT NULL DEFAULT 0,   -- 0 = false, 1 = true
          |    auto_collection_mode TEXT NOT NULL DEFAULT 'COLLECT_AND_CORRECT' -- COLLECT_AND_CORRECT | COLLECT_ONLY | DISABLED
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS elevation_points (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    segment_id TEXT NOT NULL,
          |    distance_from_start_km REAL NOT NULL,
          |    elevation_meters REAL NOT NULL,
          |    gradient_permille REAL NOT NULL DEFAULT 0.0,
          |    contribution_count INTEGER NOT NULL DEFAULT 1,
          |    FOREIGN KEY (segment_id) REFERENCES elevation_segments(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS elevation_samples (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    session_id TEXT NOT NULL,
          |    latitude REAL NOT NULL,
          |    longitude REAL NOT NULL,
          |    gps_altitude REAL NOT NULL,
          |    barometer_altitude REAL,
          |    horizontal_accuracy_meters REAL NOT NULL,
          |    vertical_accuracy_meters REAL,
          |    timestamp INTEGER NOT NULL,                -- epoch millis
          |    distance_from_session_start_km REAL NOT NULL DEFAULT 0.0,
          |    is_synced INTEGER NOT NULL DEFAULT 0,      -- 0 = pending, 1 = sent to server
          |    FOREIGN KEY (session_id) REFERENCES trip_sessions(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE INDEX IF NOT EXISTS idx_track_points_session
          |    ON track_points(session_id)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE INDEX IF NOT EXISTS idx_station_events_session
          |    ON station_events(session_id)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE INDEX IF NOT EXISTS idx_elevation_points_segment
          |    ON elevation_points(segment_id)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE INDEX IF NOT EXISTS idx_elevation_samples_session
          |    ON elevation_samples(session_id)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE INDEX IF NOT EXISTS idx_elevation_samples_sync
          |    ON elevation_samples(is_synced)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
