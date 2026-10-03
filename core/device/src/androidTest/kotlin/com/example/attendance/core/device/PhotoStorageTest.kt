package com.example.attendance.core.device
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.attendance.core.model.Role
import com.example.attendance.core.model.Session
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.ZoneId
@RunWith(AndroidJUnit4::class)
class PhotoStorageTest {
 private val instrumentation=InstrumentationRegistry.getInstrumentation()
 private val context=instrumentation.targetContext
 private fun sample():File=File(context.cacheDir,"import-test.jpg").also{file->instrumentation.context.assets.open("person_a.jpg").use{input->file.outputStream().use{input.copyTo(it)}}}
 @Test fun repeatedImportsHaveIndependentFilesAndDeletingOnePreservesOther()=runBlocking {
  val store=PrivatePhotoStore(context);val file=sample();val a=store.importImage(Uri.fromFile(file).toString());val b=store.importImage(Uri.fromFile(file).toString())
  try{assertNotEquals(a,b);assertTrue(File(store.path(a)).exists());assertTrue(File(store.path(b)).exists());store.delete(a);assertFalse(File(store.path(a)).exists());assertTrue(File(store.path(b)).exists())}finally{store.delete(a);store.delete(b);file.delete()}
 }
 @Test fun pathTraversalIsRejected(){val store=PrivatePhotoStore(context);for(key in listOf("../accounts.db","/sdcard/photo.jpg","../../file","not-a-key")){assertThrows(IllegalArgumentException::class.java){store.path(key)}}}
 @Test fun unreadableImageFailsWithoutCreatingPhoto()=runBlocking {
  val file=File(context.cacheDir,"invalid-image").apply{writeText("not an image")};val directory=File(context.filesDir,"photos");val store=PrivatePhotoStore(context);val before=directory.list()?.toSet().orEmpty()
  try{assertTrue(runCatching{store.importImage(Uri.fromFile(file).toString())}.isFailure);assertEquals(before,directory.list()?.toSet().orEmpty())}finally{file.delete()}
 }
 @Test fun noFaceCannotBeEnrolledAsReference()=runBlocking {
  val file=File(context.cacheDir,"reference-blank.png");val bitmap=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
  val store=PrivatePhotoStore(context);val key=store.importImage(Uri.fromFile(file).toString())
  try{assertTrue(runCatching{OpenCvFaceVerifier(context,store).validateReference(key)}.isFailure)}finally{store.delete(key);file.delete()}
 }
 @Test fun officeZonePersistsAcrossSettingsInstances(){val prefs=context.getSharedPreferences("office",0);val old=prefs.getString("zone",null);try{val settings=LocalOfficeSettings(context);settings.setZone(ZoneId.of("Asia/Kolkata"));assertEquals(ZoneId.of("Asia/Kolkata"),settings.zone.value);assertEquals(ZoneId.of("Asia/Kolkata"),LocalOfficeSettings(context).zone.value)}finally{prefs.edit().apply{if(old==null)remove("zone")else putString("zone",old)}.commit()}}
 @Test fun sessionPersistsAcrossStoreInstances(){val prefs=context.getSharedPreferences("session",0);prefs.edit().clear().commit();try{val session=Session("account-1",Role.STAFF,"employee-1",Instant.parse("2026-10-01T10:15:30Z"),false);val store=PersistentSessionStore(context);store.set(session);assertEquals(session,PersistentSessionStore(context).session.value);store.set(null);assertNull(PersistentSessionStore(context).session.value)}finally{prefs.edit().clear().commit()}}
}
