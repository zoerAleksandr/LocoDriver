package com.example.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = BasicData::class,
            parentColumns = ["id"],
            childColumns = ["basicId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
internal data class Photo(
    @PrimaryKey
    var photoId: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true)
    var basicId: String,
    var urlPhoto: String = ""
)
