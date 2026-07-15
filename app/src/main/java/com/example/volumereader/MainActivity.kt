package com.example.volumereader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

// ═════════════════════════════════════════════════════════════════════════════
//  Shared drawing decorators/helpers
// ═════════════════════════════════════════════════════════════════════════════

private fun Modifier.machinePanelBackground(): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(MetalLight, MetalMid, MetalDark, MetalMid)
        ),
        shape = CutCornerShape(6.dp)
    )
    .border(1.dp, MachineRidge, CutCornerShape(6.dp))

private fun Modifier.lcdWell(): Modifier = this
    .background(LcdBackground, CutCornerShape(4.dp))
    .border(1.5.dp, LcdBezel, CutCornerShape(4.dp))

// ═════════════════════════════════════════════════════════════════════════════
//  Custom Canvas-drawn Industrial Icons (no external dependencies)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsIcon(modifier: Modifier = Modifier, color: Color = HoloBlue) {
    Canvas(modifier = modifier.size(24.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.22f
        
        for (i in 0 until 8) {
            val angle = i * PI / 4.0
            val x1 = cx + (r * 1.5f) * cos(angle).toFloat()
            val y1 = cy + (r * 1.5f) * sin(angle).toFloat()
            drawCircle(color, radius = size.width * 0.08f, center = Offset(x1, y1))
        }
        
        drawCircle(color, radius = r * 1.3f, style = Stroke(width = 2.5.dp.toPx()))
        drawCircle(color, radius = r * 0.4f)
    }
}

@Composable
fun SearchIcon(modifier: Modifier = Modifier, color: Color = TextSecondary) {
    Canvas(modifier = modifier.size(24.dp)) {
        val r = size.width * 0.22f
        val cx = size.width * 0.42f
        val cy = size.height * 0.42f
        
        drawCircle(color, radius = r, style = Stroke(width = 2.dp.toPx()))
        
        val angle = PI / 4.0
        val x1 = cx + r * cos(angle).toFloat()
        val y1 = cy + r * sin(angle).toFloat()
        val x2 = size.width * 0.82f
        val y2 = size.height * 0.82f
        drawLine(color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun BackIcon(modifier: Modifier = Modifier, color: Color = HoloBlue) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        
        drawLine(color, start = Offset(w * 0.75f, h * 0.5f), end = Offset(w * 0.25f, h * 0.5f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        
        drawLine(color, start = Offset(w * 0.25f, h * 0.5f), end = Offset(w * 0.48f, h * 0.27f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, start = Offset(w * 0.25f, h * 0.5f), end = Offset(w * 0.48f, h * 0.73f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Main Screen
// ═════════════════════════════════════════════════════════════════════════════

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

    // Smooth the needle with a spring for physical feedback
    val animatedDb by animateFloatAsState(
        targetValue = if (isRecording) dbSpl else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dbAnimation"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isRecording) healthAdvice.color else TextDim,
        animationSpec = tween(400),
        label = "statusColor"
    )

    // Pulsing glow for warning/danger states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Scanline animation offset for the LCD digital readout
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MachineDark, Color(0xFF0E0E12), MachineDark)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── 1. HEADER BAR ────────────────────────────────────────────────
        HeaderPanel(onNavigateToSettings = onNavigateToSettings)

        // ── 2. ACTIVE PROFILE STATUS LINE ────────────────────────────────
        Text(
            text = "PROFILE: ${selectedModel.displayName.uppercase()} (OFFSET: ${viewModel.audioEngine.calibrationOffset.roundToInt()} dB)",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            textAlign = TextAlign.Start
        )

        // ── 3. ARC GAUGE (Canvas-drawn) ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
                .machinePanelBackground()
                .padding(2.dp)
        ) {
            ArcGauge(
                dbValue = animatedDb,
                isRecording = isRecording,
                statusColor = statusColor,
                pulseAlpha = pulseAlpha
            )
        }

        // ── 4. LED BAR + DIGITAL READOUT (Side-by-side) ──────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LED Segmented Bar Graph
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .machinePanelBackground()
                    .padding(6.dp)
            ) {
                LedBarGraph(
                    dbValue = animatedDb,
                    isRecording = isRecording
                )
            }

            // Digital Readout Well
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .fillMaxHeight()
                    .machinePanelBackground()
                    .padding(4.dp)
            ) {
                DigitalReadout(
                    dbValue = animatedDb,
                    isRecording = isRecording,
                    statusColor = statusColor,
                    scanlineOffset = scanlineOffset
                )
            }
        }

        // ── 5. STATUS / HEALTH INFO PANEL (Wraps content naturally) ─────
        StatusPanel(
            isRecording = isRecording,
            healthAdvice = healthAdvice,
            statusColor = statusColor,
            pulseAlpha = pulseAlpha,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )

        // ── ERROR MESSAGE ──────────────────────────────────────────────
        errorMsg?.let { msg ->
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

        // ── 6. ENGAGE CONTROL BUTTON ─────────────────────────────────────
        ControlButton(
            isRecording = isRecording,
            onStart = { viewModel.startRecording() },
            onStop = { viewModel.stopRecording() }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Settings Screen
// ═════════════════════════════════════════════════════════════════════════════

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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── HEADER ────────────────────────────────────────────────
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
                BackIcon(color = HoloBlue)
            }
            Text(
                text = "SYSTEM SETTINGS",
                color = HoloBlue,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // ── CALIBRATION SECTION ──────────────────────────────────
        Text(
            text = "AUDIO CALIBRATION",
            style = MaterialTheme.typography.labelLarge,
            color = HoloBlueDim,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .machinePanelBackground()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Phone Calibration Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Selecting your device applies a known hardware offset to provide accurate dBA / dBSPL measurements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                // Dialog Trigger Button (Zero Lag)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MachineDark, CutCornerShape(4.dp))
                        .border(1.5.dp, MachineRidge, CutCornerShape(4.dp))
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
                            text = "SELECT PROFILE",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Manual Slider
                Text(
                    text = "Manual Offset Tuning: ${tempOffset.roundToInt()} dB",
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

        // ── AUTO UPDATES SECTION ─────────────────────────────────
        Text(
            text = "SYSTEM UPDATES",
            style = MaterialTheme.typography.labelLarge,
            color = HoloBlueDim,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .machinePanelBackground()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            text = "Querying latest releases from GitHub...",
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
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "New Version Available: ${state.version}",
                                color = HoloOrange,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
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
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Downloading Update: ${(progress * 100).roundToInt()}%",
                                color = HoloBlue,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                            text = "Launching Android package installer...",
                            color = GaugeGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    is UpdateState.Error -> {
                        val msg = (updateState as UpdateState.Error).message
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Update Error: $msg",
                                color = HoloRed,
                                style = MaterialTheme.typography.bodyMedium
                            )
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

        // ── SYSTEM INFO SECTION ──────────────────────────────────
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
            modifier = Modifier.fillMaxWidth()
        )
    }

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

// ═════════════════════════════════════════════════════════════════════════════
//  Searchable Device Dialog (Fixes Dropdown Lag)
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDeviceDialog(
    currentSelected: PhoneModel,
    onDismiss: () -> Unit,
    onSelect: (PhoneModel) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
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

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search phone models...", color = TextDim) },
                    leadingIcon = { SearchIcon(color = TextSecondary) },
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

// ═════════════════════════════════════════════════════════════════════════════
//  Visual Sub-Components (Gauges, Digital Display, Status)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ArcGauge(
    dbValue: Float,
    isRecording: Boolean,
    statusColor: Color,
    pulseAlpha: Float
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.88f
        val outerRadius = minOf(w, h) * 0.46f
        val innerRadius = outerRadius * 0.72f
        val needleLen   = outerRadius * 0.92f

        val startAngle = 200f
        val sweepAngle = 140f
        val clampedDb  = dbValue.coerceIn(30f, 130f)
        val fraction   = ((clampedDb - 30f) / 100f).coerceIn(0f, 1f)
        val needleAngleDeg = startAngle + fraction * sweepAngle

        // Outer bezel ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MachineRidge, MachineBezel, MachineSurface),
                center = Offset(cx, cy),
                radius = outerRadius * 1.12f
            ),
            radius = outerRadius * 1.08f,
            center = Offset(cx, cy)
        )

        // Dark face
        drawCircle(
            color = LcdBackground,
            radius = outerRadius * 1.02f,
            center = Offset(cx, cy)
        )

        // Coloured arc segments (green → yellow → orange → red)
        val arcStroke = outerRadius - innerRadius
        val segments = listOf(
            Pair(0f to 0.40f, GaugeGreen),
            Pair(0.40f to 0.60f, GaugeYellow),
            Pair(0.60f to 0.80f, GaugeOrange),
            Pair(0.80f to 1.0f, GaugeRed)
        )

        segments.forEach { (range, color) ->
            val (segStart, segEnd) = range
            drawArc(
                color = if (isRecording) color else color.copy(alpha = 0.15f),
                startAngle = startAngle + segStart * sweepAngle,
                sweepAngle = (segEnd - segStart) * sweepAngle,
                useCenter = false,
                topLeft = Offset(cx - outerRadius, cy - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = arcStroke * 0.35f, cap = StrokeCap.Butt)
            )
        }

        // Ticks
        val majorTicks = listOf(30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130)
        majorTicks.forEach { tickDb ->
            val tickFrac = ((tickDb - 30f) / 100f).coerceIn(0f, 1f)
            val angleDeg = startAngle + tickFrac * sweepAngle
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cos = cos(angleRad).toFloat()
            val sin = sin(angleRad).toFloat()

            val outerTick = outerRadius * 0.98f
            val innerTick = outerRadius * 0.88f
            val labelR     = outerRadius * 0.78f

            drawLine(
                color = if (isRecording) TextPrimary else TextDim,
                start = Offset(cx + cos * innerTick, cy + sin * innerTick),
                end = Offset(cx + cos * outerTick, cy + sin * outerTick),
                strokeWidth = if (tickDb % 20 == 0) 3f else 1.5f,
                cap = StrokeCap.Butt
            )

            val style = TextStyle(
                color = if (isRecording) TextSecondary else TextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            val measured = textMeasurer.measure(tickDb.toString(), style)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    cx + cos * labelR - measured.size.width / 2f,
                    cy + sin * labelR - measured.size.height / 2f
                )
            )
        }

        // Minor Ticks
        for (tickDb in 30..130 step 5) {
            if (tickDb % 10 == 0) continue
            val tickFrac = ((tickDb - 30f) / 100f)
            val angleDeg = startAngle + tickFrac * sweepAngle
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cos = cos(angleRad).toFloat()
            val sin = sin(angleRad).toFloat()
            drawLine(
                color = if (isRecording) TextDim else TextDim.copy(alpha = 0.3f),
                start = Offset(cx + cos * outerRadius * 0.93f, cy + sin * outerRadius * 0.93f),
                end = Offset(cx + cos * outerRadius * 0.98f, cy + sin * outerRadius * 0.98f),
                strokeWidth = 1f,
                cap = StrokeCap.Butt
            )
        }

        // Glow behind needle when danger/warning
        if (isRecording && dbValue >= 85f) {
            val needleRad = Math.toRadians(needleAngleDeg.toDouble())
            val tipX = cx + cos(needleRad).toFloat() * needleLen * 0.8f
            val tipY = cy + sin(needleRad).toFloat() * needleLen * 0.8f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(statusColor.copy(alpha = 0.35f * pulseAlpha), Color.Transparent),
                    center = Offset(tipX, tipY),
                    radius = outerRadius * 0.35f
                ),
                radius = outerRadius * 0.35f,
                center = Offset(tipX, tipY)
            )
        }

        // Needle
        if (isRecording) {
            val needleRad = Math.toRadians(needleAngleDeg.toDouble())
            val tipX = cx + cos(needleRad).toFloat() * needleLen
            val tipY = cy + sin(needleRad).toFloat() * needleLen

            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(cx + 2, cy + 2),
                end = Offset(tipX + 2, tipY + 2),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = HoloRedBright,
                start = Offset(cx, cy),
                end = Offset(tipX, tipY),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
            drawCircle(color = HoloRedBright, radius = 4f, center = Offset(tipX, tipY))
        }

        // Center hub
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MachineHighlight, MachineBezel, MachineDark),
                center = Offset(cx, cy),
                radius = 18f
            ),
            radius = 14f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = if (isRecording) HoloRedBright else TextDim,
            radius = 5f,
            center = Offset(cx, cy)
        )

        // Units label
        val unitStyle = TextStyle(
            color = if (isRecording) HoloBlue else TextDim,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        val unitMeasured = textMeasurer.measure("dB SPL", unitStyle)
        drawText(
            textLayoutResult = unitMeasured,
            topLeft = Offset(
                cx - unitMeasured.size.width / 2f,
                cy - outerRadius * 0.35f
            )
        )
    }
}

@Composable
private fun LedBarGraph(
    dbValue: Float,
    isRecording: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize().lcdWell()) {
        val totalSegments = 40
        val segWidth = size.width / totalSegments
        val gap = 2f
        val clampedDb = dbValue.coerceIn(30f, 130f)
        val litSegments = if (isRecording) {
            ((clampedDb - 30f) / 100f * totalSegments).toInt().coerceIn(0, totalSegments)
        } else 0

        for (i in 0 until totalSegments) {
            val frac = i.toFloat() / totalSegments
            val segColor = when {
                frac < 0.40f -> GaugeGreen
                frac < 0.60f -> GaugeYellow
                frac < 0.80f -> GaugeOrange
                else         -> GaugeRed
            }

            val isLit = i < litSegments
            val color = if (isLit) segColor else SegmentOff

            val left = i * segWidth + gap / 2
            val barHeight = size.height * (0.5f + 0.5f * frac)
            val top = (size.height - barHeight) / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(segWidth - gap, barHeight),
                cornerRadius = CornerRadius(1f, 1f)
            )

            if (isLit && frac > 0.6f) {
                drawRoundRect(
                    color = segColor.copy(alpha = 0.3f),
                    topLeft = Offset(left - 1, top - 1),
                    size = Size(segWidth - gap + 2, barHeight + 2),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
private fun DigitalReadout(
    dbValue: Float,
    isRecording: Boolean,
    statusColor: Color,
    scanlineOffset: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .lcdWell()
            .drawBehind {
                val lineSpacing = 4f
                val totalLines = (size.height / lineSpacing).toInt()
                val offset = scanlineOffset * lineSpacing
                for (i in 0..totalLines) {
                    val y = i * lineSpacing + offset
                    if (y <= size.height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.02f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isRecording) "${dbValue.roundToInt()}" else "--",
                style = MaterialTheme.typography.displayMedium,
                color = if (isRecording) statusColor else TextDim,
                textAlign = TextAlign.Center
            )
            Text(
                text = "dB SPL",
                style = MaterialTheme.typography.labelMedium,
                color = if (isRecording) HoloBlue else TextDim,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusPanel(
    isRecording: Boolean,
    healthAdvice: com.example.volumereader.health.HealthAdvice,
    statusColor: Color,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .machinePanelBackground()
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        if (isRecording) {
                            drawCircle(
                                color = statusColor.copy(alpha = 0.4f * pulseAlpha),
                                radius = size.minDimension / 1.5f
                            )
                        }
                        drawCircle(
                            color = if (isRecording) statusColor else TextDim,
                            radius = size.minDimension / 2.8f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.25f),
                            radius = size.minDimension / 8f,
                            center = Offset(size.width * 0.38f, size.height * 0.35f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "● MONITORING" else "○ STANDBY",
                        color = if (isRecording) statusColor else TextDim,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isRecording) statusColor.copy(alpha = 0.15f) else Color.Transparent,
                            shape = CutCornerShape(3.dp)
                        )
                        .border(
                            1.dp,
                            if (isRecording) statusColor.copy(alpha = 0.5f) else MachineRidge,
                            CutCornerShape(3.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRecording) healthAdvice.effect.uppercase() else "INACTIVE",
                        color = if (isRecording) statusColor else TextDim,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, MachineRidge, MachineHighlight, MachineRidge, Color.Transparent)
                        )
                    )
            )

            Text(
                text = if (isRecording) healthAdvice.advice
                       else "Engage acoustic monitoring to begin real-time sound pressure level analysis.",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            if (isRecording) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(LcdBackground, CutCornerShape(2.dp))
                            .border(1.dp, MachineBezel, CutCornerShape(2.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = "NIOSH LIMIT", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Text(text = healthAdvice.nioshLimit, style = MaterialTheme.typography.labelLarge, color = HoloBlue)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(LcdBackground, CutCornerShape(2.dp))
                            .border(1.dp, MachineBezel, CutCornerShape(2.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = "OSHA LIMIT", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Text(text = healthAdvice.oshaLimit, style = MaterialTheme.typography.labelLarge, color = HoloBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderPanel(onNavigateToSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .machinePanelBackground()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(48.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ACOUSTIC SAFETY MONITOR",
                    color = HoloBlue,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "MODEL ASM-7700  ·  REV 2.3",
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(48.dp)
            ) {
                SettingsIcon(color = HoloBlue)
            }
        }
    }
}

@Composable
private fun ControlButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isRecording) HoloRed else HoloBlue,
        animationSpec = tween(300),
        label = "btnBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isRecording) HoloRedBright else HoloBlue,
        animationSpec = tween(300),
        label = "btnText"
    )

    Button(
        onClick = { if (isRecording) onStop() else onStart() },
        shape = CutCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = textColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isRecording) listOf(Color(0xFF2A0A0A), Color(0xFF1A0606))
                             else listOf(Color(0xFF0A1A2A), Color(0xFF060E1A))
                ),
                shape = CutCornerShape(6.dp)
            )
            .border(2.dp, borderColor, CutCornerShape(6.dp))
    ) {
        Text(
            text = if (isRecording) "◼  STOP MONITORING" else "▶  ENGAGE MONITOR",
            style = MaterialTheme.typography.titleLarge,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MachineDark)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val path = Path().apply {
                moveTo(cx, cy - size.height * 0.4f)
                lineTo(cx + size.width * 0.4f, cy + size.height * 0.35f)
                lineTo(cx - size.width * 0.4f, cy + size.height * 0.35f)
                close()
            }
            drawPath(path, color = HoloOrange, style = Stroke(width = 3f, join = StrokeJoin.Miter))
            drawLine(HoloOrange, Offset(cx, cy - 6f), Offset(cx, cy + 4f), strokeWidth = 3f)
            drawCircle(HoloOrange, radius = 2f, center = Offset(cx, cy + 10f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "AUDIO INPUT REQUIRED",
            color = HoloOrange,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Microphone access is required for sound pressure level monitoring. Grant the RECORD_AUDIO permission to proceed.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            shape = CutCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = HoloBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.verticalGradient(colors = listOf(Color(0xFF0A1A2A), Color(0xFF060E1A))),
                    shape = CutCornerShape(6.dp)
                )
                .border(2.dp, HoloBlue, CutCornerShape(6.dp))
        ) {
            Text(
                "AUTHORIZE ACCESS",
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = 2.sp
            )
        }
    }
}

private fun Float.roundToInt(): Int = kotlin.math.roundToInt(this.toDouble()).toInt()
