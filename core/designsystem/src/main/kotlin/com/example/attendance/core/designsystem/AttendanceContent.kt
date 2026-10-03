package com.example.attendance.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.attendance.core.model.MonthUi

@Composable
fun AttendanceContent(
    month: MonthUi,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onPrevious,
            enabled = hasPrevious
        ) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month") }
        Text(month.label, style = MaterialTheme.typography.titleMedium)
        IconButton(onNext, enabled = hasNext) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                "Next month"
            )
        }
    }
    Panel(tinted = true) {
        Caption("MONTHLY ATTENDANCE")
        Text(month.percentage, style = MaterialTheme.typography.displaySmall)
        Caption(month.eligibleLabel)
        Caption("Attendance by week (%)")
        Row(
            Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            month.weeks.forEachIndexed { index, value ->
                Column(
                    Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Week ${index + 1}: $value percent" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("$value%", style = MaterialTheme.typography.labelSmall)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((value * .8f).coerceAtLeast(2f).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Caption("W${index + 1}")
                }
            }
        }
    }
    SectionTitle("Daily attendance")
    month.days.forEach { day ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(day.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Badge(day.status, day.status in listOf("Absent", "Pending"))
            }
            Caption("In ${day.checkIn}  ·  Out ${day.checkOut}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
