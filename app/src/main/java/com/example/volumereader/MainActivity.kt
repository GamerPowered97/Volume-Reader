package com.example.volumereader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volumereader.engine.CalibrationLogic
import com.example.volumereader.engine.PhoneModel
import com.example.volumereader.health.HealthLogic
import com.example.volumereader.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Screen {
    Main, Settings
}

class MainActivity : ComponentActivity() {

    private val viewModel: VolumeReaderViewModel by viewModels()
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
            var currentScreen by remember { mutableStateOf(Screen.Main) }

            VolumeReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MachineDark
                ) {
                    if (hasPermission) {
                        when (currentScreen) {
                            Screen.Main -> MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentScreen = Screen.Settings }
                            )
                            Screen.Settings -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToMain = { currentScreen = Screen.Main }
                            )
                        }
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
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasPermission = true
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VolumeReaderViewModel,
    onNavigateToSettings: () -> Unit
) {
    val dbSpl by viewModel.audioEngine.currentDbSpl.collectAsStateWithLifecycle()
    val isRecording by viewModel.audioEngine.isRecording.collectAsStateWithLifecycle()
    val errorMsg by viewModel.audioEngine.error.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val healthAdvice = HealthLogic.getAdviceForDb(dbSpl)

    // Animate the dB value for smooth gauge movement
    val animatedDb by animateFloatAsState(
        targetValue = if (isRecording) dbSpl else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "dbAnimation"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isRecording) healthAdvice.statusColor else TextDim,
        animationSpec = tween(300),
        label = "statusColor"
    )

    // Pulsing glow for danger states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MachineDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── HEADER BAR ─────────────────────────────────────────────
        HeaderPanel(onNavigateToSettings = onNavigateToSettings)

        Spacer(modifier = Modifier.height(8.dp))

        // Active Device Status Line
        Text(
            text = "PROFILE: ${selectedModel.displayName.uppercase()} (OFFSET: ${viewModel.audioEngine.calibrationOffset.roundToInt()} dB)",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── ARC GAUGE ──────────────────────────────────────────────
        MachinePanel {
            ArcGauge(
                dbValue = animatedDb,
                maxDb = 130f,
                accentColor = if (isRecording) healthAdvice.color else MachineBezel,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── DIGITAL READOUT ────────────────────────────────────────
        LcdPanel(
            dbValue = if (isRecording) animatedDb else null,
            accentColor = if (isRecording) healthAdvice.color else MachineBezel,
            pulseAlpha = if (isRecording && healthAdvice.severity >= 3) pulseAlpha else 1f
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── LED BAR GRAPH ──────────────────────────────────────────
        MachinePanel {
            LedBarGraph(
                dbValue = animatedDb,
                maxDb = 130f,
                isActive = isRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── STATUS / HEALTH PANEL ──────────────────────────────────
        StatusPanel(
            isRecording = isRecording,
            healthAdvice = healthAdvice,
            statusColor = statusColor,
            pulseAlpha = if (healthAdvice.severity >= 3 && isRecording) pulseAlpha else 1f
        )

        // ── ERROR MESSAGE ──────────────────────────────────────────
        errorMsg?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠ $msg",
                color = HoloRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LcdBackground, CutCornerShape(2.dp))
                    .border(1.dp, HoloRed.copy(alpha = 0.4f), CutCornerShape(2.dp))
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── CONTROL BUTTONS ────────────────────────────────────────
        ControlButton(
            isRecording = isRecording,
            onStart = { viewModel.startRecording() },
            onStop = { viewModel.stopRecording() }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VolumeReaderViewModel,
    onNavigateToMain: () -> Unit
) {
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    
    var showDialog by remember { mutableStateOf(false) }
    var tempOffset by remember { mutableStateOf(viewModel.audioEngine.calibrationOffset) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MachineDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── SETTINGS HEADER ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(MetalLight, MetalMid, MetalDark)
                    ),
                    shape = CutCornerShape(6.dp)
                )
                .border(1.dp, MachineRidge, CutCornerShape(6.dp))
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(onClick = onNavigateToMain) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = HoloBlue
                )
            }
            Text(
                text = "SYSTEM SETTINGS",
                color = HoloBlue,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── SECTION 1: CALIBRATION ─────────────────────────────────
        Text(
            text = "AUDIO CALIBRATION",
            style = MaterialTheme.typography.labelLarge,
            color = HoloBlueDim,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        MachinePanel {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    text = "Phone Calibration Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Selecting your device applies a known hardware offset to provide accurate dBA / dBSPL measurements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Clicking this button opens the Searchable Dialog (no lag!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MachineDark, CutCornerShape(4.dp))
                        .border(1.dp, MachineRidge, CutCornerShape(4.dp))
                        .clickable { showDialog = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedModel.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HoloBlue
                        )
                        Text(
                            text = "CHANGE",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual offset adjustment
                Text(
                    text = "Manual Offset: ${tempOffset.roundToInt()} dB",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Slider(
                    value = tempOffset,
                    onValueChange = {
                        tempOffset = it
                        viewModel.audioEngine.calibrationOffset = it
                    },
                    valueRange = 60f..110f,
                    colors = SliderDefaults.colors(
                        thumbColor = HoloBlue,
                        activeTrackColor = HoloBlueDim,
                        inactiveTrackColor = MachineBezel
                    )
                )
                Text(
                    text = "Fine-tune the dB offset if you have a secondary calibrated decibel meter for reference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── SECTION 2: SYSTEM UPDATES ──────────────────────────────
        Text(
            text = "SYSTEM UPDATES",
            style = MaterialTheme.typography.labelLarge,
            color = HoloBlueDim,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        MachinePanel {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current App Version",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = viewModel.currentVersion,
                        style = MaterialTheme.typography.titleMedium,
                        color = HoloBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (updateState) {
                    is UpdateState.Idle -> {
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            shape = CutCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MachineDark, contentColor = HoloBlue),
                            modifier = Modifier.fillMaxWidth().border(1.dp, HoloBlue, CutCornerShape(4.dp))
                        ) {
                            Text("CHECK FOR UPDATES")
                        }
                    }
                    is UpdateState.Checking -> {
                        Text(
                            text = "Checking GitHub for updates...",
                            color = HoloBlue,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    is UpdateState.UpToDate -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Your app is up to date!",
                                color = GaugeGreen,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { viewModel.resetUpdateState() }) {
                                Text("OK", color = HoloBlue)
                            }
                        }
                    }
                    is UpdateState.UpdateAvailable -> {
                        val state = updateState as UpdateState.UpdateAvailable
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "New Version Available: ${state.version}",
                                color = HoloOrange,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.downloadAndInstallUpdate(state.downloadUrl) },
                                shape = CutCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MachineDark, contentColor = HoloOrange),
                                modifier = Modifier.fillMaxWidth().border(1.dp, HoloOrange, CutCornerShape(4.dp))
                            ) {
                                Text("DOWNLOAD & INSTALL UPDATE")
                            }
                        }
                    }
                    is UpdateState.Downloading -> {
                        val progress = (updateState as UpdateState.Downloading).progress
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Downloading Update: ${(progress * 100).roundToInt()}%",
                                color = HoloBlue,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                color = HoloBlue,
                                trackColor = MachineBezel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is UpdateState.ReadyToInstall -> {
                        Text(
                            text = "Launching system package installer...",
                            color = GaugeGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    is UpdateState.Error -> {
                        val msg = (updateState as UpdateState.Error).message
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Update Error: $msg",
                                color = HoloRed,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.checkForUpdates() },
                                shape = CutCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MachineDark, contentColor = HoloRed),
                                modifier = Modifier.fillMaxWidth().border(1.dp, HoloRed, CutCornerShape(4.dp))
                            ) {
                                Text("RETRY UPDATE CHECK")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device system information
        Text(
            text = "SYSTEM INFO",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "BUILD MANUFACTURER: ${Build.MANUFACTURER}\nBUILD MODEL: ${Build.MODEL}",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    // Searchable Dialog for smooth device selection
    if (showDialog) {
        SearchableDeviceDialog(
            currentSelected = selectedModel,
            onDismiss = { showDialog = false },
            onSelect = { model ->
                viewModel.selectModel(model)
                tempOffset = model.offset
                showDialog = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SEARCHABLE DEVICE DIALOG (Fixes Dropdown Lag)
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDeviceDialog(
    currentSelected: PhoneModel,
    onDismiss: () -> Unit,
    onSelect: (PhoneModel) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter models on search query
    val filteredModels = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CalibrationLogic.knownModels
        } else {
            CalibrationLogic.knownModels.filter { model ->
                model.displayName.contains(searchQuery, ignoreCase = true) ||
                model.brand.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, MachineRidge, CutCornerShape(8.dp)),
            shape = CutCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MachineSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "SELECT DEVICE PROFILE",
                    style = MaterialTheme.typography.titleMedium,
                    color = HoloBlue,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search phone models...", color = TextDim) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = MachineDark,
                        unfocusedContainerColor = MachineDark,
                        focusedBorderColor = HoloBlue,
                        unfocusedBorderColor = MachineBezel
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = CutCornerShape(4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable LazyColumn for instant rendering (zero lag)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MachineDark, CutCornerShape(4.dp))
                        .border(1.dp, MachineBezel, CutCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    items(filteredModels) { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(model) }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (model == currentSelected) HoloBlue else TextPrimary
                            )
                            if (model != CalibrationLogic.defaultModel) {
                                Text(
                                    text = "offset: ${model.offset.roundToInt()} dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        HorizontalDivider(color = LcdBezel, thickness = 0.5.dp)
                    }
                    if (filteredModels.isEmpty()) {
                        item {
                            Text(
                                text = "No matching device profiles.",
                                color = TextDim,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dismiss button
                Button(
                    onClick = onDismiss,
                    shape = CutCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MachineDark, contentColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MachineRidge, CutCornerShape(4.dp))
                ) {
                    Text("CLOSE")
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// OTHER COMPONENTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun HeaderPanel(onNavigateToSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(MetalLight, MetalMid, MetalDark)
                ),
                shape = CutCornerShape(6.dp)
            )
            .border(1.dp, MachineRidge, CutCornerShape(6.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(48.dp)) // Equalizer spacer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ACOUSTIC  SAFETY  MONITOR",
                    color = HoloBlue,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "MODEL ASM-7700 · REV 2.2",
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = HoloBlue
                )
            }
        }
    }
}

@Composable
fun MachinePanel(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(MetalMid, MetalDark, MachineSurface)
                ),
                shape = CutCornerShape(4.dp)
            )
            .border(1.dp, MachineRidge, CutCornerShape(4.dp))
            .padding(8.dp)
    ) {
        content()
    }
}

@Composable
fun LcdPanel(dbValue: Float?, accentColor: Color, pulseAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(LcdBackground, CutCornerShape(4.dp))
            .border(2.dp, LcdBezel, CutCornerShape(4.dp))
            .drawBehind {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.08f * pulseAlpha),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (dbValue != null) "${dbValue.roundToInt()}" else "--",
                style = MaterialTheme.typography.displayLarge,
                color = accentColor.copy(alpha = pulseAlpha)
            )
            Text(
                text = "dB SPL",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor.copy(alpha = 0.6f)
            )
        }
    }
}

// ── ARC GAUGE (Canvas-drawn) ─────────────────────────────────────────

@Composable
fun ArcGauge(
    dbValue: Float,
    maxDb: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height * 0.85f
        val radius = size.width * 0.42f
        val strokeWidth = size.width * 0.025f

        val startAngle = 200f
        val sweepAngle = 140f

        // Background arc (track)
        drawArc(
            color = MachineBezel,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Draw colored segments
        val segments = listOf(
            Pair(0f to 0.54f, GaugeGreen),     // 0-70 dB
            Pair(0.54f to 0.65f, GaugeYellow),  // 70-85
            Pair(0.65f to 0.77f, GaugeOrange),  // 85-100
            Pair(0.77f to 1f, GaugeRed)          // 100-130
        )

        val progress = (dbValue / maxDb).coerceIn(0f, 1f)

        for ((range, color) in segments) {
            val segStart = range.first
            val segEnd = range.second
            if (progress > segStart) {
                val segProgress = ((progress - segStart) / (segEnd - segStart)).coerceIn(0f, 1f)
                val segSweep = (segEnd - segStart) * sweepAngle
                drawArc(
                    color = color,
                    startAngle = startAngle + segStart * sweepAngle,
                    sweepAngle = segSweep * segProgress,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Butt)
                )
            }
        }

        // Tick marks
        val tickCount = 13  // 0, 10, 20 ... 120
        for (i in 0..tickCount) {
            val tickAngle = startAngle + (i.toFloat() / tickCount) * sweepAngle
            val angleRad = tickAngle * PI.toFloat() / 180f
            val isMajor = i % 2 == 0
            val tickLen = if (isMajor) radius * 0.12f else radius * 0.06f
            val outerR = radius + strokeWidth
            val innerR = outerR + tickLen

            val x1 = centerX + outerR * cos(angleRad)
            val y1 = centerY + outerR * sin(angleRad)
            val x2 = centerX + innerR * cos(angleRad)
            val y2 = centerY + innerR * sin(angleRad)

            drawLine(
                color = if (isMajor) TextSecondary else TextDim,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = if (isMajor) 2f else 1f
            )

            // Labels for major ticks
            if (isMajor) {
                val labelR = innerR + radius * 0.08f
                val lx = centerX + labelR * cos(angleRad)
                val ly = centerY + labelR * sin(angleRad)
                val labelValue = (i * 10).toString()
                val measured = textMeasurer.measure(
                    text = labelValue,
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                )
                drawText(
                    measured,
                    topLeft = Offset(
                        lx - measured.size.width / 2f,
                        ly - measured.size.height / 2f
                    )
                )
            }
        }

        // Needle
        val needleAngle = startAngle + progress * sweepAngle
        val needleAngleRad = needleAngle * PI.toFloat() / 180f
        val needleLength = radius * 0.95f

        val needleTipX = centerX + needleLength * cos(needleAngleRad)
        val needleTipY = centerY + needleLength * sin(needleAngleRad)

        // Needle shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.5f),
            start = Offset(centerX + 2f, centerY + 2f),
            end = Offset(needleTipX + 2f, needleTipY + 2f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        // Needle body
        drawLine(
            color = accentColor,
            start = Offset(centerX, centerY),
            end = Offset(needleTipX, needleTipY),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // Center pivot
        drawCircle(color = MachineHighlight, radius = 8f, center = Offset(centerX, centerY))
        drawCircle(color = accentColor, radius = 4f, center = Offset(centerX, centerY))
    }
}

// ── LED BAR GRAPH ────────────────────────────────────────────────────

@Composable
fun LedBarGraph(
    dbValue: Float,
    maxDb: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val segmentCount = 40

    Canvas(modifier = modifier) {
        val segWidth = (size.width - (segmentCount - 1) * 2f) / segmentCount
        val progress = if (isActive) (dbValue / maxDb).coerceIn(0f, 1f) else 0f
        val litSegments = (progress * segmentCount).toInt()

        for (i in 0 until segmentCount) {
            val x = i * (segWidth + 2f)
            val fraction = i.toFloat() / segmentCount

            val segColor = when {
                fraction < 0.54f -> GaugeGreen
                fraction < 0.65f -> GaugeYellow
                fraction < 0.77f -> GaugeOrange
                else -> GaugeRed
            }

            val isLit = i < litSegments
            val color = if (isLit) segColor else segColor.copy(alpha = 0.08f)

            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(segWidth, size.height),
                cornerRadius = CornerRadius(1f, 1f)
            )
        }
    }
}

// ── STATUS PANEL ─────────────────────────────────────────────────────

@Composable
fun StatusPanel(
    isRecording: Boolean,
    healthAdvice: com.example.volumereader.health.HealthAdvice,
    statusColor: Color,
    pulseAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MachineSurface, CutCornerShape(4.dp))
            .border(1.dp, MachineRidge, CutCornerShape(4.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STATUS",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = statusColor.copy(alpha = pulseAlpha))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) healthAdvice.effect.uppercase() else "STANDBY",
                        style = MaterialTheme.typography.titleSmall,
                        color = statusColor.copy(alpha = pulseAlpha)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MachineRidge, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRecording) healthAdvice.advice
                       else "Press START to begin monitoring environmental noise levels.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ExposureLimitChip("NIOSH", healthAdvice.nioshLimit)
                    ExposureLimitChip("OSHA", healthAdvice.oshaLimit)
                }
            }
        }
    }
}

@Composable
fun ExposureLimitChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(LcdBackground, CutCornerShape(2.dp))
            .border(1.dp, MachineBezel, CutCornerShape(2.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = HoloBlue)
    }
}

// ── CONTROL BUTTON ───────────────────────────────────────────────────

@Composable
fun ControlButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isRecording) HoloRed else HoloBlue,
        animationSpec = tween(300),
        label = "buttonBorder"
    )

    Button(
        onClick = { if (isRecording) onStop() else onStart() },
        shape = CutCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) MachineDark else MachineSurface,
            contentColor = if (isRecording) HoloRed else HoloBlue
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(2.dp, borderColor, CutCornerShape(6.dp))
    ) {
        Text(
            text = if (isRecording) "■  STOP  MONITORING" else "▶  START  MONITORING",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// ── PERMISSION SCREEN ────────────────────────────────────────────────

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MachineDark),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "MICROPHONE ACCESS REQUIRED",
            color = HoloBlue,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This application requires microphone access\nto measure environmental sound levels.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            shape = CutCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HoloBlue, contentColor = MachineDark)
        ) {
            Text("GRANT PERMISSION", style = MaterialTheme.typography.titleMedium)
        }
    }
}
