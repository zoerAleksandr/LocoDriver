package com.example.data_local.route.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data_local.route.type_converters.PhotosConverter
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = BasicData::class,
            parentColumns = ["id"],
            childColumns = ["baseId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(PhotosConverter::class)
internal data class Notes(
    @PrimaryKey
    var notesId: String = UUID.randomUUID().toString(),
    @ColumnInfo(index = true)
    var baseId: String,
    var text: String? = null,
    var photos: MutableList<String> = mutableListOf()
)
