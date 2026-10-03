package com.example.attendance.core.common

import com.example.attendance.core.model.AttendanceSummary
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.DayUi
import com.example.attendance.core.model.Employee
import com.example.attendance.core.model.MonthUi
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun percent(value: Double?) =
    value?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "N/A"

fun displayTime(value: Instant?, zone: ZoneId) =
    value?.let { DateTimeFormatter.ofPattern("hh:mm a").withZone(zone).format(it) } ?: "—"

fun Employee.initials() = name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
fun AttendanceSummary.ui(month: YearMonth): MonthUi {
    val weeks = days.filter { it.type == DayType.WORKING }.groupBy { (it.date.dayOfMonth - 1) / 7 }
        .toSortedMap().values.map { rows -> rows.count { it.checkIn != null && it.checkOut != null } * 100 / rows.size }
    return MonthUi(
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
        percent(percentage),
        "$attended of $eligible eligible workdays",
        weeks,
        days.map { day ->
            DayUi(
                day.date.format(DateTimeFormatter.ofPattern("d MMM · EEEE")),
                displayTime(day.checkIn, ZoneId.of(day.timeZoneId)),
                displayTime(day.checkOut, ZoneId.of(day.timeZoneId)),
                when {
                    day.pending -> "Pending"; day.checkIn != null && day.checkOut != null -> "Present"; day.checkIn != null -> "Incomplete"; day.type == DayType.HOLIDAY -> "Holiday"; day.type == DayType.WEEKLY_OFF -> "Weekly off"; else -> "Not marked"
                }
            )
        })
}
