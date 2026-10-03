package com.example.attendance.core.device

import android.content.Context
import com.example.attendance.core.domain.DomainException
import com.example.attendance.core.domain.FaceScore
import com.example.attendance.core.domain.FaceVerifier
import com.example.attendance.core.domain.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import java.io.File
import kotlin.math.roundToInt

/** Scores are cosine similarity × 10000, not calibrated identity probabilities. No liveness claim. */
class OpenCvFaceVerifier(private val context: Context, private val photos: PhotoStore) :
    FaceVerifier {
    private val mutex = Mutex()
    private var detector: FaceDetectorYN? = null
    private var recognizer: FaceRecognizerSF? = null
    private fun model(name: String): String {
        val file = File(context.noBackupFilesDir, name)
        if (!file.exists()) {
            val temporary = File(context.noBackupFilesDir, "$name.tmp")
            context.assets.open("models/$name")
                .use { input -> temporary.outputStream().use { input.copyTo(it) } }
            if (!temporary.renameTo(file)) throw DomainException("Unable to prepare the face verification model.")
        }
        return file.absolutePath
    }

    private fun initialize() {
        if (detector != null) return
        if (!OpenCVLoader.initLocal()) throw DomainException("Face verification is unavailable on this device.")
        val loadedDetector = FaceDetectorYN.create(
            model("face_detection_yunet_2023mar.onnx"),
            "",
            Size(320.0, 320.0),
            0.9f
        )
        val loadedRecognizer =
            FaceRecognizerSF.create(model("face_recognition_sface_2021dec.onnx"), "")
        detector = loadedDetector
        recognizer = loadedRecognizer
    }

    private fun feature(key: String): Mat? {
        val image = Imgcodecs.imread(photos.path(key));
        val faces = Mat();
        val aligned = Mat();
        val output = Mat()
        try {
            if (image.empty()) throw DomainException("Photo could not be read.")
            detector!!.setInputSize(image.size()); detector!!.detect(image, faces)
            if (faces.rows() != 1) return null
            val face = faces.row(0)
            try {
                recognizer!!.alignCrop(image, face, aligned)
            } finally {
                face.release()
            }
            recognizer!!.feature(aligned, output)
            if (output.empty() || !Core.checkRange(output) || Core.norm(output) <= 0.0) {
                throw DomainException("Face embedding could not be computed. Please retry.")
            }
            // DNN forward() may reuse its native output buffer on the next call.
            // A new Mat header is not a deep copy: detach BEFORE another inference,
            // otherwise the capture overwrites the reference and match becomes 1.0.
            return output.clone()
        } finally {
            output.release(); image.release(); faces.release(); aligned.release()
        }
    }

    override suspend fun validateReference(key: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            initialize();
            val feature = feature(key)
                ?: throw DomainException("Use a clear photo containing exactly one face."); feature.release()
        }
    }

    override suspend fun compare(referenceKey: String, capturedKey: String): FaceScore =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                initialize()
                val reference = feature(referenceKey)
                    ?: throw DomainException("Your reference photo is invalid. Contact your admin.")
                try {
                    val capture = feature(capturedKey) ?: return@withLock FaceScore(
                        0,
                        "opencv-sface-2021dec-cosine-v2-owned"
                    )
                    try {
                        val cosine =
                            recognizer!!.match(reference, capture, FaceRecognizerSF.FR_COSINE)
                        if (!cosine.isFinite() || cosine < -1.00001 || cosine > 1.00001) throw DomainException(
                            "Face matching failed. Please retry."
                        )
                        FaceScore(
                            (cosine.coerceIn(0.0, 1.0) * 10000).roundToInt(),
                            "opencv-sface-2021dec-cosine-v2-owned"
                        )
                    } finally {
                        capture.release()
                    }
                } finally {
                    reference.release()
                }
            }
        }
}
