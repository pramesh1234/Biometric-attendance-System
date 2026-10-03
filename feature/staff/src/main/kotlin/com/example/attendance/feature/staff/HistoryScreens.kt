package com.example.attendance.feature.staff
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attendance.core.common.*
import com.example.attendance.core.designsystem.*
import com.example.attendance.core.model.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun AttendanceHistoryScreen(onBack:()->Unit,vm:StaffViewModel=hiltViewModel()){
    val data by vm.data.collectAsStateWithLifecycle();val zone by vm.zone.collectAsStateWithLifecycle();var monthText by rememberSaveable{mutableStateOf(YearMonth.now(zone).toString())};val month=YearMonth.parse(monthText)
    Screen("Attendance history",onBack){StaffFeedback(vm);data?.let{state->val flow=remember(state.profile.employee.id,month){vm.history(state.profile.employee.id,month)};val summary by flow.collectAsStateWithLifecycle(null)
        summary?.let{AttendanceContent(it.ui(month),{monthText=month.minusMonths(1).toString()},{monthText=month.plusMonths(1).toString()},month>YearMonth.from(state.profile.employee.joiningDate),month<YearMonth.now(zone))}?:CircularProgressIndicator()
        Caption("Both Check In and Check Out are required. Today enters the percentage after Check Out or when the date ends. N/A means no eligible completed workdays yet.")
    }}
}
@Composable fun HolidayListScreen(onBack:()->Unit,vm:StaffViewModel=hiltViewModel()){
    val holidays by vm.holidays.collectAsStateWithLifecycle();val zone by vm.zone.collectAsStateWithLifecycle();var year by rememberSaveable{mutableIntStateOf(LocalDate.now(zone).year)}
    Screen("Holidays",onBack){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){OutlinedButton({year--}){Text("Previous year")};Text(year.toString());OutlinedButton({year++}){Text("Next year")}};StaffFeedback(vm)
        val rows=holidays.filter{it.date.year==year};if(rows.isEmpty())Panel{SectionTitle("No holidays listed for $year");Caption("Your admin can add company holidays to the working calendar.")}
        rows.forEach{h->Panel{SectionTitle(h.name);Caption(h.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));if(h.date>=LocalDate.now(zone))Badge("Upcoming")}}
    }
}
@Composable fun NotificationsScreen(onBack:()->Unit,onHistory:()->Unit,vm:StaffViewModel=hiltViewModel()){
    val items by vm.notifications.collectAsStateWithLifecycle();val zone by vm.zone.collectAsStateWithLifecycle();var filter by rememberSaveable{mutableStateOf("All")}
    Screen("Notifications",onBack){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("All","Notices","Reviews").forEach{value->FilterChip(filter==value,{filter=value},{Text(value)})}};StaffFeedback(vm)
        val rows=items.filter{filter=="All"||(filter=="Notices")== (it.type==NotificationType.NOTICE)}
        if(rows.isEmpty())Panel{SectionTitle("You’re all caught up");Caption("Notices and attendance review updates will appear here.")}
        rows.forEach{item->Panel{Badge(item.type.name.lowercase().replace('_',' '),item.type==NotificationType.REVIEW_REJECTED);SectionTitle(item.title);Text(item.body);Caption(DateTimeFormatter.ofPattern("d MMM · hh:mm a").withZone(zone).format(item.createdAt));if(!item.read)TextButton({vm.markRead(item.id)}){Text("Mark as read")};if(item.type!=NotificationType.NOTICE)TextButton({vm.markRead(item.id);onHistory()}){Text("View attendance")}}}
    }
}
