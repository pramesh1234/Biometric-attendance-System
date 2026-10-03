package com.example.attendance.feature.admin
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.attendance.core.designsystem.*
import com.example.attendance.core.model.*
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable fun StaffEditorScreen(employeeId: String?, onBack: () -> Unit, vm: AdminViewModel = hiltViewModel()) {
    val data by vm.data.collectAsStateWithLifecycle(); val busy by vm.busy.collectAsStateWithLifecycle();val zone by vm.zone.collectAsStateWithLifecycle()
    val profile=data?.staff?.firstOrNull{it.employee.id==employeeId}
    var loaded by rememberSaveable(employeeId){mutableStateOf(false)}
    var name by rememberSaveable {mutableStateOf("")};var code by rememberSaveable{mutableStateOf("")};var job by rememberSaveable{mutableStateOf("")};var email by rememberSaveable{mutableStateOf("")};var phone by rememberSaveable{mutableStateOf("")};var gender by rememberSaveable{mutableStateOf("Prefer not to say")};var joining by rememberSaveable{mutableStateOf(LocalDate.now(zone).toString())};var ending by rememberSaveable{mutableStateOf("")};var username by rememberSaveable{mutableStateOf("")};var password by remember{mutableStateOf("")};var photo by rememberSaveable{mutableStateOf<String?>(null)};var inputError by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(profile){if(!loaded&&profile!=null){name=profile.employee.name;code=profile.employee.code;job=profile.employee.jobTitle;email=profile.employee.email;phone=profile.employee.phone;gender=profile.employee.gender;joining=profile.employee.joiningDate.toString();ending=profile.employee.endDate?.toString().orEmpty();username=profile.username;loaded=true}}
    val context=LocalContext.current
    var captureUri by rememberSaveable{mutableStateOf<String?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->uri?.let{vm.importPhoto(it.toString()){photo=it}}}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ok->if(ok)captureUri?.let{vm.importPhoto(it){photo=it}}}
    fun launchCamera(){val dir=File(context.cacheDir,"captures").also{it.mkdirs()};val file=File.createTempFile("reference-",".jpg",dir);val uri=FileProvider.getUriForFile(context,context.packageName+".files",file);captureUri=uri.toString();camera.launch(uri)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)launchCamera() else inputError="Camera permission is needed to take a photo. You can also upload one."}
    Screen(if(employeeId==null)"Add staff" else "Edit staff",onBack){
        SectionTitle("Reference photo")
        (photo?:profile?.photoKey)?.let{AsyncImage(vm.path(it),"Reference photo",Modifier.fillMaxWidth().height(180.dp))}?:Caption("Add a clear, front-facing portrait of one person.")
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){OutlinedButton({picker.launch("image/*")},enabled=!busy){Text("Upload photo")};OutlinedButton({if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)launchCamera() else permission.launch(Manifest.permission.CAMERA)},enabled=!busy){Text("Take photo")}}
        Field("Full name",name,{name=it});Field("Employee ID",code,{code=it});Field("Job title",job,{job=it});Field("Email",email,{email=it});Field("Phone",phone,{phone=it})
        SectionTitle("Gender");FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Female","Male","Non-binary","Prefer not to say").forEach{label->FilterChip(gender==label,{gender=label},{Text(label)})}}
        Field("Joining date · YYYY-MM-DD",joining,{joining=it});Field("Last working date · optional",ending,{ending=it})
        SectionTitle("Login credentials");Field("Username",username,{username=it});OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text(if(employeeId==null)"Temporary password" else "New temporary password (optional)")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
        Caption("At least 10 characters. Share the temporary password with the employee; they must change it at login.")
        AdminFeedback(vm);inputError?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        PrimaryButton(if(busy)"Saving…" else if(employeeId==null)"Create staff" else "Save changes",{
            val start=runCatching{LocalDate.parse(joining)}.getOrNull();val end=if(ending.isBlank())null else runCatching{LocalDate.parse(ending)}.getOrNull()
            if(start==null||(ending.isNotBlank()&&end==null))inputError="Enter dates as YYYY-MM-DD." else {inputError=null;vm.save(StaffDraft(employeeId,code,name,email,phone,job,gender,start,end,username,password,photo),onBack)}
        },!busy&&(employeeId==null||loaded))
    }
}
