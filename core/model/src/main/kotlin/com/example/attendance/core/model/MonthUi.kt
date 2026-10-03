package com.example.attendance.core.model

data class MonthUi(
    val label: String,
    val percentage: String,
    val eligibleLabel: String,
    val weeks: List<Int>,
    val days: List<DayUi>
)
