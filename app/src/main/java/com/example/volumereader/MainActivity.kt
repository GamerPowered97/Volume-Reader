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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volumereader.engine.CalibrationLogic
import com.example.volumereader.engine.PhoneModel
import com.example.volumereader.health.HealthAdvice
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
                    color = Color(0xFF0C0C0E) // Sleek dark workbench background
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
//  Brushed metal background helper extension
// ═════════════════════════════════════════════════════════════════════════════

private fun Modifier.machinePanelBackground(): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(MetalLight, MetalMid, MetalDark, MetalMid)
        ),
        shape = CutCornerShape(6.dp)
    )
    .border(1.5.dp, MachineRidge, CutCornerShape(6.dp))

// ═════════════════════════════════════════════════════════════════════════════
//  Color manipulation helpers for vintage textured look
// ═════════════════════════════════════════════════════════════════════════════

private fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  1. Panel Screws Composable
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PanelScrew(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(13.dp)) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        
        // Dark outer recess bevel
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF88888E), Color(0xFF2C2C30)),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        
        // Shiny metallic screw head surface with highlight reflection
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE5E5EB), Color(0xFF787880), Color(0xFF38383E)),
                start = Offset(cx - r * 0.8f, cy - r * 0.8f),
                end = Offset(cx + r * 0.8f, cy + r * 0.8f)
            ),
            radius = r * 0.8f,
            center = Offset(cx, cy)
        )
        
        // Recessed Phillips/Flathead slots
        drawCircle(
            color = Color(0xFF28282E),
            radius = r * 0.55f,
            center = Offset(cx, cy)
        )
        
        val slotWidth = 2.dp.toPx()
        val slotLength = r * 0.5f
        
        // Phillips slot 1 (45 degrees)
        val angle1 = 45.0
        val rad1 = Math.toRadians(angle1)
        val cos1 = cos(rad1).toFloat()
        val sin1 = sin(rad1).toFloat()
        drawLine(
            color = Color(0xFF0F0F12),
            start = Offset(cx - cos1 * slotLength, cy - sin1 * slotLength),
            end = Offset(cx + cos1 * slotLength, cy + sin1 * slotLength),
            strokeWidth = slotWidth,
            cap = StrokeCap.Round
        )
        
        // Phillips slot 2 (135 degrees)
        val angle2 = 135.0
        val rad2 = Math.toRadians(angle2)
        val cos2 = cos(rad2).toFloat()
        val sin2 = sin(rad2).toFloat()
        drawLine(
            color = Color(0xFF0F0F12),
            start = Offset(cx - cos2 * slotLength, cy - sin2 * slotLength),
            end = Offset(cx + cos2 * slotLength, cy + sin2 * slotLength),
            strokeWidth = slotWidth,
            cap = StrokeCap.Round
        )
        
        // Small metallic shine center dot
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = 1.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  DIY Panel Component (Wraps content with retro metal border and 4 screws)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun DiyPanel(
    modifier: Modifier = Modifier,
    padding: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.machinePanelBackground()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            content()
        }

        // Metallic corner screws
        PanelScrew(modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
        PanelScrew(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        PanelScrew(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp))
        PanelScrew(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  2. Embossed Label Tape
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun EmbossedLabel(
    text: String,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .drawBehind {
                // Drop shadow behind label strip
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        labelColor.lighten(0.2f),
                        labelColor,
                        labelColor.darken(0.3f)
                    )
                ),
                shape = CutCornerShape(1.dp) // Vintage tape look (rough-cut plastic tape)
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CutCornerShape(1.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Shadow behind white embossed text
            Text(
                text = text,
                color = Color.Black.copy(alpha = 0.45f),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.offset(x = 1.dp, y = 1.dp)
            )
            // Raised embossed white lettering
            Text(
                text = text,
                color = Color.White,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  3. Retro Metal Toggle
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun HardwareToggleSwitch(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedLeverPos by animateFloatAsState(
        targetValue = if (isOn) -1f else 1f, // -1f is UP (ON), 1f is DOWN (OFF)
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh // springy mechanical snap
        ),
        label = "leverPosition"
    )

    Box(
        modifier = modifier
            .size(width = 65.dp, height = 75.dp)
            .clickable { onToggle(!isOn) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            
            // 1. Metal mounting plate bezel
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF55555A), Color(0xFF2B2B2F), Color(0xFF1E1E22))
                ),
                size = size,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.4f),
                size = size,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Miniature screws holding the plate
            drawCircle(Color(0xFF1A1A1E), radius = 1.5.dp.toPx(), center = Offset(5.dp.toPx(), 5.dp.toPx()))
            drawCircle(Color(0xFF1A1A1E), radius = 1.5.dp.toPx(), center = Offset(w - 5.dp.toPx(), 5.dp.toPx()))
            drawCircle(Color(0xFF1A1A1E), radius = 1.5.dp.toPx(), center = Offset(5.dp.toPx(), h - 5.dp.toPx()))
            drawCircle(Color(0xFF1A1A1E), radius = 1.5.dp.toPx(), center = Offset(w - 5.dp.toPx(), h - 5.dp.toPx()))

            // 2. Circular metallic base collar
            val collarRadius = minOf(w, h) * 0.28f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9E9EA4), Color(0xFF4C4C50), Color(0xFF2A2A2E)),
                    center = Offset(cx, cy),
                    radius = collarRadius
                ),
                radius = collarRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF0F0F12),
                radius = collarRadius * 0.82f,
                center = Offset(cx, cy)
            )
            
            // 3. Lever slot (dark well)
            val slotW = collarRadius * 0.38f
            val slotH = collarRadius * 1.15f
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(cx - slotW / 2f, cy - slotH / 2f),
                size = Size(slotW, slotH),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )

            // 4. Lever tip position
            val maxTravel = slotH * 0.72f
            val leverTipY = cy + (animatedLeverPos * maxTravel)
            val leverTipX = cx
            
            // Lever shadow
            drawLine(
                color = Color.Black.copy(alpha = 0.6f),
                start = Offset(cx + 3f, cy + 3f),
                end = Offset(leverTipX + 5f, leverTipY + 5f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Metal lever shaft
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEEEEEE), Color(0xFF9E9EA4), Color(0xFF44444A))
                ),
                start = Offset(cx, cy),
                end = Offset(leverTipX, leverTipY),
                strokeWidth = 4.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Red lever ball tip
            val tipRadius = collarRadius * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFFB71C1C), Color(0xFF5D0000)),
                    center = Offset(leverTipX - tipRadius * 0.2f, leverTipY - tipRadius * 0.2f),
                    radius = tipRadius
                ),
                radius = tipRadius,
                center = Offset(leverTipX, leverTipY)
            )
            
            // Glare highlight on red ball
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = tipRadius * 0.25f,
                center = Offset(leverTipX - tipRadius * 0.3f, leverTipY - tipRadius * 0.3f)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  5. Glow Lamps
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun GlowLamp(
    isOn: Boolean,
    glowColor: Color,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(30.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.38f
        
        // 1. Metal socket holder ring (chassis mount)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC0C0C6), Color(0xFF6C6C72), Color(0xFF2A2A2E)),
                center = Offset(cx, cy),
                radius = r * 1.25f
            ),
            radius = r * 1.15f,
            center = Offset(cx, cy)
        )
        
        // Outer dark slot
        drawCircle(
            color = Color(0xFF0C0C0F),
            radius = r * 0.98f,
            center = Offset(cx, cy)
        )
        
        // 2. Diffuse phosphor bloom glow (behind bulb dome)
        if (isOn) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.65f * pulseAlpha),
                        glowColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 2.3f
                ),
                radius = r * 2.3f,
                center = Offset(cx, cy)
            )
        }
        
        // 3. Colored glass dome base
        val baseGlassColor = if (isOn) glowColor.darken(0.35f) else Color(0xFF1E1E22)
        drawCircle(
            color = baseGlassColor,
            radius = r * 0.88f,
            center = Offset(cx, cy)
        )
        
        // 4. Filament (vintage carbon curve inside dome)
        val filamentPath = Path().apply {
            moveTo(cx - r * 0.32f, cy + r * 0.35f)
            quadraticTo(cx - r * 0.32f, cy - r * 0.28f, cx, cy - r * 0.28f)
            quadraticTo(cx + r * 0.32f, cy - r * 0.28f, cx + r * 0.32f, cy + r * 0.35f)
        }
        val filamentColor = if (isOn) Color(0xFFFFD54F) else Color(0xFF505055)
        drawPath(
            path = filamentPath,
            color = filamentColor,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Filament center spark (when on)
        if (isOn) {
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = Offset(cx, cy - r * 0.18f)
            )
        }
        
        // 5. Spherical glass reflection highlighting
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.0f)),
                center = Offset(cx - r * 0.28f, cy - r * 0.28f),
                radius = r * 0.45f
            ),
            radius = r * 0.65f,
            center = Offset(cx - r * 0.28f, cy - r * 0.28f)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Custom Canvas-drawn Industrial Icons
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

    // Scanline animation offset for the CRT digital readout
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080B)) // Vintage laboratory desk dark background
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MetalLight, MetalMid, MetalDark, MetalMid)
                    ),
                    shape = CutCornerShape(10.dp)
                )
                .border(3.dp, MachineRidge, CutCornerShape(10.dp))
                .shadow(18.dp, shape = CutCornerShape(10.dp), clip = false)
        ) {
            // Main Chassis corners panel screws
            PanelScrew(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── 1. HEADER BAR ────────────────────────────────────────────────
                HeaderPanel(onNavigateToSettings = onNavigateToSettings)

                // ── 2. ACTIVE PROFILE STATUS LINE ────────────────────────────────
                Text(
                    text = "PROFILE: ${selectedModel.displayName.uppercase()} (OFFSET: ${viewModel.audioEngine.calibrationOffset.roundToInt()} dB)",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textAlign = TextAlign.Start
                )

                // ── 3. ARC GAUGE (CRT Style) ─────────────────────────────────────
                DiyPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.55f),
                    padding = 8.dp
                ) {
                    ArcGauge(
                        dbValue = animatedDb,
                        isRecording = isRecording,
                        statusColor = statusColor,
                        pulseAlpha = pulseAlpha
                    )
                }

                // ── 4. LED BAR + DIGITAL READOUT (Side-by-side CRT & LED) ────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LED Segmented Bar Graph
                    DiyPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        padding = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            EmbossedLabel(text = "ACOUSTIC SENSOR", labelColor = Color(0xFF0E3814))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
                                LedBarGraph(
                                    dbValue = animatedDb,
                                    isRecording = isRecording
                                )
                            }
                        }
                    }

                    // Digital Readout (CRT grid styling)
                    DiyPanel(
                        modifier = Modifier
                            .width(135.dp)
                            .fillMaxHeight(),
                        padding = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            EmbossedLabel(text = "LEVEL dB", labelColor = Color(0xFF1E1E22))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
                                DigitalReadout(
                                    dbValue = animatedDb,
                                    isRecording = isRecording,
                                    statusColor = statusColor,
                                    scanlineOffset = scanlineOffset
                                )
                            }
                        }
                    }
                }

                // ── 5. STATUS / HEALTH INFO PANEL ───────────────────────────────
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
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LcdBackground, CutCornerShape(2.dp))
                            .border(1.dp, HoloRed.copy(alpha = 0.4f), CutCornerShape(2.dp))
                            .padding(8.dp)
                    )
                }

                // ── 6. ENGAGE CONTROL TOGGLE ─────────────────────────────────────
                DiyPanel(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EmbossedLabel(text = "POWER / MONITOR TRIGGER", labelColor = Color(0xFF15151A))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "STANDBY",
                                color = if (!isRecording) TextPrimary else TextDim,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            
                            HardwareToggleSwitch(
                                isOn = isRecording,
                                onToggle = { isOn ->
                                    if (isOn) {
                                        viewModel.startRecording()
                                    } else {
                                        viewModel.stopRecording()
                                    }
                                }
                            )
                            
                            Text(
                                text = "MONITOR",
                                color = if (isRecording) HoloGreen else TextDim,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }
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
    val autoDetectStatus by viewModel.autoDetectStatus.collectAsStateWithLifecycle()
    
    var showDialog by remember { mutableStateOf(false) }
    var tempOffset by remember { mutableStateOf(viewModel.audioEngine.calibrationOffset) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MetalLight, MetalMid, MetalDark, MetalMid)
                    ),
                    shape = CutCornerShape(10.dp)
                )
                .border(3.dp, MachineRidge, CutCornerShape(10.dp))
                .shadow(18.dp, shape = CutCornerShape(10.dp), clip = false)
        ) {
            // Settings Chassis corners panel screws
            PanelScrew(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── HEADER ────────────────────────────────────────────────
                DiyPanel(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.clearAutoDetectStatus()
                            onNavigateToMain()
                        }) {
                            BackIcon(color = HoloBlue)
                        }
                        EmbossedLabel(text = "SYSTEM SETTINGS", labelColor = Color(0xFF1E1E22))
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // ── CALIBRATION SECTION ──────────────────────────────────
                EmbossedLabel(
                    text = "AUDIO CALIBRATION",
                    labelColor = Color(0xFF0E3814),
                    modifier = Modifier.fillMaxWidth()
                )

                DiyPanel(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 10.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Phone Calibration Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selecting your device applies a known hardware offset to provide accurate measurements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MachineDark, CutCornerShape(4.dp))
                                .border(1.5.dp, MachineRidge, CutCornerShape(4.dp))
                                .clickable { showDialog = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedModel.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HoloBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "SELECT PROFILE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Auto-Detect Profile
                        Button(
                            onClick = {
                                viewModel.autoDetectDevice()
                                tempOffset = viewModel.audioEngine.calibrationOffset
                            },
                            shape = CutCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MachineDark, contentColor = HoloBlue),
                            modifier = Modifier.fillMaxWidth().border(1.dp, HoloBlue, CutCornerShape(4.dp))
                        ) {
                            Text("AUTO-DETECT PROFILE", fontFamily = FontFamily.Monospace)
                        }

                        autoDetectStatus?.let { status ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LcdBackground, CutCornerShape(2.dp))
                                    .border(1.dp, LcdBezel, CutCornerShape(2.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = status,
                                    color = if (status.startsWith("SUCCESS")) GaugeGreen else HoloOrange,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Manual Offset Tuning: ${tempOffset.roundToInt()} dB",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
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
                            text = "Fine-tune the dB offset if you have a calibrated decibel meter for reference.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // ── AUTO UPDATES SECTION ─────────────────────────────────
                EmbossedLabel(
                    text = "SYSTEM UPDATES",
                    labelColor = Color(0xFF0E3814),
                    modifier = Modifier.fillMaxWidth()
                )

                DiyPanel(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 10.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current App Version",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = viewModel.currentVersion,
                                style = MaterialTheme.typography.titleMedium,
                                color = HoloBlue,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
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
                                    Text("CHECK FOR UPDATES", fontFamily = FontFamily.Monospace)
                                }
                            }
                            is UpdateState.Checking -> {
                                Text(
                                    text = "Querying latest releases from GitHub...",
                                    color = HoloBlue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            is UpdateState.UpToDate -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Your app is up to date!",
                                        color = GaugeGreen,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    TextButton(onClick = { viewModel.resetUpdateState() }) {
                                        Text("OK", color = HoloBlue, fontFamily = FontFamily.Monospace)
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
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Button(
                                        onClick = { viewModel.downloadAndInstallUpdate(state.downloadUrl) },
                                        shape = CutCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MachineDark, contentColor = HoloOrange),
                                        modifier = Modifier.fillMaxWidth().border(1.dp, HoloOrange, CutCornerShape(4.dp))
                                    ) {
                                        Text("DOWNLOAD & INSTALL UPDATE", fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            is UpdateState.Downloading -> {
                                val progress = (updateState as UpdateState.Downloading).progress
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Downloading Update: ${(progress * 100).roundToInt()}%",
                                        color = HoloBlue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace
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
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            is UpdateState.Error -> {
                                val msg = (updateState as UpdateState.Error).message
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Update Error: $msg",
                                        color = HoloRed,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Button(
                                        onClick = { viewModel.checkForUpdates() },
                                        shape = CutCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MachineDark, contentColor = HoloRed),
                                        modifier = Modifier.fillMaxWidth().border(1.dp, HoloRed, CutCornerShape(4.dp))
                                    ) {
                                        Text("RETRY UPDATE CHECK", fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── SYSTEM INFO SECTION ──────────────────────────────────
                Text(
                    text = "BUILD MANUFACTURER: ${Build.MANUFACTURER}\nBUILD MODEL: ${Build.MODEL}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
//  Searchable Device Dialog
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MetalLight, MetalMid, MetalDark, MetalMid)
                    ),
                    shape = CutCornerShape(10.dp)
                )
                .border(3.dp, MachineRidge, CutCornerShape(10.dp))
                .shadow(18.dp, shape = CutCornerShape(10.dp), clip = false)
        ) {
            PanelScrew(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 20.dp)
            ) {
                EmbossedLabel(
                    text = "SELECT DEVICE PROFILE",
                    labelColor = Color(0xFF1E1E22),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search phone models...", color = TextDim, fontFamily = FontFamily.Monospace) },
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
                    shape = CutCornerShape(4.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace)
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
                                color = if (model == currentSelected) HoloBlue else TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (model != CalibrationLogic.defaultModel) {
                                Text(
                                    text = "offset: ${model.offset.roundToInt()} dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
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
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
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
                    Text("CLOSE", fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Visual Sub-Components
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

        // CRT Olive Green background screen
        val crtBgColor = Color(0xFF091C0E)
        drawRoundRect(
            color = crtBgColor,
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        
        // CRT grid spacing
        val gridSpacing = 16.dp.toPx()
        val gridColor = Color(0xFF13361A) // faint green coordinates grid
        
        var x = 0f
        while (x < size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += gridSpacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += gridSpacing
        }

        // Faint circular target/scope coordinates on the screen
        drawCircle(
            color = gridColor,
            radius = outerRadius * 0.6f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
        )
        
        // CRT glass shine diagonal gradient overlay
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.04f), Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
        
        // Inner shadow edge bevel for CRT screen
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.4f),
            style = Stroke(width = 1.5.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
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
                color = if (isRecording) color else color.copy(alpha = 0.12f),
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
            val labelR     = outerRadius * 0.76f

            drawLine(
                color = if (isRecording) Color(0xFF39FF14) else Color(0xFF13361A),
                start = Offset(cx + cos * innerTick, cy + sin * innerTick),
                end = Offset(cx + cos * outerTick, cy + sin * outerTick),
                strokeWidth = if (tickDb % 20 == 0) 3f else 1.5f,
                cap = StrokeCap.Butt
            )

            val style = TextStyle(
                color = if (isRecording) Color(0xFF39FF14) else Color(0xFF13361A),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
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
                color = if (isRecording) Color(0xFF39FF14).copy(alpha = 0.5f) else Color(0xFF13361A).copy(alpha = 0.2f),
                start = Offset(cx + cos * outerRadius * 0.93f, cy + sin * outerRadius * 0.93f),
                end = Offset(cx + cos * outerRadius * 0.98f, cy + sin * outerRadius * 0.98f),
                strokeWidth = 1f,
                cap = StrokeCap.Butt
            )
        }

        // Phosphor danger glow behind needle when levels are high
        if (isRecording && dbValue >= 85f) {
            val needleRad = Math.toRadians(needleAngleDeg.toDouble())
            val tipX = cx + cos(needleRad).toFloat() * needleLen * 0.8f
            val tipY = cy + sin(needleRad).toFloat() * needleLen * 0.8f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(statusColor.copy(alpha = 0.3f * pulseAlpha), Color.Transparent),
                    center = Offset(tipX, tipY),
                    radius = outerRadius * 0.35f
                ),
                radius = outerRadius * 0.35f,
                center = Offset(tipX, tipY)
            )
        }

        // Physical Amber Needle
        if (isRecording) {
            val needleRad = Math.toRadians(needleAngleDeg.toDouble())
            val tipX = cx + cos(needleRad).toFloat() * needleLen
            val tipY = cy + sin(needleRad).toFloat() * needleLen

            // Needle shadow on phosphor screen
            drawLine(
                color = Color.Black.copy(alpha = 0.45f),
                start = Offset(cx + 2.5f, cy + 2.5f),
                end = Offset(tipX + 2.5f, tipY + 2.5f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
            // Glowing neon amber needle
            drawLine(
                color = Color(0xFFFFB300), 
                start = Offset(cx, cy),
                end = Offset(tipX, tipY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawCircle(color = Color(0xFFFFB300), radius = 3.5f, center = Offset(tipX, tipY))
        }

        // Center needle hub (metal cover)
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
            color = if (isRecording) Color(0xFFFFB300) else Color(0xFF13361A),
            radius = 4.5f,
            center = Offset(cx, cy)
        )

        // CRT Scope Center grid line labels
        val unitStyle = TextStyle(
            color = if (isRecording) Color(0xFF39FF14) else Color(0xFF13361A),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        val unitMeasured = textMeasurer.measure("dB SPL", unitStyle)
        drawText(
            textLayoutResult = unitMeasured,
            topLeft = Offset(
                cx - unitMeasured.size.width / 2f,
                cy - outerRadius * 0.38f
            )
        )
    }
}

@Composable
private fun LedBarGraph(
    dbValue: Float,
    isRecording: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF040606), CutCornerShape(3.dp)).border(1.dp, Color(0xFF121616), CutCornerShape(3.dp))) {
        val totalSegments = 30
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
            val barHeight = size.height * (0.45f + 0.55f * frac)
            val top = (size.height - barHeight) / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(segWidth - gap, barHeight),
                cornerRadius = CornerRadius(1f, 1f)
            )

            // Segment glow
            if (isLit && (frac > 0.5f || isLit)) {
                drawRoundRect(
                    color = segColor.copy(alpha = 0.25f),
                    topLeft = Offset(left - 1, top - 1),
                    size = Size(segWidth - gap + 2, barHeight + 2),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
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
            .background(Color(0xFF091D0E), CutCornerShape(3.dp)) // CRT olive green window
            .border(1.dp, Color(0xFF13361A), CutCornerShape(3.dp))
            .drawBehind {
                // Coordinate lines every 15 pixels
                val gridSpacing = 15f
                val gridColor = Color(0xFF112D18)
                
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += gridSpacing
                }
                
                // CRT curved glare
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.03f), Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
                
                // Scanline visual effect
                val lineSpacing = 3f
                val totalLines = (size.height / lineSpacing).toInt()
                val offset = scanlineOffset * lineSpacing
                for (i in 0..totalLines) {
                    val ly = i * lineSpacing + offset
                    if (ly <= size.height) {
                        drawLine(
                            color = Color(0xFF39FF14).copy(alpha = 0.03f),
                            start = Offset(0f, ly),
                            end = Offset(size.width, ly),
                            strokeWidth = 0.8f
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
            // Neon Green phosphor digit color
            Text(
                text = if (isRecording) "${dbValue.roundToInt()}" else "--",
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) Color(0xFF39FF14) else Color(0xFF133C1A),
                textAlign = TextAlign.Center
            )
            Text(
                text = "dB SPL",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) Color(0xFF39FF14).copy(alpha = 0.7f) else Color(0xFF133C1A),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusPanel(
    isRecording: Boolean,
    healthAdvice: HealthAdvice,
    statusColor: Color,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    DiyPanel(
        modifier = modifier,
        padding = 10.dp
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
                    // Custom filamental glowing lamp
                    GlowLamp(
                        isOn = isRecording,
                        glowColor = statusColor,
                        pulseAlpha = pulseAlpha
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "MONITORING" else "STANDBY",
                        color = if (isRecording) statusColor else TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                // Embossed Tape status indicator
                if (isRecording) {
                    val labelColor = when {
                        dbValueGe(healthAdvice) -> Color(0xFF8C1D18)  // Warning (Red)
                        dbValueCaution(healthAdvice) -> Color(0xFF8F5E00) // Caution (Amber)
                        else -> Color(0xFF0E3814) // Safe (Green)
                    }
                    val labelText = when {
                        dbValueGe(healthAdvice) -> "WARNING"
                        dbValueCaution(healthAdvice) -> "CAUTION"
                        else -> "SAFE"
                    }
                    EmbossedLabel(text = labelText, labelColor = labelColor)
                } else {
                    EmbossedLabel(text = "STANDBY", labelColor = Color(0xFF2E2E32))
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
                       else "Engage monitoring via the lever switch below to start real-time acoustic analysis.",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
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
                            .background(Color(0xFF060909), CutCornerShape(2.dp))
                            .border(1.dp, Color(0xFF131718), CutCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "NIOSH LIMIT", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextDim)
                        Text(text = healthAdvice.nioshLimit, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = HoloBlue)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF060909), CutCornerShape(2.dp))
                            .border(1.dp, Color(0xFF131718), CutCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "OSHA LIMIT", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextDim)
                        Text(text = healthAdvice.oshaLimit, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = HoloBlue)
                    }
                }
            }
        }
    }
}

// Helpers for Status Tape
private fun dbValueGe(advice: HealthAdvice): Boolean {
    return advice.effect.contains("danger", ignoreCase = true) || advice.effect.contains("severe", ignoreCase = true) || advice.color == StatusDanger
}

private fun dbValueCaution(advice: HealthAdvice): Boolean {
    return advice.effect.contains("caution", ignoreCase = true) || advice.effect.contains("moderate", ignoreCase = true) || advice.color == StatusCaution
}

@Composable
private fun HeaderPanel(onNavigateToSettings: () -> Unit) {
    DiyPanel(
        modifier = Modifier.fillMaxWidth(),
        padding = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(40.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmbossedLabel(
                    text = "EMF SURVEY METER",
                    labelColor = Color(0xFF16161C)
                )
                Text(
                    text = "MODEL ASM-7700  ·  REV 2.4.1",
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(40.dp)
            ) {
                SettingsIcon(color = HoloBlue)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Permission Screen
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080B))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MetalLight, MetalMid, MetalDark, MetalMid)
                    ),
                    shape = CutCornerShape(10.dp)
                )
                .border(3.dp, MachineRidge, CutCornerShape(10.dp))
                .shadow(16.dp, shape = CutCornerShape(10.dp), clip = false)
        ) {
            PanelScrew(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
            PanelScrew(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
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

                EmbossedLabel(text = "ACCESS REQUIRED", labelColor = Color(0xFF8C1D18))
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Microphone access is required for sound pressure level monitoring. Grant the RECORD_AUDIO permission to proceed.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
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
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
