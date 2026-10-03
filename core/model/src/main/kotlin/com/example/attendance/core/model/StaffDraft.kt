package com.example.attendance.core.model

import java.time.LocalDate

data class StaffDraft(
    val id: String? = null,
    val code: String,
    val name: String,
    val email: String,
    val phone: String,
    val jobTitle: String,
    val gender: String,
    val joiningDate: LocalDate,
    val endDate: LocalDate? = null,
    val username: String,
    val temporaryPassword: String = "",
    val newPhotoKey: String? = null
)
