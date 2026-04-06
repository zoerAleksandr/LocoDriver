package com.z_company.tracking.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.z_company.tracking.db.tracking.newInstance
import com.z_company.tracking.db.tracking.schema
import com.zcompany.tracking.db.Tracking_databaseQueries
import kotlin.Unit

public interface TrackingDatabase : Transacter {
  public val tracking_databaseQueries: Tracking_databaseQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = TrackingDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): TrackingDatabase =
        TrackingDatabase::class.newInstance(driver)
  }
}
