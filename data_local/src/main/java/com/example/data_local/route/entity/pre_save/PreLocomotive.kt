package com.example.data_local.route.entity.pre_save

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data_local.route.entity.SectionDiesel
import com.example.data_local.route.entity.SectionElectric
import com.example.data_local.route.type_converters.SectionDieselToPrimitiveConverters
import com.example.data_local.route.type_converters.SectionElectricToPrimitiveConverter
import com.example.data_local.route.type_converters.TypeLocoConverter
import com.example.domain.entities.route.LocoType
import org.jetbrains.annotations.NotNull

@Entity
@TypeConverters(
    TypeLocoConverter::class,
    SectionElectricToPrimitiveConverter::class,
    SectionDieselToPrimitiveConverters::class
)
internal data class PreLocomotive(
    @NotNull
    @PrimaryKey
    val locoId: String,
    var series: String? = null,
    var number: String? = null,
    var type: LocoType = LocoType.ELECTRIC,
    var electricSectionList: List<SectionElectric> = emptyList(),
    var dieselSectionList: List<SectionDiesel> = emptyList(),
    var timeStartOfAcceptance: Long? = null,
    var timeEndOfAcceptance: Long? = null,
    var timeStartOfDelivery: Long? = null,
    var timeEndOfDelivery: Long? = null
)
