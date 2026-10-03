package com.example.attendance.feature.admin
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attendance.core.common.*
import com.example.attendance.core.designsystem.*
import com.example.attendance.core.model.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable internal fun AdminFeedback(vm: AdminViewModel) {
    val error by vm.error.collectAsStateWithLifecycle(); val message by vm.message.collectAsStateWithLifecycle(); val busy by vm.busy.collectAsStateWithLifecycle()
    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    message?.let { Panel(tinted = true) { Text(it) } }
}
@Composable fun AdminHomeScreen(onEmployees: () -> Unit, onNotice: () -> Unit, onReviews: () -> Unit, onCalendar: () -> Unit, onOfficeLocation: () -> Unit, onSignOut: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val data by vm.data.collectAsStateWithLifecycle(); val zone by vm.zone.collectAsStateWithLifecycle(); val office by vm.officeLocation.collectAsStateWithLifecycle()
    Screen("Workspace", actions = { IconButton(onOfficeLocation) { Icon(Icons.Outlined.LocationOn,"Office attendance location") }; IconButton(onCalendar) { Icon(Icons.Outlined.CalendarMonth,"Working calendar") }; IconButton(onSignOut) { Icon(Icons.AutoMirrored.Outlined.Logout,"Sign out") } }) {
        Caption("ADMIN OVERVIEW"); Text("Welcome back", style = MaterialTheme.typography.headlineLarge)
        Caption(LocalDate.now(zone).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")))
        AdminFeedback(vm)
        if (office == null) Panel(tinted = true) { SectionTitle("Set office attendance location"); Caption("Staff attendance is blocked until a latitude and longitude are configured."); PrimaryButton("Set location", onOfficeLocation) }
        data?.let { state ->
            Panel(tinted = true) { SectionTitle("Today at a glance"); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) { Text("${state.checkedIn} / ${state.staff.size}",style = MaterialTheme.typography.headlineLarge); Caption("Checked in") }
                Column(Modifier.weight(1f)) { Text(state.reviews.count { it.request.status == ReviewStatus.PENDING }.toString(),style = MaterialTheme.typography.headlineLarge); Caption("Awaiting review") }
            } }
            NavigationCard("Employees & attendance","${state.staff.size} staff · directory and history",Icons.Outlined.PeopleOutline,onEmployees)
            NavigationCard("Notices & messages","Send an update to your team",Icons.AutoMirrored.Outlined.Send,onNotice)
            NavigationCard("Review requests","Review captured attendance",Icons.AutoMirrored.Outlined.FactCheck,onReviews)
            NavigationCard("Office location",office?.let{"${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)} · 100 m radius"} ?: "Set attendance boundary",Icons.Outlined.LocationOn,onOfficeLocation)
        } ?: CircularProgressIndicator()
    }
}

@Composable fun OfficeLocationScreen(onBack: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val office by vm.officeLocation.collectAsStateWithLifecycle(); val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun hasLocationPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    var allowed by remember { mutableStateOf(hasLocationPermission()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { allowed = hasLocationPermission(); if (allowed) vm.updateOfficeLocationFromCurrent() }
    Screen("Office location", onBack) {
        AdminFeedback(vm)
        Panel(tinted = true) {
            Icon(Icons.Outlined.LocationOn, null, Modifier.size(40.dp))
            SectionTitle("Attendance geofence")
            Caption("Staff must be within 100 meters of the admin's saved current location before Check In or Check Out can be recorded.")
        }
        PrimaryButton(if (office == null) "Use current location" else "Update from current location", { if (allowed) vm.updateOfficeLocationFromCurrent() else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, !busy)
        office?.let { Caption("Current: ${"%.6f".format(it.latitude)}, ${"%.6f".format(it.longitude)}"); Caption("Address: ${it.address ?: "Address unavailable"}") } ?: Caption("No office location configured yet.")
    }
}
@Composable fun EmployeeListScreen(onBack: () -> Unit, onEmployee: (String) -> Unit, onAdd: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val data by vm.data.collectAsStateWithLifecycle(); var query by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("All") }
    val people = data?.staff.orEmpty().filter { (query.isBlank() || "${it.employee.name} ${it.employee.code}".contains(query,true)) && (filter == "All" || (filter == "Blocked") == (it.status == AccountStatus.BLOCKED)) }
    Screen("Employees",onBack,actions = { TextButton(onAdd) { Icon(Icons.Outlined.Add,null); Text("Add staff") } }) {
        Field("Search name or employee ID",query,{query=it}); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All","Active","Blocked").forEach { label -> FilterChip(filter==label,{filter=label},{Text(label)}) } }
        AdminFeedback(vm); Caption("${people.size} employees")
        if(people.isEmpty()) Panel { SectionTitle("No employees found"); Caption("Add your first employee or change the search.") }
        people.forEach { p -> ListItem(headlineContent = {Text(p.employee.name)},supportingContent = {Caption("${p.employee.code} · ${p.employee.jobTitle}")}, leadingContent = {Avatar(p.employee.initials())},trailingContent = {if(p.status==AccountStatus.BLOCKED) Badge("Blocked",true)},modifier=Modifier.clickable {onEmployee(p.employee.id)}, colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surface)); HorizontalDivider() }
    }
}
@Composable fun EmployeeDetailsScreen(employeeId: String, onBack: () -> Unit, onEdit: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val data by vm.data.collectAsStateWithLifecycle(); val zone by vm.zone.collectAsStateWithLifecycle(); val busy by vm.busy.collectAsStateWithLifecycle()
    val profile = data?.staff?.firstOrNull { it.employee.id == employeeId }
    var menu by remember { mutableStateOf(false) }; var action by rememberSaveable {mutableStateOf<String?>(null)}; var password by remember {mutableStateOf("")}
    var monthText by rememberSaveable {mutableStateOf(YearMonth.now(zone).toString())}; val month=YearMonth.parse(monthText)
    val monthFlow=remember(employeeId,month){vm.history(employeeId,month)}; val summary by monthFlow.collectAsStateWithLifecycle(null)
    val overallFlow=remember(employeeId){vm.history(employeeId,null)}; val overall by overallFlow.collectAsStateWithLifecycle(null)
    Screen("Employee details",onBack,actions={Box { TextButton({menu=true},enabled=profile!=null&&!busy){Text("Manage")}; DropdownMenu(menu,{menu=false}){
        DropdownMenuItem({Text("Edit staff details")},{menu=false;onEdit()}); DropdownMenuItem({Text("Reset password")},{menu=false;action="reset"})
        DropdownMenuItem({Text(if(profile?.status==AccountStatus.BLOCKED)"Unblock Employee" else "Block Employee")},{menu=false;action=if(profile?.status==AccountStatus.BLOCKED)"unblock" else "block"})
        DropdownMenuItem({Text("Delete Employee",color=MaterialTheme.colorScheme.error)},{menu=false;action="delete"})
    } }}) {
        AdminFeedback(vm)
        profile?.let { p ->
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Avatar(p.employee.initials());Column{Text(p.employee.name,style=MaterialTheme.typography.titleLarge);Caption("${p.employee.code} · ${p.employee.jobTitle}")}}
            Caption("Joined ${p.employee.joiningDate}"); Badge(p.status.name.lowercase().replaceFirstChar(Char::uppercase)); Caption(p.employee.email)
            Panel { Caption("Overall attendance since joining"); Text(percent(overall?.percentage),style=MaterialTheme.typography.displaySmall);Caption("${overall?.attended ?: 0} of ${overall?.eligible ?: 0} eligible workdays") }
            summary?.let{ AttendanceContent(it.ui(month),{monthText=month.minusMonths(1).toString()},{monthText=month.plusMonths(1).toString()},month>YearMonth.from(p.employee.joiningDate),month<YearMonth.now(zone)) }
        } ?: Caption("Employee unavailable or loading.")
    }
    profile?.let { p ->
        if(action in listOf("block","unblock","delete")) {
            val deleting=action=="delete"; val verb=if(deleting)"Delete" else if(action=="block")"Block" else "Unblock"
            Confirmation("$verb ${p.employee.name}?",when(action){"delete"->"This employee will be removed from active staff and their account disabled. Attendance and historical records remain available.";"block"->"This employee will no longer be able to sign in or mark attendance. You can unblock them later.";else->"This employee will be able to sign in and mark attendance again."},"$verb Employee",{action=null},{val block=action=="block";action=null;vm.access(employeeId,block,deleting){if(deleting)onBack()}},deleting)
        }
        if(action=="reset") AlertDialog(onDismissRequest={action=null},title={Text("Reset password")},text={Column{OutlinedTextField(password,{password=it},label={Text("Temporary password")},visualTransformation=PasswordVisualTransformation());Caption("At least 10 characters. The employee must change it at next login.");AdminFeedback(vm)}},confirmButton={TextButton({vm.resetPassword(employeeId,password){password="";action=null}},enabled=!busy&&password.length>=10){Text("Reset password")}},dismissButton={TextButton({action=null}){Text("Cancel")}})
    }
}
