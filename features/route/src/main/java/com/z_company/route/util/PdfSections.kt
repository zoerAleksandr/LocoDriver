package com.z_company.route.util

data class PdfSections(
    val includeRouteDetails: Boolean = true,
    val includeSchedule: Boolean = true,
    val includeSalary: Boolean = true
)
