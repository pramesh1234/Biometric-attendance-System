package com.example.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.attendance.core.designsystem.AttendanceTheme
import com.example.attendance.core.model.Role
import com.example.attendance.core.navigation.Destination
import com.example.attendance.feature.admin.AdminHomeScreen
import com.example.attendance.feature.admin.CalendarScreen
import com.example.attendance.feature.admin.EmployeeDetailsScreen
import com.example.attendance.feature.admin.EmployeeListScreen
import com.example.attendance.feature.admin.OfficeLocationScreen
import com.example.attendance.feature.admin.ReviewDetailsScreen
import com.example.attendance.feature.admin.ReviewListScreen
import com.example.attendance.feature.admin.SendNoticeScreen
import com.example.attendance.feature.admin.StaffEditorScreen
import com.example.attendance.feature.onboarding.ChangePasswordScreen
import com.example.attendance.feature.onboarding.LoginScreen
import com.example.attendance.feature.staff.AttendanceHistoryScreen
import com.example.attendance.feature.staff.HolidayListScreen
import com.example.attendance.feature.staff.NotificationsScreen
import com.example.attendance.feature.staff.StaffHomeScreen
import com.example.attendance.feature.staff.VerificationScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { AttendanceTheme { AttendanceApp() } }
    }
}

@Composable
private fun AttendanceApp(vm: SessionViewModel = hiltViewModel()) {
    val session by vm.session.collectAsStateWithLifecycle();
    val stack = rememberNavBackStack(Destination.Login)
    var navigationSession by rememberSaveable { mutableStateOf("uninitialized") }
    val sessionKey =
        session?.let { "${it.accountId}:${it.role}:${it.mustChangePassword}" } ?: "signed-out"
    LaunchedEffect(sessionKey) {
        // Preserve the restored back stack on rotation; reset it when access changes.
        // Persisted sessions restore the correct landing screen after process death,
        // then ValidateSessionUseCase clears stale access if the account changed.
        if (navigationSession != sessionKey) {
            stack.clear()
            stack.add(
                when {
                    session == null -> Destination.Login; session!!.mustChangePassword -> Destination.ChangePassword; session!!.role == Role.ADMIN -> Destination.AdminHome; else -> Destination.StaffHome
                }
            )
            navigationSession = sessionKey
        }
    }
    val back: () -> Unit = { if (stack.size > 1) stack.removeLastOrNull() }
    NavDisplay(
        backStack = stack,
        onBack = back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Destination.Login> { LoginScreen() }
            entry<Destination.ChangePassword> { ChangePasswordScreen(vm::signOut) }
            entry<Destination.AdminHome> {
                AdminHomeScreen(
                    { stack.add(Destination.Employees) },
                    { stack.add(Destination.SendNotice) },
                    { stack.add(Destination.Reviews) },
                    { stack.add(Destination.Calendar) },
                    { stack.add(Destination.OfficeLocation) },
                    vm::signOut
                )
            }
            entry<Destination.Employees> {
                EmployeeListScreen(
                    back,
                    { stack.add(Destination.EmployeeDetails(it)) },
                    { stack.add(Destination.StaffEditor()) })
            }
            entry<Destination.EmployeeDetails> { key ->
                EmployeeDetailsScreen(
                    key.employeeId,
                    back,
                    { stack.add(Destination.StaffEditor(key.employeeId)) })
            }
            entry<Destination.StaffEditor> { key -> StaffEditorScreen(key.employeeId, back) }
            entry<Destination.SendNotice> { SendNoticeScreen(back) }
            entry<Destination.Reviews> {
                ReviewListScreen(
                    back,
                    { stack.add(Destination.ReviewDetails(it)) })
            }
            entry<Destination.ReviewDetails> { key -> ReviewDetailsScreen(key.reviewId, back) }
            entry<Destination.Calendar> { CalendarScreen(back) }
            entry<Destination.OfficeLocation> { OfficeLocationScreen(back) }
            entry<Destination.StaffHome> {
                StaffHomeScreen(
                    { stack.add(Destination.Verification(it)) },
                    { stack.add(Destination.History) },
                    { stack.add(Destination.Holidays) },
                    { stack.add(Destination.Notifications) },
                    vm::signOut
                )
            }
            entry<Destination.Verification> { key -> VerificationScreen(key.checkOut, back) }
            entry<Destination.History> { AttendanceHistoryScreen(back) }
            entry<Destination.Holidays> { HolidayListScreen(back) }
            entry<Destination.Notifications> {
                NotificationsScreen(
                    back,
                    { stack.add(Destination.History) })
            }
        })
}
