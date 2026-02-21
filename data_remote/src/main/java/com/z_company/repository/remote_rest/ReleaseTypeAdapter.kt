package com.z_company.repository.remote_rest

// Gson TypeAdapter для ReleaseType удалён.
// Теперь ReleaseType сериализуется через @Serializable(with = ReleaseTypeSerializer::class)
// в domain/entities/MonthOfYear.kt.
// Для Room (data_local) своя реализация в data_local/setting/type_converter/MonthOfYearToPrimitiveConverter.kt.
