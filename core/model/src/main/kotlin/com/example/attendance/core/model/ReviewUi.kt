package com.example.attendance.core.model

data class ReviewUi(
    val id: String,
    val employee: EmployeeUi,
    val action: String,
    val time: String,
    val confidence: Int
)
