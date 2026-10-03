package com.example.attendance.feature.onboarding
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attendance.core.designsystem.*

@Composable fun LoginScreen(vm: OnboardingViewModel = hiltViewModel()) {
    val setup by vm.needsSetup.collectAsStateWithLifecycle(); val busy by vm.busy.collectAsStateWithLifecycle(); val error by vm.error.collectAsStateWithLifecycle()
    var admin by rememberSaveable { mutableStateOf(false) }; var username by rememberSaveable { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var visible by remember { mutableStateOf(false) }; var zone by rememberSaveable { mutableStateOf(vm.initialZone) }
    Screen("Attendance") {
        Icon(Icons.Outlined.Face, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(if (setup == true) "Welcome to your workspace" else "Good to see you.", style = MaterialTheme.typography.headlineLarge)
        Caption(if (setup == true) "Create the first administrator account on this device." else "Sign in to start your day.")
        if (setup == null) CircularProgressIndicator() else {
            if (setup == false) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(!admin, { admin = false }, { Text("Staff") }); FilterChip(admin, { admin = true }, { Text("Admin") })
            }
            SectionTitle(if (setup == true || admin) "Admin sign in" else "Staff sign in")
            Field("Username", username, { username = it })
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Toggle password visibility") } })
            if (setup == true) { Field("Office time zone", zone, { zone = it }); Caption("For example Asia/Kolkata. Monday–Friday are working days. Use at least 10 characters for your password.") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(if (busy) "Please wait…" else if (setup == true) "Create admin account" else "Sign in", { if (setup == true) vm.createAdmin(username, password, zone) else vm.signIn(username, password, admin) }, !busy && username.isNotBlank() && password.isNotBlank())
            Caption("Need access or a password reset? Contact your admin.")
        }
    }
}
@Composable fun ChangePasswordScreen(onSignOut: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    var old by remember { mutableStateOf("") }; var new by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    val busy by vm.busy.collectAsStateWithLifecycle(); val error by vm.error.collectAsStateWithLifecycle()
    Screen("Change temporary password", actions = { TextButton(onSignOut) { Text("Sign out") } }) {
        Text("Choose your own password", style = MaterialTheme.typography.headlineSmall); Caption("Replace the temporary password your admin provided before using the app.")
        listOf(Triple("Temporary password", old, { v: String -> old = v }), Triple("New password", new, { v: String -> new = v }), Triple("Confirm new password", confirm, { v: String -> confirm = v })).forEach { (label,value,change) -> OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }
        Caption("At least 10 characters."); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(if (busy) "Saving…" else "Change password", { vm.change(old,new) }, !busy && new == confirm && new.length >= 10 && old.isNotBlank())
    }
}
