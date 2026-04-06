package com.zcompany.tracking.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

public class Tracking_databaseQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllStations(mapper: (
    id: String,
    name: String,
    latitude: Double,
    longitude: Double,
    type: String,
    scheduled_arrival: Long?,
    scheduled_departure: Long?,
    geofence_radius_meters: Double,
  ) -> T): Query<T> = Query(2_035_052_408, arrayOf("stations"), driver, "tracking_database.sq",
      "getAllStations",
      "SELECT stations.id, stations.name, stations.latitude, stations.longitude, stations.type, stations.scheduled_arrival, stations.scheduled_departure, stations.geofence_radius_meters FROM stations") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getDouble(2)!!,
      cursor.getDouble(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getDouble(7)!!
    )
  }

  public fun getAllStations(): Query<Stations> = getAllStations { id, name, latitude, longitude,
      type, scheduled_arrival, scheduled_departure, geofence_radius_meters ->
    Stations(
      id,
      name,
      latitude,
      longitude,
      type,
      scheduled_arrival,
      scheduled_departure,
      geofence_radius_meters
    )
  }

  public fun <T : Any> getStationById(id: String, mapper: (
    id: String,
    name: String,
    latitude: Double,
    longitude: Double,
    type: String,
    scheduled_arrival: Long?,
    scheduled_departure: Long?,
    geofence_radius_meters: Double,
  ) -> T): Query<T> = GetStationByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getDouble(2)!!,
      cursor.getDouble(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5),
      cursor.getLong(6),
      cursor.getDouble(7)!!
    )
  }

  public fun getStationById(id: String): Query<Stations> = getStationById(id) { id_, name, latitude,
      longitude, type, scheduled_arrival, scheduled_departure, geofence_radius_meters ->
    Stations(
      id_,
      name,
      latitude,
      longitude,
      type,
      scheduled_arrival,
      scheduled_departure,
      geofence_radius_meters
    )
  }

  public fun <T : Any> getSessionById(id: String, mapper: (
    id: String,
    start_time: Long,
    end_time: Long?,
    route_id: String?,
    total_distance_km: Double,
    max_speed_kmh: Double,
    avg_speed_kmh: Double,
  ) -> T): Query<T> = GetSessionByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2),
      cursor.getString(3),
      cursor.getDouble(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!
    )
  }

  public fun getSessionById(id: String): Query<Trip_sessions> = getSessionById(id) { id_,
      start_time, end_time, route_id, total_distance_km, max_speed_kmh, avg_speed_kmh ->
    Trip_sessions(
      id_,
      start_time,
      end_time,
      route_id,
      total_distance_km,
      max_speed_kmh,
      avg_speed_kmh
    )
  }

  public fun <T : Any> getAllSessions(mapper: (
    id: String,
    start_time: Long,
    end_time: Long?,
    route_id: String?,
    total_distance_km: Double,
    max_speed_kmh: Double,
    avg_speed_kmh: Double,
  ) -> T): Query<T> = Query(2_121_800_278, arrayOf("trip_sessions"), driver, "tracking_database.sq",
      "getAllSessions",
      "SELECT trip_sessions.id, trip_sessions.start_time, trip_sessions.end_time, trip_sessions.route_id, trip_sessions.total_distance_km, trip_sessions.max_speed_kmh, trip_sessions.avg_speed_kmh FROM trip_sessions ORDER BY start_time DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2),
      cursor.getString(3),
      cursor.getDouble(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!
    )
  }

  public fun getAllSessions(): Query<Trip_sessions> = getAllSessions { id, start_time, end_time,
      route_id, total_distance_km, max_speed_kmh, avg_speed_kmh ->
    Trip_sessions(
      id,
      start_time,
      end_time,
      route_id,
      total_distance_km,
      max_speed_kmh,
      avg_speed_kmh
    )
  }

  public fun <T : Any> getEventsBySession(sessionId: String, mapper: (
    id: String,
    session_id: String,
    station_id: String,
    event_type: String,
    actual_time: Long,
    delay_minutes: Long,
    station_name: String,
    station_lat: Double,
    station_lon: Double,
    station_type: String,
    geofence_radius_meters: Double,
  ) -> T): Query<T> = GetEventsBySessionQuery(sessionId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getDouble(7)!!,
      cursor.getDouble(8)!!,
      cursor.getString(9)!!,
      cursor.getDouble(10)!!
    )
  }

  public fun getEventsBySession(sessionId: String): Query<GetEventsBySession> =
      getEventsBySession(sessionId) { id, session_id, station_id, event_type, actual_time,
      delay_minutes, station_name, station_lat, station_lon, station_type, geofence_radius_meters ->
    GetEventsBySession(
      id,
      session_id,
      station_id,
      event_type,
      actual_time,
      delay_minutes,
      station_name,
      station_lat,
      station_lon,
      station_type,
      geofence_radius_meters
    )
  }

  public fun <T : Any> getRecentEvents(
    sessionId: String,
    limit: Long,
    mapper: (
      id: String,
      session_id: String,
      station_id: String,
      event_type: String,
      actual_time: Long,
      delay_minutes: Long,
      station_name: String,
      station_lat: Double,
      station_lon: Double,
      station_type: String,
      geofence_radius_meters: Double,
    ) -> T,
  ): Query<T> = GetRecentEventsQuery(sessionId, limit) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getDouble(7)!!,
      cursor.getDouble(8)!!,
      cursor.getString(9)!!,
      cursor.getDouble(10)!!
    )
  }

  public fun getRecentEvents(sessionId: String, limit: Long): Query<GetRecentEvents> =
      getRecentEvents(sessionId, limit) { id, session_id, station_id, event_type, actual_time,
      delay_minutes, station_name, station_lat, station_lon, station_type, geofence_radius_meters ->
    GetRecentEvents(
      id,
      session_id,
      station_id,
      event_type,
      actual_time,
      delay_minutes,
      station_name,
      station_lat,
      station_lon,
      station_type,
      geofence_radius_meters
    )
  }

  public fun <T : Any> getTrackPointsBySession(sessionId: String, mapper: (
    id: Long,
    session_id: String,
    latitude: Double,
    longitude: Double,
    speed_kmh: Double,
    accuracy_meters: Double,
    timestamp: Long,
    altitude: Double,
    bearing_degrees: Double,
    barometer_altitude: Double?,
    vertical_accuracy_meters: Double?,
  ) -> T): Query<T> = GetTrackPointsBySessionQuery(sessionId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getDouble(2)!!,
      cursor.getDouble(3)!!,
      cursor.getDouble(4)!!,
      cursor.getDouble(5)!!,
      cursor.getLong(6)!!,
      cursor.getDouble(7)!!,
      cursor.getDouble(8)!!,
      cursor.getDouble(9),
      cursor.getDouble(10)
    )
  }

  public fun getTrackPointsBySession(sessionId: String): Query<Track_points> =
      getTrackPointsBySession(sessionId) { id, session_id, latitude, longitude, speed_kmh,
      accuracy_meters, timestamp, altitude, bearing_degrees, barometer_altitude,
      vertical_accuracy_meters ->
    Track_points(
      id,
      session_id,
      latitude,
      longitude,
      speed_kmh,
      accuracy_meters,
      timestamp,
      altitude,
      bearing_degrees,
      barometer_altitude,
      vertical_accuracy_meters
    )
  }

  public fun countTrackPoints(sessionId: String): Query<Long> = CountTrackPointsQuery(sessionId) {
      cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> getElevationSegment(segmentId: String, mapper: (
    id: String,
    from_station_id: String,
    from_station_name: String,
    to_station_id: String,
    to_station_name: String,
    total_distance_km: Double,
    min_elevation_meters: Double,
    max_elevation_meters: Double,
    contribution_count: Long,
    updated_at: Long,
    source: String,
    is_manually_verified: Long,
    auto_collection_mode: String,
  ) -> T): Query<T> = GetElevationSegmentQuery(segmentId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getString(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12)!!
    )
  }

  public fun getElevationSegment(segmentId: String): Query<Elevation_segments> =
      getElevationSegment(segmentId) { id, from_station_id, from_station_name, to_station_id,
      to_station_name, total_distance_km, min_elevation_meters, max_elevation_meters,
      contribution_count, updated_at, source, is_manually_verified, auto_collection_mode ->
    Elevation_segments(
      id,
      from_station_id,
      from_station_name,
      to_station_id,
      to_station_name,
      total_distance_km,
      min_elevation_meters,
      max_elevation_meters,
      contribution_count,
      updated_at,
      source,
      is_manually_verified,
      auto_collection_mode
    )
  }

  public fun <T : Any> getElevationSegmentByStations(
    fromStationId: String,
    toStationId: String,
    mapper: (
      id: String,
      from_station_id: String,
      from_station_name: String,
      to_station_id: String,
      to_station_name: String,
      total_distance_km: Double,
      min_elevation_meters: Double,
      max_elevation_meters: Double,
      contribution_count: Long,
      updated_at: Long,
      source: String,
      is_manually_verified: Long,
      auto_collection_mode: String,
    ) -> T,
  ): Query<T> = GetElevationSegmentByStationsQuery(fromStationId, toStationId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getString(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12)!!
    )
  }

  public fun getElevationSegmentByStations(fromStationId: String, toStationId: String):
      Query<Elevation_segments> = getElevationSegmentByStations(fromStationId, toStationId) { id,
      from_station_id, from_station_name, to_station_id, to_station_name, total_distance_km,
      min_elevation_meters, max_elevation_meters, contribution_count, updated_at, source,
      is_manually_verified, auto_collection_mode ->
    Elevation_segments(
      id,
      from_station_id,
      from_station_name,
      to_station_id,
      to_station_name,
      total_distance_km,
      min_elevation_meters,
      max_elevation_meters,
      contribution_count,
      updated_at,
      source,
      is_manually_verified,
      auto_collection_mode
    )
  }

  public fun <T : Any> getAllElevationSegments(mapper: (
    id: String,
    from_station_id: String,
    from_station_name: String,
    to_station_id: String,
    to_station_name: String,
    total_distance_km: Double,
    min_elevation_meters: Double,
    max_elevation_meters: Double,
    contribution_count: Long,
    updated_at: Long,
    source: String,
    is_manually_verified: Long,
    auto_collection_mode: String,
  ) -> T): Query<T> = Query(1_022_477_188, arrayOf("elevation_segments"), driver,
      "tracking_database.sq", "getAllElevationSegments",
      "SELECT elevation_segments.id, elevation_segments.from_station_id, elevation_segments.from_station_name, elevation_segments.to_station_id, elevation_segments.to_station_name, elevation_segments.total_distance_km, elevation_segments.min_elevation_meters, elevation_segments.max_elevation_meters, elevation_segments.contribution_count, elevation_segments.updated_at, elevation_segments.source, elevation_segments.is_manually_verified, elevation_segments.auto_collection_mode FROM elevation_segments ORDER BY updated_at DESC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getDouble(5)!!,
      cursor.getDouble(6)!!,
      cursor.getDouble(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getString(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12)!!
    )
  }

  public fun getAllElevationSegments(): Query<Elevation_segments> = getAllElevationSegments { id,
      from_station_id, from_station_name, to_station_id, to_station_name, total_distance_km,
      min_elevation_meters, max_elevation_meters, contribution_count, updated_at, source,
      is_manually_verified, auto_collection_mode ->
    Elevation_segments(
      id,
      from_station_id,
      from_station_name,
      to_station_id,
      to_station_name,
      total_distance_km,
      min_elevation_meters,
      max_elevation_meters,
      contribution_count,
      updated_at,
      source,
      is_manually_verified,
      auto_collection_mode
    )
  }

  public fun <T : Any> getElevationPointsBySegment(segmentId: String, mapper: (
    id: Long,
    segment_id: String,
    distance_from_start_km: Double,
    elevation_meters: Double,
    gradient_permille: Double,
    contribution_count: Long,
  ) -> T): Query<T> = GetElevationPointsBySegmentQuery(segmentId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getDouble(2)!!,
      cursor.getDouble(3)!!,
      cursor.getDouble(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun getElevationPointsBySegment(segmentId: String): Query<Elevation_points> =
      getElevationPointsBySegment(segmentId) { id, segment_id, distance_from_start_km,
      elevation_meters, gradient_permille, contribution_count ->
    Elevation_points(
      id,
      segment_id,
      distance_from_start_km,
      elevation_meters,
      gradient_permille,
      contribution_count
    )
  }

  public fun <T : Any> getElevationSamplesBySession(sessionId: String, mapper: (
    id: Long,
    session_id: String,
    latitude: Double,
    longitude: Double,
    gps_altitude: Double,
    barometer_altitude: Double?,
    horizontal_accuracy_meters: Double,
    vertical_accuracy_meters: Double?,
    timestamp: Long,
    distance_from_session_start_km: Double,
    is_synced: Long,
  ) -> T): Query<T> = GetElevationSamplesBySessionQuery(sessionId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getDouble(2)!!,
      cursor.getDouble(3)!!,
      cursor.getDouble(4)!!,
      cursor.getDouble(5),
      cursor.getDouble(6)!!,
      cursor.getDouble(7),
      cursor.getLong(8)!!,
      cursor.getDouble(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun getElevationSamplesBySession(sessionId: String): Query<Elevation_samples> =
      getElevationSamplesBySession(sessionId) { id, session_id, latitude, longitude, gps_altitude,
      barometer_altitude, horizontal_accuracy_meters, vertical_accuracy_meters, timestamp,
      distance_from_session_start_km, is_synced ->
    Elevation_samples(
      id,
      session_id,
      latitude,
      longitude,
      gps_altitude,
      barometer_altitude,
      horizontal_accuracy_meters,
      vertical_accuracy_meters,
      timestamp,
      distance_from_session_start_km,
      is_synced
    )
  }

  public fun <T : Any> getUnsyncedElevationSamples(limit: Long, mapper: (
    id: Long,
    session_id: String,
    latitude: Double,
    longitude: Double,
    gps_altitude: Double,
    barometer_altitude: Double?,
    horizontal_accuracy_meters: Double,
    vertical_accuracy_meters: Double?,
    timestamp: Long,
    distance_from_session_start_km: Double,
    is_synced: Long,
  ) -> T): Query<T> = GetUnsyncedElevationSamplesQuery(limit) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getDouble(2)!!,
      cursor.getDouble(3)!!,
      cursor.getDouble(4)!!,
      cursor.getDouble(5),
      cursor.getDouble(6)!!,
      cursor.getDouble(7),
      cursor.getLong(8)!!,
      cursor.getDouble(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun getUnsyncedElevationSamples(limit: Long): Query<Elevation_samples> =
      getUnsyncedElevationSamples(limit) { id, session_id, latitude, longitude, gps_altitude,
      barometer_altitude, horizontal_accuracy_meters, vertical_accuracy_meters, timestamp,
      distance_from_session_start_km, is_synced ->
    Elevation_samples(
      id,
      session_id,
      latitude,
      longitude,
      gps_altitude,
      barometer_altitude,
      horizontal_accuracy_meters,
      vertical_accuracy_meters,
      timestamp,
      distance_from_session_start_km,
      is_synced
    )
  }

  public fun countUnsyncedSamples(): Query<Long> = Query(-515_980_587, arrayOf("elevation_samples"),
      driver, "tracking_database.sq", "countUnsyncedSamples",
      "SELECT COUNT(*) FROM elevation_samples WHERE is_synced = 0") { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertStation(
    id: String,
    name: String,
    latitude: Double,
    longitude: Double,
    type: String,
    scheduled_arrival: Long?,
    scheduled_departure: Long?,
    geofence_radius_meters: Double,
  ) {
    driver.execute(1_508_847_693, """
        |INSERT OR REPLACE INTO stations (id, name, latitude, longitude, type,
        |    scheduled_arrival, scheduled_departure, geofence_radius_meters)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindString(0, id)
          bindString(1, name)
          bindDouble(2, latitude)
          bindDouble(3, longitude)
          bindString(4, type)
          bindLong(5, scheduled_arrival)
          bindLong(6, scheduled_departure)
          bindDouble(7, geofence_radius_meters)
        }
    notifyQueries(1_508_847_693) { emit ->
      emit("stations")
    }
  }

  public fun deleteStation(id: String) {
    driver.execute(1_815_583_003, """DELETE FROM stations WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(1_815_583_003) { emit ->
      emit("station_events")
      emit("stations")
    }
  }

  public fun deleteAllStations() {
    driver.execute(-179_298_585, """DELETE FROM stations""", 0)
    notifyQueries(-179_298_585) { emit ->
      emit("station_events")
      emit("stations")
    }
  }

  public fun insertSession(
    id: String,
    start_time: Long,
    end_time: Long?,
    route_id: String?,
    total_distance_km: Double,
    max_speed_kmh: Double,
    avg_speed_kmh: Double,
  ) {
    driver.execute(1_096_004_015, """
        |INSERT OR REPLACE INTO trip_sessions (id, start_time, end_time, route_id,
        |    total_distance_km, max_speed_kmh, avg_speed_kmh)
        |VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 7) {
          bindString(0, id)
          bindLong(1, start_time)
          bindLong(2, end_time)
          bindString(3, route_id)
          bindDouble(4, total_distance_km)
          bindDouble(5, max_speed_kmh)
          bindDouble(6, avg_speed_kmh)
        }
    notifyQueries(1_096_004_015) { emit ->
      emit("trip_sessions")
    }
  }

  public fun updateSessionEnd(
    endTime: Long?,
    totalDistanceKm: Double,
    maxSpeedKmh: Double,
    avgSpeedKmh: Double,
    id: String,
  ) {
    driver.execute(-787_321_156, """
        |UPDATE trip_sessions SET end_time = ?,
        |    total_distance_km = ?,
        |    max_speed_kmh = ?,
        |    avg_speed_kmh = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindLong(0, endTime)
          bindDouble(1, totalDistanceKm)
          bindDouble(2, maxSpeedKmh)
          bindDouble(3, avgSpeedKmh)
          bindString(4, id)
        }
    notifyQueries(-787_321_156) { emit ->
      emit("trip_sessions")
    }
  }

  public fun deleteSession(id: String) {
    driver.execute(1_402_739_325, """DELETE FROM trip_sessions WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(1_402_739_325) { emit ->
      emit("elevation_samples")
      emit("station_events")
      emit("track_points")
      emit("trip_sessions")
    }
  }

  public fun insertStationEvent(
    id: String,
    session_id: String,
    station_id: String,
    event_type: String,
    actual_time: Long,
    delay_minutes: Long,
  ) {
    driver.execute(-210_273_715, """
        |INSERT OR REPLACE INTO station_events (id, session_id, station_id,
        |    event_type, actual_time, delay_minutes)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, id)
          bindString(1, session_id)
          bindString(2, station_id)
          bindString(3, event_type)
          bindLong(4, actual_time)
          bindLong(5, delay_minutes)
        }
    notifyQueries(-210_273_715) { emit ->
      emit("station_events")
    }
  }

  public fun insertTrackPoint(
    session_id: String,
    latitude: Double,
    longitude: Double,
    speed_kmh: Double,
    accuracy_meters: Double,
    timestamp: Long,
    altitude: Double,
    bearing_degrees: Double,
    barometer_altitude: Double?,
    vertical_accuracy_meters: Double?,
  ) {
    driver.execute(198_400_332, """
        |INSERT INTO track_points (session_id, latitude, longitude, speed_kmh,
        |    accuracy_meters, timestamp, altitude, bearing_degrees,
        |    barometer_altitude, vertical_accuracy_meters)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, session_id)
          bindDouble(1, latitude)
          bindDouble(2, longitude)
          bindDouble(3, speed_kmh)
          bindDouble(4, accuracy_meters)
          bindLong(5, timestamp)
          bindDouble(6, altitude)
          bindDouble(7, bearing_degrees)
          bindDouble(8, barometer_altitude)
          bindDouble(9, vertical_accuracy_meters)
        }
    notifyQueries(198_400_332) { emit ->
      emit("track_points")
    }
  }

  public fun deleteTrackPointsBySession(sessionId: String) {
    driver.execute(-53_493_942, """DELETE FROM track_points WHERE session_id = ?""", 1) {
          bindString(0, sessionId)
        }
    notifyQueries(-53_493_942) { emit ->
      emit("track_points")
    }
  }

  public fun insertElevationSegment(
    id: String,
    from_station_id: String,
    from_station_name: String,
    to_station_id: String,
    to_station_name: String,
    total_distance_km: Double,
    min_elevation_meters: Double,
    max_elevation_meters: Double,
    contribution_count: Long,
    updated_at: Long,
    source: String,
    is_manually_verified: Long,
    auto_collection_mode: String,
  ) {
    driver.execute(-1_934_929_123, """
        |INSERT OR REPLACE INTO elevation_segments (id, from_station_id, from_station_name,
        |    to_station_id, to_station_name, total_distance_km,
        |    min_elevation_meters, max_elevation_meters, contribution_count, updated_at,
        |    source, is_manually_verified, auto_collection_mode)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 13) {
          bindString(0, id)
          bindString(1, from_station_id)
          bindString(2, from_station_name)
          bindString(3, to_station_id)
          bindString(4, to_station_name)
          bindDouble(5, total_distance_km)
          bindDouble(6, min_elevation_meters)
          bindDouble(7, max_elevation_meters)
          bindLong(8, contribution_count)
          bindLong(9, updated_at)
          bindString(10, source)
          bindLong(11, is_manually_verified)
          bindString(12, auto_collection_mode)
        }
    notifyQueries(-1_934_929_123) { emit ->
      emit("elevation_segments")
    }
  }

  public fun deleteElevationSegment(segmentId: String) {
    driver.execute(-193_924_593, """DELETE FROM elevation_segments WHERE id = ?""", 1) {
          bindString(0, segmentId)
        }
    notifyQueries(-193_924_593) { emit ->
      emit("elevation_points")
      emit("elevation_segments")
    }
  }

  public fun insertElevationPoint(
    segment_id: String,
    distance_from_start_km: Double,
    elevation_meters: Double,
    gradient_permille: Double,
    contribution_count: Long,
  ) {
    driver.execute(1_215_626_234, """
        |INSERT INTO elevation_points (segment_id, distance_from_start_km,
        |    elevation_meters, gradient_permille, contribution_count)
        |VALUES (?, ?, ?, ?, ?)
        """.trimMargin(), 5) {
          bindString(0, segment_id)
          bindDouble(1, distance_from_start_km)
          bindDouble(2, elevation_meters)
          bindDouble(3, gradient_permille)
          bindLong(4, contribution_count)
        }
    notifyQueries(1_215_626_234) { emit ->
      emit("elevation_points")
    }
  }

  public fun deleteElevationPointsBySegment(segmentId: String) {
    driver.execute(183_776_277, """DELETE FROM elevation_points WHERE segment_id = ?""", 1) {
          bindString(0, segmentId)
        }
    notifyQueries(183_776_277) { emit ->
      emit("elevation_points")
    }
  }

  public fun insertElevationSample(
    session_id: String,
    latitude: Double,
    longitude: Double,
    gps_altitude: Double,
    barometer_altitude: Double?,
    horizontal_accuracy_meters: Double,
    vertical_accuracy_meters: Double?,
    timestamp: Long,
    distance_from_session_start_km: Double,
  ) {
    driver.execute(-897_213_312, """
        |INSERT INTO elevation_samples (session_id, latitude, longitude,
        |    gps_altitude, barometer_altitude, horizontal_accuracy_meters,
        |    vertical_accuracy_meters, timestamp, distance_from_session_start_km, is_synced)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """.trimMargin(), 9) {
          bindString(0, session_id)
          bindDouble(1, latitude)
          bindDouble(2, longitude)
          bindDouble(3, gps_altitude)
          bindDouble(4, barometer_altitude)
          bindDouble(5, horizontal_accuracy_meters)
          bindDouble(6, vertical_accuracy_meters)
          bindLong(7, timestamp)
          bindDouble(8, distance_from_session_start_km)
        }
    notifyQueries(-897_213_312) { emit ->
      emit("elevation_samples")
    }
  }

  public fun markElevationSamplesSynced(ids: Collection<Long>) {
    val idsIndexes = createArguments(count = ids.size)
    driver.execute(null, """UPDATE elevation_samples SET is_synced = 1 WHERE id IN $idsIndexes""",
        ids.size) {
          ids.forEachIndexed { index, ids_ ->
            bindLong(index, ids_)
          }
        }
    notifyQueries(1_499_962_081) { emit ->
      emit("elevation_samples")
    }
  }

  private inner class GetStationByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("stations", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("stations", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_784_394_782,
        """SELECT stations.id, stations.name, stations.latitude, stations.longitude, stations.type, stations.scheduled_arrival, stations.scheduled_departure, stations.geofence_radius_meters FROM stations WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "tracking_database.sq:getStationById"
  }

  private inner class GetSessionByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("trip_sessions", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("trip_sessions", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(519_877_760,
        """SELECT trip_sessions.id, trip_sessions.start_time, trip_sessions.end_time, trip_sessions.route_id, trip_sessions.total_distance_km, trip_sessions.max_speed_kmh, trip_sessions.avg_speed_kmh FROM trip_sessions WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "tracking_database.sq:getSessionById"
  }

  private inner class GetEventsBySessionQuery<out T : Any>(
    public val sessionId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("station_events", "stations", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("station_events", "stations", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_011_273_758, """
    |SELECT se.id, se.session_id, se.station_id, se.event_type, se.actual_time, se.delay_minutes, s.name AS station_name, s.latitude AS station_lat,
    |       s.longitude AS station_lon, s.type AS station_type,
    |       s.geofence_radius_meters
    |FROM station_events se
    |JOIN stations s ON se.station_id = s.id
    |WHERE se.session_id = ?
    |ORDER BY se.actual_time ASC
    """.trimMargin(), mapper, 1) {
      bindString(0, sessionId)
    }

    override fun toString(): String = "tracking_database.sq:getEventsBySession"
  }

  private inner class GetRecentEventsQuery<out T : Any>(
    public val sessionId: String,
    public val limit: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("station_events", "stations", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("station_events", "stations", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(626_369_884, """
    |SELECT se.id, se.session_id, se.station_id, se.event_type, se.actual_time, se.delay_minutes, s.name AS station_name, s.latitude AS station_lat,
    |       s.longitude AS station_lon, s.type AS station_type,
    |       s.geofence_radius_meters
    |FROM station_events se
    |JOIN stations s ON se.station_id = s.id
    |WHERE se.session_id = ?
    |ORDER BY se.actual_time DESC
    |LIMIT ?
    """.trimMargin(), mapper, 2) {
      bindString(0, sessionId)
      bindLong(1, limit)
    }

    override fun toString(): String = "tracking_database.sq:getRecentEvents"
  }

  private inner class GetTrackPointsBySessionQuery<out T : Any>(
    public val sessionId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("track_points", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("track_points", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_058_976_807,
        """SELECT track_points.id, track_points.session_id, track_points.latitude, track_points.longitude, track_points.speed_kmh, track_points.accuracy_meters, track_points.timestamp, track_points.altitude, track_points.bearing_degrees, track_points.barometer_altitude, track_points.vertical_accuracy_meters FROM track_points WHERE session_id = ? ORDER BY timestamp ASC""",
        mapper, 1) {
      bindString(0, sessionId)
    }

    override fun toString(): String = "tracking_database.sq:getTrackPointsBySession"
  }

  private inner class CountTrackPointsQuery<out T : Any>(
    public val sessionId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("track_points", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("track_points", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-938_819_539,
        """SELECT COUNT(*) FROM track_points WHERE session_id = ?""", mapper, 1) {
      bindString(0, sessionId)
    }

    override fun toString(): String = "tracking_database.sq:countTrackPoints"
  }

  private inner class GetElevationSegmentQuery<out T : Any>(
    public val segmentId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("elevation_segments", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("elevation_segments", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(152_392_734,
        """SELECT elevation_segments.id, elevation_segments.from_station_id, elevation_segments.from_station_name, elevation_segments.to_station_id, elevation_segments.to_station_name, elevation_segments.total_distance_km, elevation_segments.min_elevation_meters, elevation_segments.max_elevation_meters, elevation_segments.contribution_count, elevation_segments.updated_at, elevation_segments.source, elevation_segments.is_manually_verified, elevation_segments.auto_collection_mode FROM elevation_segments WHERE id = ?""",
        mapper, 1) {
      bindString(0, segmentId)
    }

    override fun toString(): String = "tracking_database.sq:getElevationSegment"
  }

  private inner class GetElevationSegmentByStationsQuery<out T : Any>(
    public val fromStationId: String,
    public val toStationId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("elevation_segments", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("elevation_segments", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_375_695_348, """
    |SELECT elevation_segments.id, elevation_segments.from_station_id, elevation_segments.from_station_name, elevation_segments.to_station_id, elevation_segments.to_station_name, elevation_segments.total_distance_km, elevation_segments.min_elevation_meters, elevation_segments.max_elevation_meters, elevation_segments.contribution_count, elevation_segments.updated_at, elevation_segments.source, elevation_segments.is_manually_verified, elevation_segments.auto_collection_mode FROM elevation_segments
    |WHERE from_station_id = ? AND to_station_id = ?
    """.trimMargin(), mapper, 2) {
      bindString(0, fromStationId)
      bindString(1, toStationId)
    }

    override fun toString(): String = "tracking_database.sq:getElevationSegmentByStations"
  }

  private inner class GetElevationPointsBySegmentQuery<out T : Any>(
    public val segmentId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("elevation_points", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("elevation_points", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(504_960_804, """
    |SELECT elevation_points.id, elevation_points.segment_id, elevation_points.distance_from_start_km, elevation_points.elevation_meters, elevation_points.gradient_permille, elevation_points.contribution_count FROM elevation_points WHERE segment_id = ?
    |ORDER BY distance_from_start_km ASC
    """.trimMargin(), mapper, 1) {
      bindString(0, segmentId)
    }

    override fun toString(): String = "tracking_database.sq:getElevationPointsBySegment"
  }

  private inner class GetElevationSamplesBySessionQuery<out T : Any>(
    public val sessionId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("elevation_samples", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("elevation_samples", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-105_493_589,
        """SELECT elevation_samples.id, elevation_samples.session_id, elevation_samples.latitude, elevation_samples.longitude, elevation_samples.gps_altitude, elevation_samples.barometer_altitude, elevation_samples.horizontal_accuracy_meters, elevation_samples.vertical_accuracy_meters, elevation_samples.timestamp, elevation_samples.distance_from_session_start_km, elevation_samples.is_synced FROM elevation_samples WHERE session_id = ? ORDER BY timestamp ASC""",
        mapper, 1) {
      bindString(0, sessionId)
    }

    override fun toString(): String = "tracking_database.sq:getElevationSamplesBySession"
  }

  private inner class GetUnsyncedElevationSamplesQuery<out T : Any>(
    public val limit: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("elevation_samples", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("elevation_samples", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(647_860_071,
        """SELECT elevation_samples.id, elevation_samples.session_id, elevation_samples.latitude, elevation_samples.longitude, elevation_samples.gps_altitude, elevation_samples.barometer_altitude, elevation_samples.horizontal_accuracy_meters, elevation_samples.vertical_accuracy_meters, elevation_samples.timestamp, elevation_samples.distance_from_session_start_km, elevation_samples.is_synced FROM elevation_samples WHERE is_synced = 0 ORDER BY timestamp ASC LIMIT ?""",
        mapper, 1) {
      bindLong(0, limit)
    }

    override fun toString(): String = "tracking_database.sq:getUnsyncedElevationSamples"
  }
}
