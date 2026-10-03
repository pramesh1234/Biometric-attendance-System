package com.example.attendance.core.navigation
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface Destination : NavKey {
    @Serializable data object Login : Destination
    @Serializable data object ChangePassword : Destination
    @Serializable data object Calendar : Destination
    @Serializable data object OfficeLocation : Destination
    @Serializable data object AdminHome : Destination
    @Serializable data object Employees : Destination
    @Serializable data class EmployeeDetails(val employeeId: String) : Destination
    @Serializable data class StaffEditor(val employeeId: String? = null) : Destination
    @Serializable data object SendNotice : Destination
    @Serializable data object Reviews : Destination
    @Serializable data class ReviewDetails(val reviewId: String) : Destination
    @Serializable data object StaffHome : Destination
    @Serializable data class Verification(val checkOut: Boolean) : Destination
    @Serializable data object History : Destination
    @Serializable data object Holidays : Destination
    @Serializable data object Notifications : Destination
}
