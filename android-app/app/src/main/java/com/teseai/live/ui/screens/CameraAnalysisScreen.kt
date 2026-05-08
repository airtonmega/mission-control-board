package com.teseai.live.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teseai.live.domain.model.ImageAnalysisResult
import com.teseai.live.ui.theme.*
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAnalysisScreen(
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForPermission(
        onGranted = { viewModel.onPermissionGranted() },
        onDenied = { viewModel.onPermissionDenied() },
    )

    LaunchedEffect(Unit) {
        if (context.hasPermission(Manifest.permission.CAMERA)) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text("Análise por Câmera", color = OnSurfaceLight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OnSurfaceLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark),
                actions = {
                    if (uiState !is CameraUiState.PermissionRequired) {
                        CamActiveIndicator()
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = uiState) {
                is CameraUiState.PermissionRequired -> PermissionDeniedContent(context)
                is CameraUiState.Ready -> CameraPreviewContent(
                    onCapture = { file ->
                        viewModel.onCapturing()
                        viewModel.analyzeImage(file)
                    },
                )
                is CameraUiState.Capturing, is CameraUiState.Analyzing -> AnalyzingContent()
                is CameraUiState.Result -> ImageResultContent(
                    result = state.result,
                    onReset = { viewModel.reset() },
                )
                is CameraUiState.Error -> CameraErrorContent(
                    message = state.message,
                    onRetry = { viewModel.reset() },
                )
            }
        }
    }
}

// ── Permission denied ─────────────────────────────────────────────────────────

@Composable
private fun PermissionDeniedContent(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = OnSurfaceDim,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Permissão de câmera necessária",
            style = MaterialTheme.typography.titleLarge,
            color = OnSurfaceLight,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Para analisar imagens, o app precisa de acesso à câmera. Conceda a permissão nas configurações do sistema.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Abrir Configurações")
        }
    }
}

// ── Camera preview + capture button ──────────────────────────────────────────

@Composable
private fun CameraPreviewContent(onCapture: (File) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: Executor = ContextCompat.getMainExecutor(context)

    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                    }, executor)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        ) {
            CaptureButton {
                val photoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onCapture(photoFile)
                        }
                        override fun onError(exc: ImageCaptureException) {
                            // Capture failure treated as a transient error; user can retry
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capturar",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

// ── Analyzing overlay ─────────────────────────────────────────────────────────

@Composable
private fun AnalyzingContent() {
    Box(
        modifier = Modifier.fillMaxSize().background(SurfaceDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Analisando imagem...", style = MaterialTheme.typography.titleMedium, color = AccentCyan)
        }
    }
}

// ── Result cards ──────────────────────────────────────────────────────────────

@Composable
private fun ImageResultContent(result: ImageAnalysisResult, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (result.detectedText.isNotBlank()) {
            ImageInfoCard(title = "Texto Detectado", accent = AccentCyan) {
                Text(result.detectedText, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
            }
        }
        ImageInfoCard(title = "Tema Identificado", accent = CardTheme) {
            Text(result.detectedTheme, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceLight, fontWeight = FontWeight.SemiBold)
        }
        ImageInfoCard(title = "Dica Rápida", accent = CardTip) {
            Text(result.quickTip, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        }
        ImageInfoCard(title = "Resposta Curta", accent = CardShort) {
            Text(result.shortAnswer, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        }
        ImageInfoCard(title = "Resposta para Entrevista", accent = CardInterview) {
            Text(result.interviewAnswer, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        }
        if (result.commonErrors.isNotEmpty()) {
            ImageInfoCard(title = "Erros Comuns", accent = CardErrors) {
                result.commonErrors.forEach { error ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = CardErrors, fontWeight = FontWeight.Bold)
                        Text(error, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMedium)
                    }
                }
            }
        }
        if (result.studySuggestions.isNotEmpty()) {
            ImageInfoCard(title = "Sugestões de Estudo", accent = CardStudy) {
                result.studySuggestions.forEach { suggestion ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("→ ", color = CardStudy, fontWeight = FontWeight.Bold)
                        Text(suggestion, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (result.isMock) {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                    color = WarningAmber.copy(alpha = 0.15f),
                ) {
                    Text(
                        "MOCK",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                "${result.processingTimeMs} ms",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceDim,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nova Captura")
        }
    }
}

@Composable
private fun ImageInfoCard(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────

@Composable
private fun CameraErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Erro na análise", style = MaterialTheme.typography.titleLarge, color = ErrorRed, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tentar Novamente")
        }
    }
}

// ── Active camera indicator ───────────────────────────────────────────────────

@Composable
private fun CamActiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "cam_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "cam_alpha",
    )
    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ActiveIndicator.copy(alpha = 0.15f))
            .border(1.dp, ActiveIndicator.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ActiveIndicator)
                .alpha(alpha),
        )
        Spacer(Modifier.width(6.dp))
        Text("CAM ATIVA", style = MaterialTheme.typography.labelSmall, color = ActiveIndicator, fontWeight = FontWeight.Bold)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

@Composable
private fun rememberLauncherForPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean> {
    return androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onGranted() else onDenied()
    }
}
