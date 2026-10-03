package com.example.attendance.feature.staff

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.InsertChartOutlined
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attendance.core.common.displayTime
import com.example.attendance.core.common.initials
import com.example.attendance.core.common.percent
import com.example.attendance.core.designsystem.Avatar
import com.example.attendance.core.designsystem.Caption
import com.example.attendance.core.designsystem.NavigationCard
import com.example.attendance.core.designsystem.Panel
import com.example.attendance.core.designsystem.Screen
import com.example.attendance.core.designsystem.SectionTitle
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun StaffFeedback(vm: StaffViewModel) {
    val error by vm.error.collectAsStateWithLifecycle();
    val busy by vm.busy.collectAsStateWithLifecycle(); if (busy) LinearProgressIndicator(Modifier.fillMaxWidth()); error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun StaffHomeScreen(
    onCapture: (Boolean) -> Unit,
    onHistory: () -> Unit,
    onHolidays: () -> Unit,
    onNotifications: () -> Unit,
    onSignOut: () -> Unit,
    vm: StaffViewModel = hiltViewModel()
) {
    val data by vm.data.collectAsStateWithLifecycle();
    val zone by vm.zone.collectAsStateWithLifecycle();
    val location by vm.location.collectAsStateWithLifecycle();
    val context = LocalContext.current
    var now by remember { mutableStateOf(ZonedDateTime.now(zone)) }; LaunchedEffect(zone) {
        while (true) {
            now = ZonedDateTime.now(zone); delay(1000)
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants -> if (grants.values.any { it }) vm.refreshLocation() }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) vm.refreshLocation()
    }
    Screen(
        "Attendance",
        actions = {
            IconButton(onNotifications) {
                BadgedBox(badge = {
                    if ((data?.unreadCount
                            ?: 0) > 0
                    ) androidx.compose.material3.Badge { Text(data!!.unreadCount.toString()) }
                }) { Icon(Icons.Outlined.NotificationsNone, "Notifications") }
            }
        }) {
        data?.let { state ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(state.profile.employee.initials()); Column {
                Caption(if (now.hour < 12) "Good morning," else if (now.hour < 17) "Good afternoon," else "Good evening,"); Text(
                state.profile.employee.name.substringBefore(' '),
                style = MaterialTheme.typography.titleLarge
            )
            }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Caption(
                    now.format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                    ).uppercase()
                ); Text(
                now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")),
                style = MaterialTheme.typography.displaySmall
            ); Caption(location?.label ?: "Location not available"); TextButton({
                permission.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }) { Text(if (location == null) "Enable / refresh location" else "Refresh location") }
            }
            val complete = state.checkOut != null;
            val checked = state.checkIn != null
            Panel(tinted = true) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.example.attendance.core.designsystem.Badge(
                        when {
                            state.pending != null -> "Awaiting admin review"; complete -> "Day complete"; checked -> "Checked in at ${
                            displayTime(
                                state.checkIn,
                                ZoneId.of(state.record!!.timeZoneId)
                            )
                        }"; else -> "Ready for your day"
                        }, state.pending != null
                    )
                    when {
                        state.pending != null -> {
                            Icon(Icons.Outlined.Schedule, null, Modifier.size(44.dp)); SectionTitle(
                                "Attendance under review"
                            ); Caption("We’ll notify you when your admin decides."); TextButton(
                                onNotifications
                            ) { Text("View notifications") }
                        }

                        complete -> {
                            Icon(
                                Icons.Outlined.CheckCircleOutline,
                                null,
                                Modifier.size(44.dp)
                            ); Text(
                                "See you tomorrow",
                                style = MaterialTheme.typography.headlineSmall
                            ); Caption(
                                "In ${displayTime(state.checkIn, zone)} · Out ${
                                    displayTime(
                                        state.checkOut,
                                        zone
                                    )
                                }"
                            )
                        }

                        else -> {
                            Button(
                                { onCapture(checked) },
                                Modifier.size(156.dp),
                                shape = CircleShape
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Face,
                                        null,
                                        Modifier.size(36.dp)
                                    ); Text(
                                    if (checked) "Check Out" else "Check In",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                }
                            }; Caption(if (checked) "Ready to leave? Verify with a quick photo." else "Verify with a quick photo")
                        }
                    }
                    if (checked && !complete) {
                        HorizontalDivider(); Caption(
                            "Check In: ${state.record?.date} · ${
                                displayTime(
                                    state.checkIn,
                                    ZoneId.of(state.record!!.timeZoneId)
                                )
                            }"
                        ); Caption("Check Out: not recorded")
                    }
                }
            }
            NavigationCard(
                "${percent(state.month.percentage)} attendance",
                "This month · ${now.format(DateTimeFormatter.ofPattern("MMMM"))}",
                Icons.Outlined.InsertChartOutlined,
                onHistory
            )
            NavigationCard(
                state.nextHoliday?.name ?: "No upcoming holidays",
                state.nextHoliday?.date?.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
                    ?: "View the company calendar",
                Icons.Outlined.CalendarMonth,
                onHolidays
            )
        } ?: CircularProgressIndicator()
        StaffFeedback(vm); TextButton(
        onSignOut,
        Modifier.align(Alignment.CenterHorizontally)
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Logout,
            null
        ); Spacer(Modifier.width(8.dp)); Text("Sign out")
    }
    }
}
