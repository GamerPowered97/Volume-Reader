package com.example.volumereader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.volumereader.engine.AudioEngine
import com.example.volumereader.engine.CalibrationLogic
import com.example.volumereader.health.HealthLogic
import com.example.volumereader.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val audioEngine = AudioEngine()
    private var hasPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermission()

        setContent {
            VolumeReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermission) {
                        MainScreen(audioEngine)
                    } else {
                        PermissionScreen { checkPermission() }
                    }
                }
            }
        }
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasPermission = true
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.stopRecording()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(audioEngine: AudioEngine) {
    val dbSpl by audioEngine.currentDbSpl.collectAsState()
    val healthAdvice = HealthLogic.getAdviceForDb(dbSpl)
    
    var isRecording by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(CalibrationLogic.knownModels.last()) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MachineSurface, MachineDark)
                    ),
                    shape = CutCornerShape(4.dp)
                )
                .border(2.dp, MachineBezel, CutCornerShape(4.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ACOUSTIC SAFETY MONITOR",
                color = HoloBlue,
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Device Selection (Machine style dropdown)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = "${selectedModel.brand} ${selectedModel.model}",
                onValueChange = {},
                readOnly = true,
                label = { Text("Calibration Profile", color = HoloBlue) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = HoloBlue,
                    unfocusedBorderColor = MachineBezel,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = CutCornerShape(0.dp) // Sharp edges
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MachineSurface)
            ) {
                CalibrationLogic.knownModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text("${model.brand} ${model.model}", color = TextPrimary) },
                        onClick = {
                            selectedModel = model
                            audioEngine.calibrationOffset = model.offset
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // DB Readout Screen (Digital style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .shadow(10.dp, CutCornerShape(8.dp), clip = false)
                .background(Color.Black, CutCornerShape(8.dp))
                .border(3.dp, healthAdvice.color, CutCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRecording) "${dbSpl.roundToInt()} dB" else "-- dB",
                style = MaterialTheme.typography.displayLarge,
                color = if (isRecording) healthAdvice.color else MachineBezel
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Health Warning Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MachineSurface, CutCornerShape(4.dp))
                .border(1.dp, MachineBezel, CutCornerShape(4.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) healthAdvice.effect.uppercase() else "SYSTEM STANDBY",
                    color = if (isRecording) healthAdvice.color else TextSecondary,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isRecording) healthAdvice.advice else "Press START to begin monitoring environmental noise.",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Machine Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        audioEngine.stopRecording()
                    } else {
                        isRecording = true
                        coroutineScope.launch {
                            audioEngine.startRecording()
                        }
                    }
                },
                shape = CutCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MachineDark else MachineSurface,
                    contentColor = if (isRecording) HoloRed else HoloBlue
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .border(2.dp, if (isRecording) HoloRed else HoloBlue, CutCornerShape(4.dp))
            ) {
                Text(
                    text = if (isRecording) "STOP" else "START",
                    style = MaterialTheme.typography.titleLarge,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Audio recording permission is required.",
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRequestPermission,
            shape = CutCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HoloBlue)
        ) {
            Text("GRANT PERMISSION", color = MachineDark)
        }
    }
}
