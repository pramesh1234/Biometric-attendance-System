package com.example.attendance.core.data
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.attendance.core.database.*
import com.example.attendance.core.domain.*
import com.example.attendance.core.model.*
import com.example.attendance.core.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomIntegrationTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AttendanceDatabase
    private lateinit var repo: RoomWorkspaceRepository
    private lateinit var f: Fixture
    private lateinit var name:String
    @Before fun setup() = runBlocking {
        name="test-${UUID.randomUUID()}.db";db=createAttendanceDatabase(context,name);repo=RoomWorkspaceRepository(db)
        repo.update{Fixture.seed() to Unit};f=Fixture(repo)
    }
    @After fun close() { db.close();context.deleteDatabase(name) }
    private suspend fun rejected(block:suspend()->Unit) {
        val result=runCatching{block()};assertTrue("Expected storage constraint rejection",result.isFailure)
        assertTrue("Expected SQLite constraint, got ${result.exceptionOrNull()}",result.exceptionOrNull() is android.database.sqlite.SQLiteConstraintException)
    }
    @Test fun allElevenTablesAndEvidenceSurviveReopen() = runBlocking {
        f.asStaff();val id=f.submit(f.punch(score=7999).id);f.asAdmin();f.decide(id,true,"Accepted");f.send(setOf("e1","e2"),"Notice","Body");f.calendar(LocalDate.of(2026,10,6),DayType.HOLIDAY,"Holiday")
        f.asStaff();f.clock.advanceSeconds(60);f.punch(PunchAction.CHECK_OUT,8000);f.readNotification(repo.read().notifications.last{it.employeeId=="e1"}.id)
        val before=repo.read();db.close();db=createAttendanceDatabase(context,name);repo=RoomWorkspaceRepository(db);assertEquals(before,repo.read())
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use{assertFalse(it.moveToFirst())}
        db.openHelper.readableDatabase.query("PRAGMA integrity_check").use{assertTrue(it.moveToFirst());assertEquals("ok",it.getString(0))}
        db.openHelper.readableDatabase.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'room_%' AND name NOT LIKE 'android_%'").use{it.moveToFirst();assertEquals(11,it.getInt(0))}
    }
    @Test fun transactionRollsBackEarlierWritesWhenLaterTableFails() = runBlocking {
        val before=repo.read();val e=before.employees.first().copy(id="new",code="NEW")
        rejected { repo.update{w->w.copy(employees=w.employees+e,accounts=w.accounts+ w.accounts.first().copy(id="broken",username="broken",employeeId="missing",role=Role.STAFF)) to Unit} }
        assertEquals(before,repo.read())
    }
    @Test fun noticeAndReviewChangesAreAtomicAndIdempotent() = runBlocking {
        f.asStaff();val id=f.submit(f.punch(score=7000).id);f.asAdmin();f.decide(id,true,"");f.decide(id,true,"");f.send(setOf("e1","e2"),"Notice","Body")
        val w=repo.read();assertEquals(1,w.attendance.size);assertEquals(2,w.recipients.size);assertEquals(4,w.notifications.size)
        assertEquals(1,w.notifications.count{it.type==NotificationType.REVIEW_APPROVED})
    }
    @Test fun duplicateKeysAndActivePhotosAreRejected() = runBlocking {
        val w=repo.read()
        rejected {repo.update{it.copy(employees=it.employees+w.employees.first().copy(id="duplicate")) to Unit}}
        rejected {repo.update{it.copy(accounts=it.accounts+w.accounts.first().copy(id="duplicate")) to Unit}}
        rejected {repo.update{it.copy(photos=it.photos+w.photos.first().copy(id="duplicate")) to Unit}}
        assertEquals(w,repo.read())
        f.asStaff();f.punch();val a=repo.read().attendance.single()
        rejected {repo.update{it.copy(attendance=it.attendance+a.copy(id="duplicate")) to Unit}}
    }
    @Test fun foreignKeyAndReferenceOwnershipAreEnforced() = runBlocking {
        val w=repo.read();rejected {repo.update{it.copy(photos=it.photos+w.photos.first().copy(id="other",employeeId="missing")) to Unit}}
        f.asStaff();val a=f.punch(score=6000)
        rejected {repo.update{it.copy(attempts=it.attempts+a.copy(id="cross-owner",referencePhotoId="pe2")) to Unit}}
        rejected {db.openHelper.writableDatabase.execSQL("DELETE FROM employees WHERE id='e1'")}
    }
    @Test fun invalidRolesScoresReviewAndNotificationShapesAreRejected() = runBlocking {
        val before=repo.read();val account=before.accounts.first{it.role==Role.STAFF}
        rejected {repo.update{w->w.copy(accounts=w.accounts.map{if(it.id==account.id)it.copy(employeeId=null)else it}) to Unit}}
        f.asStaff();val a=f.punch();rejected {repo.update{it.copy(attempts=it.attempts+a.copy(id="bad-score",confidenceBps=10001)) to Unit}}
        rejected {repo.update{it.copy(reviews=listOf(ReviewRequest("invalid",a.id,ReviewStatus.PENDING,f.clock.instant()))) to Unit}}
        rejected {repo.update{it.copy(notifications=listOf(InboxNotification("invalid","e1",NotificationType.NOTICE,createdAt=f.clock.instant()))) to Unit}}
    }
    @Test fun updatingRowsWithoutChangingCountEmitsFreshWorkspace() = runBlocking {
        val changed=async(start=CoroutineStart.UNDISPATCHED){withTimeout(5000){repo.observe().first{it.accounts.any{a->a.id=="a1"&&a.status==AccountStatus.BLOCKED}}}}
        f.asAdmin();f.access("e1",true);assertEquals(AccountStatus.BLOCKED,changed.await().accounts.first{it.id=="a1"}.status)
    }
    @Test fun concurrentCaptureCannotCreateDuplicateAttendance() = runBlocking {
        f.asStaff();val results=(1..2).map{async(Dispatchers.Default){runCatching{f.punch()}}}.awaitAll()
        assertEquals(1,results.count{it.isSuccess});assertEquals(1,repo.read().attendance.size);assertEquals(1,repo.read().attempts.size)
    }
    @Test fun replacementReferenceBlockAndDeletePreserveHistory() = runBlocking {
        f.asStaff();f.punch();f.asAdmin();f.save(f.draft("e1","EMP-001","staff").copy(temporaryPassword=""));f.access("e1",true);f.access("e1",false);f.access("e1",true,true)
        val w=repo.read();assertEquals(1,w.attendance.size);assertEquals(1,w.attempts.size);assertEquals(2,w.photos.count{it.employeeId=="e1"});assertNotNull(w.photos.first{it.id=="pe1"}.retiredAt)
    }
}
