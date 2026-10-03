package com.example.attendance.feature.staff

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.attendance.core.common.displayTime
import com.example.attendance.core.designsystem.Badge
import com.example.attendance.core.designsystem.Caption
import com.example.attendance.core.designsystem.Panel
import com.example.attendance.core.designsystem.PrimaryButton
import com.example.attendance.core.designsystem.Screen
import com.example.attendance.core.designsystem.SectionTitle
import com.example.attendance.core.domain.message
import com.example.attendance.core.model.GeofenceStatus
import com.example.attendance.core.model.Outcome
import java.io.File
import java.time.ZoneId

@Composable
fun VerificationScreen(
    checkOut: Boolean, onBack: () -> Unit, vm: StaffViewModel = hiltViewModel()
) {
    val context = LocalContext.current;
    val attempt by vm.attempt.collectAsStateWithLifecycle();
    val busy by vm.busy.collectAsStateWithLifecycle();
    val submitted by vm.submitted.collectAsStateWithLifecycle()
    val geofence by vm.geofence.collectAsStateWithLifecycle()
    val warning by vm.geofenceWarning.collectAsStateWithLifecycle()
    fun granted() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED && (ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED)

    var allowed by remember { mutableStateOf(granted()) };
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            allowed = granted()
        };
    val action = if (checkOut) "Check Out" else "Check In"
    val isInsideGeofence = geofence.state.status == GeofenceStatus.INSIDE
    Screen("Verify $action", onBack) {
        StaffFeedback(vm)
        if (warning != null) AlertDialog(
            onDismissRequest = vm::dismissGeofenceWarning,
            title = { Text("Move closer to the office location") },
            text = { Text(warning!!) },
            confirmButton = { TextButton(vm::dismissGeofenceWarning) { Text("OK") } })
        if (geofence.state.status == GeofenceStatus.OUTSIDE) AlertDialog(
            onDismissRequest = {},
            title = { Text("Outside attendance area") },
            text = { Text(geofence.state.message()) },
            confirmButton = { TextButton({}) { Text("Move closer") } })
        if (submitted) {
            Panel(tinted = true) {
                Icon(
                    Icons.Outlined.Schedule, null, Modifier.size(48.dp)
                ); Text(
                "Sent for review", style = MaterialTheme.typography.headlineSmall
            ); Text("Your admin will review your photo."); Caption("Attendance stays pending until approved.")
            }; PrimaryButton("Back to home", onBack)
        } else if (attempt != null) {
            val a = attempt!!; AsyncImage(
                vm.path(a.imageKey),
                "Captured attendance photo",
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )
            Panel(tinted = true) {
                Text(
                    when (a.outcome) {
                        Outcome.VERIFIED -> "You’re verified"; Outcome.REVIEW_REQUIRED -> "Let’s double-check"; Outcome.FAILED -> "Face not verified"
                    }, style = MaterialTheme.typography.headlineSmall
                ); Badge(
                "${a.confidenceBps / 100.0}% face-match score", a.outcome != Outcome.VERIFIED
            ); Caption(
                if (a.outcome == Outcome.VERIFIED) "$action recorded at ${
                    displayTime(
                        a.capturedAt, ZoneId.of(a.timeZoneId)
                    )
                }" else "$action has not been marked."
            )
            }
            when (a.outcome) {
                Outcome.VERIFIED -> PrimaryButton("Done", onBack); Outcome.REVIEW_REQUIRED -> {
                PrimaryButton("Retry photo", vm::retry, !busy); OutlinedButton(
                    vm::submit, Modifier.fillMaxWidth(), enabled = !busy
                ) { Text("Send for Admin Review") }
            }; Outcome.FAILED -> {
                Caption("Make sure only your face is visible, with good lighting."); PrimaryButton(
                    "Retry photo", vm::retry, !busy
                )
            }
            }
        } else if (!allowed) {
            Panel(tinted = true) {
                Icon(Icons.Outlined.CameraAlt, null, Modifier.size(48.dp)); Text(
                "Allow camera and location", style = MaterialTheme.typography.headlineSmall
            ); Caption("We need your photo and current location to record attendance.")
            }; PrimaryButton("Grant permissions", {
                permission.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }); Caption("If permission was previously denied permanently, enable it in Android Settings.")
        } else {
            Text(
                "Center your face", style = MaterialTheme.typography.headlineSmall
            ); Caption("Look at the camera in good lighting."); GeofencePanel(geofence.state); LiveCamera(
                !busy && isInsideGeofence
            ) {
                vm.capture(
                    it, checkOut
                )
            }; Caption("Face matching runs on this device. Your photo is not uploaded.")
        }
    }
}

@Composable
private fun GeofencePanel(state: com.example.attendance.core.model.GeofenceState) {
    val outside = state.status != GeofenceStatus.INSIDE
    Panel(tinted = outside) {
        SectionTitle(if (outside) "Location required" else "Location verified")
        Text(state.message())
        state.distanceM?.let { Caption("${it.toInt()} m from office · allowed radius 100 m") }
    }
}

@Composable
private fun LiveCamera(
    enabled: Boolean,
    onCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val capture = remember {
        ImageCapture.Builder()
            .setCaptureMode(
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            )
            .build()
    }

    var cameraError by remember {
        mutableStateOf<String?>(null)
    }

    var ready by remember {
        mutableStateOf(false)
    }

    var capturing by remember {
        mutableStateOf(false)
    }

    DisposableEffect(owner) {

        val future =
            ProcessCameraProvider.getInstance(context)

        var provider: ProcessCameraProvider? = null
        var disposed = false

        future.addListener(
            {
                if (!disposed) {
                    try {
                        provider = future.get()

                        val selector =
                            if (
                                provider!!.hasCamera(
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                )
                            ) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                        val preview =
                            Preview.Builder()
                                .build()
                                .apply {
                                    setSurfaceProvider(
                                        previewView.surfaceProvider
                                    )
                                }

                        provider!!.unbindAll()

                        provider!!.bindToLifecycle(
                            owner,
                            selector,
                            preview,
                            capture
                        )

                        ready = true

                    } catch (e: Exception) {
                        cameraError =
                            "Camera could not start. Close this screen and try again."
                    }
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            disposed = true
            provider?.unbindAll()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // CAMERA ONLY
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Face guide
            Box(
                modifier = Modifier
                    .size(
                        width = 175.dp,
                        height = 235.dp
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        // ERROR BELOW CAMERA
        cameraError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }

        // BUTTON BELOW CAMERA
        PrimaryButton(
            text = when {
                capturing -> "Capturing…"
                !enabled -> "Verifying location…"
                else -> "Capture photo"
            },
            onClick = {

                capturing = true
                cameraError = null

                val directory =
                    File(
                        context.cacheDir,
                        "captures"
                    ).also {
                        it.mkdirs()
                    }

                val file =
                    File.createTempFile(
                        "attendance-",
                        ".jpg",
                        directory
                    )

                capture.takePicture(
                    ImageCapture.OutputFileOptions
                        .Builder(file)
                        .build(),
                    ContextCompat.getMainExecutor(context),
                    object :
                        ImageCapture.OnImageSavedCallback {

                        override fun onImageSaved(
                            output: ImageCapture.OutputFileResults
                        ) {
                            capturing = false

                            onCaptured(
                                Uri.fromFile(file)
                                    .toString()
                            )
                        }

                        override fun onError(
                            exception: ImageCaptureException
                        ) {
                            capturing = false
                            file.delete()

                            cameraError =
                                "Photo could not be captured. Please retry."
                        }
                    }
                )
            },
            enabled = enabled && ready && !capturing
        )
    }
}