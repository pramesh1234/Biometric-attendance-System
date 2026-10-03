package com.example.attendance.core.model

// Presentation-only values used by the shared attendance visualization.
data class EmployeeUi(
    val id: String,
    val name: String,
    val jobTitle: String,
    val gender: String,
    val email: String,
    val joiningDate: String,
    val blocked: Boolean = false
) {
    val initials: String get() = name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
}
