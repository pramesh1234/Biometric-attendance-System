package com.example.attendance.core.device

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import java.io.File

/** Uses real bundled models and two different public-domain portraits, never fake scores. */
@RunWith(AndroidJUnit4::class)
class SFaceRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun fixture(name: String): File = File(context.cacheDir, name).also { file ->
        instrumentation.context.assets.open(name).use { input -> file.outputStream().use { input.copyTo(it) } }
    }
    private fun model(name: String): String = File(context.cacheDir, name).also { file ->
        context.assets.open("models/$name").use { input -> file.outputStream().use { input.copyTo(it) } }
    }.absolutePath

    @Test fun differentPeopleMustNotBecomeASelfMatch() = runBlocking {
        val photos = PrivatePhotoStore(context)
        val a = photos.importImage(Uri.fromFile(fixture("person_a.jpg")).toString())
        val b = photos.importImage(Uri.fromFile(fixture("person_b.png")).toString())
        try {
            assertNotEquals(a, b)
            assertFalse(File(photos.path(a)).readBytes().contentEquals(File(photos.path(b)).readBytes()))
            val verifier = OpenCvFaceVerifier(context, photos)
            verifier.validateReference(a); verifier.validateReference(b)
            // Alternate inputs to catch reusable native output and stale reference bugs.
            for ((ref, capture) in listOf(a to b, b to a, a to a, b to b, a to b)) {
                val score = verifier.compare(ref, capture).basisPoints
                Log.i("SFaceRegression", "sameImage=${ref == capture}, basisPoints=$score")
                if (ref == capture) assertTrue("Same image should match: $score", score >= 9900)
                else assertTrue("Different fixture identities should be below review range, got $score", score < 4000)
            }
        } finally { photos.delete(a); photos.delete(b) }
    }

    @Test fun noFaceDoesNotMarkAMatch() = runBlocking {
        val photos = PrivatePhotoStore(context)
        val a = photos.importImage(Uri.fromFile(fixture("person_a.jpg")).toString())
        val blank = File(context.cacheDir, "blank.png")
        Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            blank.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val b = photos.importImage(Uri.fromFile(blank).toString())
        try { assertEquals(0, OpenCvFaceVerifier(context, photos).compare(a, b).basisPoints) }
        finally { photos.delete(a); photos.delete(b); blank.delete() }
    }

    @Test fun diagnoseNativeOutputOwnership() {
        assertTrue(OpenCVLoader.initLocal())
        val detector = FaceDetectorYN.create(model("face_detection_yunet_2023mar.onnx"), "", Size(320.0,320.0), 0.9f)
        val recognizer = FaceRecognizerSF.create(model("face_recognition_sface_2021dec.onnx"), "")
        fun rawFeature(file: File): Mat {
            val image = Imgcodecs.imread(file.absolutePath); val faces = Mat(); val aligned = Mat()
            try {
                detector.setInputSize(image.size()); detector.detect(image, faces)
                assertEquals(1, faces.rows())
                val face = faces.row(0)
                try { recognizer.alignCrop(image, face, aligned) } finally { face.release() }
                return Mat().also { recognizer.feature(aligned, it) }
            } finally { image.release(); faces.release(); aligned.release() }
        }
        val a = rawFeature(fixture("person_a.jpg"))
        val savedA = a.clone()
        val b = rawFeature(fixture("person_b.png"))
        try {
            val overwritten = Core.norm(a, savedA, Core.NORM_INF)
            val aliasScore = recognizer.match(a, b, FaceRecognizerSF.FR_COSINE)
            val detachedScore = recognizer.match(savedA, b, FaceRecognizerSF.FR_COSINE)
            Log.i("SFaceRegression", "sameNativeBuffer=${a.dataAddr() == b.dataAddr()}, overwriteDelta=$overwritten, originalCosine=$aliasScore, detachedCosine=$detachedScore")
            assertTrue("Saved reference should distinguish these portraits", detachedScore < 0.4)
            // Diagnostic avoids pinning the regression suite to OpenCV's allocation strategy.
        } finally { a.release(); b.release(); savedA.release() }
    }
}
