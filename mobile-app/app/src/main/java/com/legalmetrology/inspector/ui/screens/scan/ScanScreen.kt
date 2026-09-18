package com.legalmetrology.inspector.ui.screens.scan

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.legalmetrology.inspector.camera.ArucoResult
import com.legalmetrology.inspector.camera.ArucoScaleAnalyzer
import com.legalmetrology.inspector.ui.theme.Amber500
import com.legalmetrology.inspector.ui.theme.ArGlassPanel
import com.legalmetrology.inspector.ui.theme.ArReticleTint
import com.legalmetrology.inspector.ui.theme.ArSearchTint
import com.legalmetrology.inspector.ui.theme.Emerald500
import com.legalmetrology.inspector.ui.theme.Indigo500
import com.legalmetrology.inspector.ui.theme.Navy900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

private const val TAG = "ScanScreen"

/**
 * ScanScreen — ArUco marker-based scanning with perspective-tilt protection.
 *
 * ## State machine
 * ```
 *  ┌────────────┐  marker visible       ┌─────────────┐
 *  │  Searching │─────(geometry ok)────▶│   Locked    │
 *  └────────────┘                       └─────────────┘
 *        ▲              marker visible        │ marker lost
 *        │          ──(geometry bad)──▶  ┌──────────┐
 *        │          ◀──(500ms debounce)──│  Tilted  │
 *        │                               └──────────┘
 *        └──────────────(500ms debounce)──────────────
 * ```
 *
 * The **500 ms debounce** on both Locked→Searching and Tilted→Searching
 * prevents the reticle from flickering due to micro hand-tremors.
 *
 * ## CameraX use cases
 * | Use case       | Purpose                                    |
 * |----------------|--------------------------------------------|
 * | Preview        | Real-time viewfinder via PreviewView       |
 * | ImageAnalysis  | ArUco detection at 30 fps (latest-frame)   |
 * | ImageCapture   | High-quality JPEG on shutter press         |
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    packageType: String,
    category: String,
    productName: String,
    onProceedToReview: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // ── Scan state machine ───────────────────────────────────
    var scanState by remember { mutableStateOf<ScanState>(ScanState.Searching) }
    var photosCaptures by remember { mutableIntStateOf(0) }
    val maxPhotos = 3

    // ── CameraX handles ──────────────────────────────────────
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // ── Latest result from the analyzer thread ───────────────
    // Written from the CameraX background thread; read on the main thread.
    // Using mutableStateOf so Compose re-reads it automatically.
    var latestResult by remember { mutableStateOf<ArucoResult>(ArucoResult.NotFound) }

    // ── Debounced state transitions ──────────────────────────
    // When the analyzer reports NotFound or Tilted we don't immediately drop
    // a Valid lock — we wait 500 ms first.  This smooths out micro-occlusions
    // and hand-tremors without making the UI feel sluggish.
    LaunchedEffect(latestResult) {
        when (val result = latestResult) {

            is ArucoResult.Valid -> {
                // Immediate promotion to Locked; update mmPerPixel live.
                if (scanState is ScanState.Searching || scanState is ScanState.Tilted) {
                    vibrate(context)
                }
                scanState = ScanState.Locked(result.mmPerPixel)
            }

            is ArucoResult.Tilted -> {
                if (scanState is ScanState.Locked) {
                    // Give the user 500 ms to correct the angle before we drop lock.
                    delay(500L)
                }
                // Only transition if the result hasn't changed back to Valid.
                if (latestResult is ArucoResult.Tilted) {
                    scanState = ScanState.Tilted
                }
            }

            is ArucoResult.NotFound -> {
                if (scanState is ScanState.Locked || scanState is ScanState.Tilted) {
                    delay(500L)
                }
                if (latestResult is ArucoResult.NotFound) {
                    scanState = ScanState.Searching
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // ── Capture logic ────────────────────────────────────────
    fun capturePhoto() {
        val capture = imageCapture ?: return
        val lockedState = scanState as? ScanState.Locked ?: return

        scope.launch {
            vibrate(context)
            scanState = ScanState.Captured

            val photoFile = File(
                context.cacheDir,
                "inspection_photo_${photosCaptures}_${System.currentTimeMillis()}.jpg"
            )
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d(
                            TAG,
                            "Captured photo ${photosCaptures + 1}/$maxPhotos " +
                                "— ${photoFile.name}, " +
                                "mmPerPx=${lockedState.mmPerPixel}"
                        )
                        scope.launch {
                            photosCaptures++
                            delay(800L)
                            if (photosCaptures >= maxPhotos) {
                                onProceedToReview(UUID.randomUUID().toString())
                            } else {
                                // Re-enter locked state if marker still valid.
                                val currentResult = latestResult
                                scanState = if (currentResult is ArucoResult.Valid) {
                                    ScanState.Locked(currentResult.mmPerPixel)
                                } else {
                                    ScanState.Searching
                                }
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        scanState = ScanState.Searching
                    }
                }
            )
        }
    }

    // ── Root layout ──────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Navy900)) {
        if (!cameraPermission.status.isGranted) {
            CameraPermissionRequest(
                onRequestPermission = { cameraPermission.launchPermissionRequest() }
            )
        } else {

            // ── CameraX PreviewView ──────────────────────────
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { preview ->
                        previewView = preview

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Preview
                            val previewUseCase = Preview.Builder().build().also {
                                it.setSurfaceProvider(preview.surfaceProvider)
                            }

                            // ImageAnalysis — single background thread, drop old frames
                            val analysisUseCase = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(
                                        Executors.newSingleThreadExecutor(),
                                        ArucoScaleAnalyzer { result ->
                                            // Callback is on the analyzer thread — update
                                            // mutableStateOf (thread-safe for Compose).
                                            latestResult = result
                                        }
                                    )
                                }

                            // ImageCapture
                            val imageCaptureUseCase = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()
                            imageCapture = imageCaptureUseCase

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    previewUseCase,
                                    analysisUseCase,
                                    imageCaptureUseCase
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Camera binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Rule-of-thirds guide lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val thirdW = size.width / 3f
                val thirdH = size.height / 3f
                val lineColor = Color.White.copy(alpha = 0.08f)
                drawLine(lineColor, Offset(thirdW, 0f),          Offset(thirdW, size.height),      1f)
                drawLine(lineColor, Offset(thirdW * 2, 0f),      Offset(thirdW * 2, size.height),  1f)
                drawLine(lineColor, Offset(0f, thirdH),          Offset(size.width, thirdH),        1f)
                drawLine(lineColor, Offset(0f, thirdH * 2),      Offset(size.width, thirdH * 2),    1f)
            }

            // Reticle + tilt warning overlay
            ArScanOverlay(scanState = scanState)

            // Top status bar
            TopHud(
                packageType = packageType,
                category = category,
                productName = productName,
                scanState = scanState,
                onBack = onBack
            )

            // Bottom shutter + progress
            BottomControls(
                scanState = scanState,
                photosCaptures = photosCaptures,
                maxPhotos = maxPhotos,
                onCapture = ::capturePhoto,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewView?.let {
                val future = ProcessCameraProvider.getInstance(context)
                if (future.isDone) future.get().unbindAll()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Scan State
// ─────────────────────────────────────────────────────────────

/**
 * UI-level state for the scan screen.  Note that this is *separate* from
 * [ArucoResult]: the UI state is debounced and drives animations, whereas
 * [ArucoResult] is the raw per-frame output of the analyzer.
 */
sealed class ScanState {
    /** No marker in frame — show pulsing indigo reticle. */
    object Searching : ScanState()

    /**
     * Marker detected but geometry fails perspective check — show amber
     * warning reticle and tilt-correction hint.  Capture is blocked.
     */
    object Tilted : ScanState()

    /**
     * Marker detected and geometry is good — show green lock ring and
     * enable the shutter button.
     *
     * @param mmPerPixel Live-updated scale ratio.
     */
    data class Locked(val mmPerPixel: Double) : ScanState()

    /** Shutter pressed — brief white-flash animation then advance. */
    object Captured : ScanState()
}

// ─────────────────────────────────────────────────────────────
// AR Scan Overlay
// ─────────────────────────────────────────────────────────────

@Composable
private fun ArScanOverlay(scanState: ScanState) {
    val infiniteTransition = rememberInfiniteTransition(label = "ar_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Amber pulse for the Tilted state — slightly slower for a "warning" feel.
    val tiltPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt_pulse_scale"
    )

    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "scan_rotation"
    )

    val reticleColor = when (scanState) {
        is ScanState.Locked   -> ArReticleTint   // green
        is ScanState.Captured -> Emerald500       // green flash
        is ScanState.Tilted   -> Amber500         // amber warning
        else                  -> ArSearchTint     // indigo searching
    }

    val reticleAlpha by animateFloatAsState(
        targetValue = if (scanState is ScanState.Captured) 0.4f else 1f,
        animationSpec = tween(300),
        label = "reticle_alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        // ── Outer pulse ring — Searching ────────────────────
        if (scanState is ScanState.Searching) {
            Canvas(modifier = Modifier.size(220.dp).alpha(0.4f)) {
                drawCircle(
                    color = ArSearchTint,
                    radius = size.minDimension / 2f * pulseScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // ── Outer pulse ring — Tilted (amber, faster) ───────
        if (scanState is ScanState.Tilted) {
            Canvas(modifier = Modifier.size(220.dp).alpha(0.5f)) {
                drawCircle(
                    color = Amber500,
                    radius = size.minDimension / 2f * tiltPulseScale,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        // ── Corner-bracket reticle (all states) ─────────────
        Canvas(
            modifier = Modifier.size(180.dp).alpha(reticleAlpha)
        ) {
            val cornerLen   = size.width * 0.2f
            val strokeWidth = 3.dp.toPx()
            val p           = 0f   // padding

            // Top-left
            drawLine(reticleColor, Offset(p, p), Offset(p + cornerLen, p), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(p, p), Offset(p, p + cornerLen), strokeWidth, StrokeCap.Round)
            // Top-right
            drawLine(reticleColor, Offset(size.width - p, p), Offset(size.width - p - cornerLen, p), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(size.width - p, p), Offset(size.width - p, p + cornerLen), strokeWidth, StrokeCap.Round)
            // Bottom-left
            drawLine(reticleColor, Offset(p, size.height - p), Offset(p + cornerLen, size.height - p), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(p, size.height - p), Offset(p, size.height - p - cornerLen), strokeWidth, StrokeCap.Round)
            // Bottom-right
            drawLine(reticleColor, Offset(size.width - p, size.height - p), Offset(size.width - p - cornerLen, size.height - p), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(size.width - p, size.height - p), Offset(size.width - p, size.height - p - cornerLen), strokeWidth, StrokeCap.Round)

            // Scanning arc (Searching only)
            if (scanState is ScanState.Searching) {
                drawArc(
                    color = ArSearchTint.copy(alpha = 0.6f),
                    startAngle = scanRotation, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(p + strokeWidth, p + strokeWidth),
                    size = Size(
                        size.width  - p * 2 - strokeWidth * 2,
                        size.height - p * 2 - strokeWidth * 2
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // ── Solid lock ring (Locked) ─────────────────────────
        if (scanState is ScanState.Locked) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawCircle(color = ArReticleTint.copy(alpha = 0.15f), radius = size.minDimension / 2f)
                drawCircle(color = ArReticleTint, radius = size.minDimension / 2f, style = Stroke(width = 2.dp.toPx()))
            }
        }

        // ── Tilt warning banner ──────────────────────────────
        if (scanState is ScanState.Tilted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 220.dp)      // sit below the reticle
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Amber500.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Amber500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Marker tilted! Hold phone flat\nand parallel to the label.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ── Capture flash (Captured) ─────────────────────────
        AnimatedVisibility(
            visible = scanState is ScanState.Captured,
            enter = fadeIn(tween(50)),
            exit  = fadeOut(tween(600))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Top HUD
// ─────────────────────────────────────────────────────────────

@Composable
private fun TopHud(
    packageType: String,
    category: String,
    productName: String,
    scanState: ScanState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(ArGlassPanel, CircleShape).size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            // Package type · category badge
            Card(
                colors = CardDefaults.cardColors(containerColor = ArGlassPanel),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        packageType.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Text(
                        " · $category",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Status pill — colour-coded per state
            val pillBackground = when (scanState) {
                is ScanState.Locked   -> ArReticleTint.copy(alpha = 0.2f)
                is ScanState.Captured -> Emerald500.copy(alpha = 0.2f)
                is ScanState.Tilted   -> Amber500.copy(alpha = 0.2f)
                else                  -> ArGlassPanel
            }
            val dotColor = when (scanState) {
                is ScanState.Locked   -> ArReticleTint
                is ScanState.Captured -> Emerald500
                is ScanState.Tilted   -> Amber500
                else                  -> Color.Yellow
            }
            val pillLabel = when (scanState) {
                is ScanState.Locked   -> "Marker Locked"
                is ScanState.Captured -> "Captured"
                is ScanState.Tilted   -> "Tilted"
                else                  -> "Searching..."
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = pillBackground),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(pillLabel, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        // Product name card (always visible)
        if (productName.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ArGlassPanel),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Product",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.6f)
                    )
                    Text(
                        productName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Scale readout (Locked state only)
        if (scanState is ScanState.Locked) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ArGlassPanel),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    MetricReadout(
                        label = "Scale",
                        value = "${String.format("%.4f", scanState.mmPerPixel)} mm/px"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricReadout(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = ArReticleTint, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────
// Bottom Controls
// ─────────────────────────────────────────────────────────────

@Composable
private fun BottomControls(
    scanState: ScanState,
    photosCaptures: Int,
    maxPhotos: Int,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoLabels = listOf("Front", "Back", "Side")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            repeat(maxPhotos) { i ->
                PhotoProgressDot(
                    label = photoLabels.getOrElse(i) { "Photo ${i + 1}" },
                    isCaptured = i < photosCaptures,
                    isCurrent  = i == photosCaptures
                )
            }
        }

        // Contextual instruction text
        val instructionText = when {
            scanState is ScanState.Tilted ->
                "Hold the phone directly above and parallel to the label."
            scanState is ScanState.Searching && photosCaptures == 0 ->
                "Place the 40 mm ArUco marker near the package label."
            scanState is ScanState.Searching ->
                "Place the ArUco marker near the package."
            scanState is ScanState.Locked && photosCaptures == 0 ->
                "Marker locked ✓  Capture the FRONT of the package."
            scanState is ScanState.Locked && photosCaptures == 1 ->
                "Now capture the BACK of the package."
            scanState is ScanState.Locked && photosCaptures == 2 ->
                "Finally, capture the SIDE panel."
            else -> "Processing…"
        }
        val instructionColor = if (scanState is ScanState.Tilted) Amber500 else Color.White.copy(alpha = 0.85f)
        val instructionBg    = if (scanState is ScanState.Tilted) Amber500.copy(alpha = 0.15f) else ArGlassPanel

        Text(
            text = instructionText,
            style = MaterialTheme.typography.bodySmall,
            color = instructionColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(instructionBg, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Shutter button — disabled when not Locked
        val isCaptureable = scanState is ScanState.Locked
        Box(contentAlignment = Alignment.Center) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        when {
                            isCaptureable              -> ArReticleTint.copy(alpha = 0.3f)
                            scanState is ScanState.Tilted -> Amber500.copy(alpha = 0.2f)
                            else                       -> Color.White.copy(alpha = 0.1f)
                        },
                        CircleShape
                    )
            )
            Button(
                onClick   = onCapture,
                enabled   = isCaptureable,
                modifier  = Modifier.size(64.dp),
                shape     = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor         = if (isCaptureable) Color.White else Color.White.copy(alpha = 0.4f),
                    disabledContainerColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector  = if (scanState is ScanState.Captured) Icons.Default.Check else Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    tint = if (isCaptureable) Navy900 else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoProgressDot(label: String, isCaptured: Boolean, isCurrent: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (isCurrent) 12.dp else 8.dp)
                .background(
                    when {
                        isCaptured -> Emerald500
                        isCurrent  -> Color.White
                        else       -> Color.White.copy(alpha = 0.3f)
                    },
                    CircleShape
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Camera permission gate
// ─────────────────────────────────────────────────────────────

@Composable
private fun CameraPermissionRequest(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Navy900),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Camera, null, modifier = Modifier.size(64.dp), tint = Indigo500)
            Spacer(Modifier.height(16.dp))
            Text(
                "Camera Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Camera access is needed to inspect product labels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors  = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Text("Grant Camera Permission")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Haptic helper
// ─────────────────────────────────────────────────────────────

private fun vibrate(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.vibrate(
            VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(80)
    }
}
