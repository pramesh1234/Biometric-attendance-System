package com.example.attendance.core.common
import com.example.attendance.core.model.*
import com.example.attendance.core.testing.Fixture
import org.junit.Test
import org.junit.Assert.*
import java.time.*
import java.util.Locale
class FormattingTest {
 @Test fun unavailablePercentageAndTimesHaveClearPlaceholders(){assertEquals("N/A",percent(null));assertEquals("—",displayTime(null,ZoneOffset.UTC))}
 @Test fun timestampsUseCapturedTimezone(){val old=Locale.getDefault();try{Locale.setDefault(Locale.US);assertEquals("05:30 AM",displayTime(Instant.parse("2026-10-01T00:00:00Z"),ZoneId.of("Asia/Kolkata")));assertEquals("80.0%",percent(80.0))}finally{Locale.setDefault(old)}}
 @Test fun dailyHistoryDistinguishesAllAttendanceStates(){
  val date=LocalDate.of(2026,10,1);val time=Instant.parse("2026-10-01T00:00:00Z")
  val days=listOf(AttendanceDay(date,DayType.WORKING,time,time,false),AttendanceDay(date.plusDays(1),DayType.WORKING,time,null,false),AttendanceDay(date.plusDays(2),DayType.WEEKLY_OFF,null,null,false),AttendanceDay(date.plusDays(3),DayType.HOLIDAY,null,null,false),AttendanceDay(date.plusDays(4),DayType.WORKING,null,null,true),AttendanceDay(date.plusDays(5),DayType.WORKING,null,null,false))
  val ui=AttendanceSummary(25.0,1,4,days).ui(YearMonth.of(2026,10))
  assertEquals(6,ui.days.size)
  val rendered=ui.toString();listOf("Present","Incomplete","Weekly off","Holiday","Pending","Not marked").forEach{assertTrue(rendered.contains(it))}
 }
 @Test fun initialsUseFirstTwoNames(){assertEquals("AS",Fixture.seed().employees.first().initials())}
}
