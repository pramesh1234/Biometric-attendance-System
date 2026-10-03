package com.example.attendance.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.attendance.core.common.initials
import com.example.attendance.core.designsystem.Avatar
import com.example.attendance.core.designsystem.Badge
import com.example.attendance.core.designsystem.Caption
import com.example.attendance.core.designsystem.Field
import com.example.attendance.core.designsystem.Panel
import com.example.attendance.core.designsystem.PrimaryButton
import com.example.attendance.core.designsystem.Screen
import com.example.attendance.core.designsystem.SectionTitle
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.PunchAction
import com.example.attendance.core.model.ReviewStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendNoticeScreen(onBack: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val data by vm.data.collectAsStateWithLifecycle();
    val busy by vm.busy.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") };
    var gender by rememberSaveable { mutableStateOf("Everyone") };
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) };
    var title by rememberSaveable { mutableStateOf("") };
    var body by rememberSaveable { mutableStateOf("") };
    var sent by rememberSaveable { mutableStateOf(false) }
    val matches = data?.staff.orEmpty().filter {
        (gender == "Everyone" || it.employee.gender == gender) && "${it.employee.name} ${it.employee.code}".contains(
            query,
            true
        )
    }
    Screen("Send notice", onBack) {
        SectionTitle("Recipients"); Field(
        "Search name or employee ID",
        query,
        { query = it }); Caption("Filter by gender")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Everyone",
                "Female",
                "Male",
                "Non-binary",
                "Prefer not to say"
            ).forEach { value -> FilterChip(gender == value, { gender = value }, { Text(value) }) }
        }
        val allSelected = matches.isNotEmpty() && matches.all { it.employee.id in selected }
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = allSelected,
                    enabled = !busy && matches.isNotEmpty(),
                    role = Role.Checkbox,
                    onValueChange = { checked ->
                        selected =
                            if (checked) (selected + matches.map { it.employee.id }).distinct() else selected - matches.map { it.employee.id }
                                .toSet()
                    }), verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(allSelected, onCheckedChange = null); Text("Select all matching employees")
        }
        if (matches.isEmpty()) Caption("No matching employees.")
        matches.forEach { p ->
            val checked = p.employee.id in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        enabled = !busy,
                        role = Role.Checkbox,
                        onValueChange = { value ->
                            selected =
                                if (value) selected + p.employee.id else selected - p.employee.id
                        }), verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked,
                    onCheckedChange = null
                ); Column { Text(p.employee.name); Caption(p.employee.code) }
            }
        }
        Badge("${selected.size} selected"); Field(
        "Notice title",
        title,
        { title = it }); OutlinedTextField(
        body,
        { body = it },
        Modifier.fillMaxWidth(),
        minLines = 4,
        label = { Text("Message") })
        AdminFeedback(vm); PrimaryButton(
        if (busy) "Sending…" else "Send notice",
        { vm.send(selected.toSet(), title, body) { sent = true } },
        !busy && selected.isNotEmpty() && title.isNotBlank() && body.isNotBlank()
    )
    }
    if (sent) AlertDialog(
        onDismissRequest = { sent = false; onBack() },
        title = { Text("Notice sent") },
        text = { Text("Sent to ${selected.size} employee(s). It is now available in their local inbox.") },
        confirmButton = { TextButton({ sent = false; onBack() }) { Text("Done") } })
}

@Composable
fun ReviewListScreen(
    onBack: () -> Unit,
    onReview: (String) -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    val data by vm.data.collectAsStateWithLifecycle();
    var reviewed by rememberSaveable { mutableStateOf(false) }
    val all = data?.reviews.orEmpty();
    val rows = all.filter { (it.request.status != ReviewStatus.PENDING) == reviewed }
    Screen("Review requests", onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!reviewed,
                { reviewed = false },
                { Text("Pending · ${all.count { it.request.status == ReviewStatus.PENDING }}") }); FilterChip(
            reviewed,
            { reviewed = true },
            { Text("Reviewed") })
        }
        AdminFeedback(vm); if (rows.isEmpty()) Panel {
        SectionTitle(if (reviewed) "No reviewed requests" else "All caught up"); Caption(
        "Attendance review requests will appear here."
    )
    }
        rows.forEach { r ->
            Card(
                onClick = { onReview(r.request.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(r.employee.initials()); Column(
                    verticalArrangement = Arrangement.spacedBy(
                        8.dp
                    )
                ) {
                    SectionTitle(
                        r.employee.name
                    ); Caption("${r.attempt.attendanceDate} · ${if (r.attempt.action == PunchAction.CHECK_IN) "Check In" else "Check Out"}"); Badge(
                    "${r.attempt.confidenceBps / 100.0}% · ${r.request.status.name.lowercase()}",
                    r.request.status == ReviewStatus.PENDING
                )
                }
                }
            }
        }
    }
}

@Composable
fun ReviewDetailsScreen(
    reviewId: String,
    onBack: () -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    val data by vm.data.collectAsStateWithLifecycle();
    val busy by vm.busy.collectAsStateWithLifecycle();
    val review = data?.reviews?.firstOrNull { it.request.id == reviewId };
    var note by rememberSaveable { mutableStateOf("") }
    Screen("Review attendance", onBack) {
        AdminFeedback(vm)
        review?.let { r ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Avatar(r.employee.initials()); Column { SectionTitle(r.employee.name); Caption("${r.employee.code} · ${if (r.attempt.action == PunchAction.CHECK_IN) "Check In" else "Check Out"}") } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    AsyncImage(
                        vm.path(r.attempt.imageKey),
                        "Captured image",
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ); Caption("Captured image")
                }; Column(Modifier.weight(1f)) {
                AsyncImage(
                    vm.path(r.referenceKey),
                    "Reference image",
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ); Caption("Reference image")
            }
            }
            Panel(tinted = true) {
                SectionTitle("${r.attempt.confidenceBps / 100.0}% face-match score"); Caption(
                "${r.request.status.name.lowercase()} · ${r.attempt.verifierVersion}"
            )
            }
            SectionTitle("Capture details"); Text(
            DateTimeFormatter.ofPattern("d MMM yyyy · hh:mm a")
                .withZone(ZoneId.of(r.attempt.timeZoneId)).format(r.attempt.capturedAt)
        ); Text(r.attempt.location.label); Caption("Accuracy ±${r.attempt.location.accuracyM.toInt()} m · ${r.attempt.timeZoneId}")
            if (r.request.status == ReviewStatus.PENDING) {
                OutlinedTextField(
                    note,
                    { note = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Decision note (optional)") },
                    minLines = 2
                ); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton({
                        vm.decide(
                            reviewId,
                            false,
                            note
                        )
                    }, Modifier.weight(1f), enabled = !busy) {
                        Text(
                            "Reject",
                            color = MaterialTheme.colorScheme.error
                        )
                    }; Button(
                    { vm.decide(reviewId, true, note) },
                    Modifier.weight(1f),
                    enabled = !busy
                ) { Text("Approve") }
                }
            } else {
                Badge(r.request.status.name.lowercase()); if (r.request.note.isNotBlank()) Text(r.request.note); Caption(
                    "The employee has been notified in their inbox."
                )
            }
        } ?: Caption("Loading review…")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val data by vm.data.collectAsStateWithLifecycle();
    val currentZone by vm.zone.collectAsStateWithLifecycle();
    val busy by vm.busy.collectAsStateWithLifecycle()
    var date by rememberSaveable { mutableStateOf(LocalDate.now(currentZone).toString()) };
    var type by rememberSaveable { mutableStateOf(DayType.HOLIDAY) };
    var name by rememberSaveable { mutableStateOf("") };
    var zone by rememberSaveable { mutableStateOf(currentZone.id) };
    var year by rememberSaveable { mutableStateOf(LocalDate.now(currentZone).year.toString()) }
    Screen("Working calendar", onBack) {
        Caption("Monday–Friday are normal working days. Add holidays or working-day exceptions below.")
        Field(
            "Date · YYYY-MM-DD",
            date,
            { date = it }); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayType.entries.forEach { value ->
            FilterChip(
                type == value,
                { type = value },
                { Text(value.name.lowercase().replace('_', ' ')) })
        }
    }
        if (type == DayType.HOLIDAY) Field("Holiday name", name, { name = it })
        Caption("Changing past dates recalculates historical attendance percentages.")
        PrimaryButton("Save calendar day", { vm.calendar(date, type, name) }, !busy); AdminFeedback(
        vm
    )
        SectionTitle("Office time zone"); Field(
        "IANA time zone",
        zone,
        { zone = it }); OutlinedButton(
        { vm.changeZone(zone) },
        enabled = !busy
    ) { Text("Update time zone") }
        SectionTitle("Generate a year"); Field(
        "Year",
        year,
        {
            year = it
        }); Caption("Fills missing dates only; existing holidays and overrides are preserved."); OutlinedButton(
        { vm.generate(year) },
        enabled = !busy
    ) { Text("Generate calendar") }
        SectionTitle("Holidays"); data?.holidays.orEmpty().forEach { h ->
        ListItem(
            headlineContent = { Text(h.name) },
            supportingContent = { Caption(h.date.toString()) })
    }
    }
}
