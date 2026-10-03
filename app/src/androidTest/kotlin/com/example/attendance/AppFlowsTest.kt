package com.example.attendance
import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.example.attendance.core.testing.*
import com.example.attendance.core.testing.Fixture.Companion.PASSWORD
import com.example.attendance.core.model.*
import com.example.attendance.di.AppModule
import dagger.hilt.android.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*

@HiltAndroidTest
@UninstallModules(AppModule::class)
@RunWith(AndroidJUnit4::class)
class AppFlowsTest {
    @get:Rule(order=0) val hilt=HiltAndroidRule(this)
    @get:Rule(order=1) val permissions=GrantPermissionRule.grant(Manifest.permission.CAMERA,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
    @get:Rule(order=2) val ui=createEmptyComposeRule()
    private lateinit var f:Fixture
    private var scenario:ActivityScenario<MainActivity>?=null
    @Before fun setup(){f=Fixture();TestGraph.fixture=f;hilt.inject()}
    @After fun close(){scenario?.close()}
    private fun launch(role:Role?=null){runBlocking{when(role){Role.ADMIN->f.asAdmin();Role.STAFF->f.asStaff();else->Unit}};scenario=ActivityScenario.launch(MainActivity::class.java)}
    private fun waitFor(text:String){ui.waitUntil(10000){ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
    private fun click(text:String){waitFor(text);val node=ui.onNode(hasText(text) and hasClickAction());runCatching{node.performScrollTo()};node.performClick()}
    private fun field(label:String,value:String){val node=ui.onNode(hasText(label) and hasSetTextAction());runCatching{node.performScrollTo()};node.performTextReplacement(value)}
    private fun back(){ui.onNodeWithContentDescription("Back").performClick()}
    private fun employees(){click("Employees & attendance");waitFor("Employees")}
    private fun details(){employees();click("Alex Staff");waitFor("Employee details")}

    @Test fun loginFailureThenAdminLoginAndLogout(){
        launch();waitFor("Staff sign in");click("Admin");field("Username","admin");field("Password","wrong");click("Sign in");waitFor("Username or password is incorrect.")
        field("Password",PASSWORD);click("Sign in");waitFor("Welcome back");ui.onNodeWithContentDescription("Sign out").performClick();waitFor("Staff sign in")
    }
    @Test fun employeeSearchEditAndBackNavigation(){
        launch(Role.ADMIN);employees();field("Search name or employee ID","EMP-001");ui.onNodeWithText("Jamie Staff").assertDoesNotExist();click("Alex Staff");click("Manage");click("Edit staff details");waitFor("Edit staff")
        field("Full name","Alex Updated");click("Save changes");waitFor("Employee details");waitFor("Alex Updated");back();waitFor("Employees")
    }
    @Test fun addStaffFormShowsValidationAndPhotoOptions(){
        launch(Role.ADMIN);employees();click("Add staff");waitFor("Reference photo");ui.onNodeWithText("Upload photo").assertExists();ui.onNodeWithText("Take photo").assertExists();click("Create staff");waitFor("Name and employee ID are required.")
    }
    @Test fun blockAndDeleteRequireConfirmation(){
        launch(Role.ADMIN);details();click("Manage");click("Block Employee");waitFor("Block Alex Staff?");click("Cancel");assertEquals(AccountStatus.ACTIVE,runBlocking{f.repo.read().accounts.first{it.id=="a1"}.status})
        click("Manage");click("Block Employee");click("Block Employee");waitFor("Employee blocked.");click("Manage");click("Unblock Employee");click("Unblock Employee");waitFor("Employee unblocked.")
        click("Manage");click("Delete Employee");waitFor("Delete Alex Staff?");click("Cancel");assertNull(runBlocking{f.repo.read().employees.first{it.id=="e1"}.deletedAt})
        click("Manage");click("Delete Employee");click("Delete Employee");waitFor("Employees");ui.onNodeWithText("Alex Staff").assertDoesNotExist()
    }
    @Test fun noticeGenderFilterSendsOnlyToSelectedStaff(){
        launch(Role.ADMIN);click("Notices & messages");click("Female");click("Select all matching employees");field("Notice title","Team update");field("Message","Please read");click("Send notice");waitFor("Notice sent");click("Done")
        assertEquals(listOf("e1"),runBlocking{f.repo.read().recipients.map{it.employeeId}})
        runBlocking{f.asStaff()};waitFor("Check In");ui.onNodeWithContentDescription("Notifications").performClick();waitFor("Team update");click("Mark as read");ui.onNodeWithText("Mark as read").assertDoesNotExist();click("Reviews");waitFor("You’re all caught up")
    }
    @Test fun reviewApprovalUpdatesStaffAndNotifies(){
        runBlocking{f.asStaff();f.submit(f.punch(score=7999).id)};launch(Role.ADMIN);click("Review requests");click("Alex Staff");waitFor("Capture details");field("Decision note (optional)","Approved by admin");click("Approve");waitFor("The employee has been notified in their inbox.")
        runBlocking{f.asStaff()};waitFor("Check Out");ui.onNodeWithContentDescription("Notifications").performClick();waitFor("Check In approved");ui.onAllNodes(hasText("View attendance") and hasClickAction())[0].performScrollTo().performClick();waitFor("Attendance history")
    }
    @Test fun reviewRejectionDoesNotMarkAttendance(){
        runBlocking{f.asStaff();f.submit(f.punch(score=7999).id)};launch(Role.ADMIN);click("Review requests");click("Alex Staff");field("Decision note (optional)","Retry please");click("Reject");waitFor("The employee has been notified in their inbox.")
        assertTrue(runBlocking{f.repo.read().attendance.isEmpty()});runBlocking{f.asStaff()};waitFor("Check In");ui.onNodeWithContentDescription("Notifications").performClick();waitFor("Check In rejected")
    }
    @Test fun checkinAtEightyThenCheckoutCompletesDay(){
        launch(Role.STAFF);waitFor("Check In");click("Check In");waitFor("Capture photo");ui.waitUntil(10000){!ui.onNodeWithText("Capture photo").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)};click("Capture photo");waitFor("You’re verified");waitFor("80.0% face-match score");click("Done");waitFor("Check Out")
        click("Check Out");waitFor("Capture photo");ui.waitUntil(10000){!ui.onNodeWithText("Capture photo").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)};click("Capture photo");waitFor("You’re verified");click("Done");waitFor("See you tomorrow")
    }
    @Test fun reviewRangeCanSubmitAndFailedRangeOnlyRetries(){
        f.faces.score=7999;launch(Role.STAFF);click("Check In");waitFor("Capture photo");ui.waitUntil(10000){!ui.onNodeWithText("Capture photo").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)};click("Capture photo");waitFor("Let’s double-check");click("Send for Admin Review");waitFor("Sent for review");click("Back to home");waitFor("Attendance under review")
    }
    @Test fun belowFortyCannotSendForReviewOrMarkAttendance(){
        f.faces.score=3999;launch(Role.STAFF);click("Check In");waitFor("Capture photo");ui.waitUntil(10000){!ui.onNodeWithText("Capture photo").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)};click("Capture photo");waitFor("Face not verified");ui.onNodeWithText("Send for Admin Review").assertDoesNotExist();click("Retry photo");waitFor("Capture photo");assertTrue(runBlocking{f.repo.read().attendance.isEmpty()})
    }
    @Test fun historyMonthHolidayYearAndCalendarNavigation(){
        runBlocking{f.asAdmin();f.calendar(LocalDate.of(2026,10,6),DayType.HOLIDAY,"Company day");f.repo.update{w->w.copy(employees=w.employees.map{it.copy(joiningDate=LocalDate.of(2026,9,1))}) to Unit}}
        launch(Role.STAFF);waitFor("Company day");val card=ui.onNode(hasText("attendance",substring=true) and hasClickAction());card.performScrollTo().performClick();waitFor("Attendance history");ui.onNodeWithContentDescription("Previous month").performClick();waitFor("September 2026");back();click("Company day");waitFor("Holidays");click("Next year");waitFor("No holidays listed for 2027")
    }
    @Test fun mandatoryPasswordChangePrecedesStaffHome(){
        runBlocking{f.asAdmin();f.reset("e1","Reset-password-123");f.login("staff","Reset-password-123",Role.STAFF)};launch();waitFor("Choose your own password")
        field("Temporary password","Reset-password-123");field("New password","Changed-password-123");field("Confirm new password","Changed-password-123");click("Change password");waitFor("Check In")
    }
    @Test fun rotationPreservesCurrentDestination(){
        launch(Role.ADMIN);details();scenario!!.recreate();waitFor("Employee details");ui.onNodeWithText("Overall attendance since joining").assertExists()
    }

    @Test fun firstRunCreatesAdminThroughUi(){
        f=Fixture(MemoryRepository());TestGraph.fixture=f;launch();waitFor("Welcome to your workspace");field("Username","owner");field("Password",PASSWORD);field("Office time zone","UTC");click("Create admin account");waitFor("Welcome back");assertEquals(1,runBlocking{f.repo.read().accounts.size})
    }
    @Test fun adminCalendarAndPasswordResetFormsCommitChanges(){
        launch(Role.ADMIN);waitFor("Welcome back");ui.onNodeWithContentDescription("Working calendar").performClick();waitFor("Holiday name");field("Date · YYYY-MM-DD","2026-10-06");field("Holiday name","Office holiday");click("Save calendar day");waitFor("Calendar updated.");assertEquals("Office holiday",runBlocking{f.repo.read().holidays.single().name});back()
        details();click("Manage");click("Reset password");field("Temporary password","Reset-password-123");click("Reset password");waitFor("Temporary password updated.");assertTrue(runBlocking{f.repo.read().accounts.first{it.id=="a1"}.mustChangePassword})
    }
}
