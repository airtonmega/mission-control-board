package com.teseai.live.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teseai.live.domain.model.AudioAnalysisResult
import com.teseai.live.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAnalysisScreen(
    onBack: () -> Unit,
    viewModel: AudioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isRecording = uiState is AudioUiState.Recording

    val permissionLauncher = rememberAudioPermissionLauncher(
        onGranted = { viewModel.onPermissionGranted() },
        onDenied = { viewModel.onPermissionDenied() },
    )

    LaunchedEffect(Unit) {
        if (context.hasAudioPermission()) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text("Pergunta por Voz", color = OnSurfaceLight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OnSurfaceLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark),
                actions = {
                    if (isRecording) MicActiveIndicator()
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
                is AudioUiState.PermissionRequired -> AudioPermissionContent(context)
                is AudioUiState.Ready -> AudioReadyContent(onStartRecording = { viewModel.startRecording() })
                is AudioUiState.Recording -> AudioRecordingContent(
                    durationSecs = state.durationSecs,
                    onStop = { viewModel.stopRecording() },
                )
                is AudioUiState.Uploading -> AudioUploadingContent()
                is AudioUiState.Result -> AudioResultContent(
                    result = state.result,
                    onReset = { viewModel.reset() },
                )
                is AudioUiState.Error -> AudioErrorContent(
                    message = state.message,
                    onRetry = { viewModel.reset() },
                )
            }
        }
    }
}

// ── Permission denied ─────────────────────────────────────────────────────────

@Composable
private fun AudioPermissionContent(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            tint = OnSurfaceDim,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Permissão de microfone necessária",
            style = MaterialTheme.typography.titleLarge,
            color = OnSurfaceLight,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Para gravar perguntas por voz, o app precisa de acesso ao microfone. Sem isso, o áudio não será gravado.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Center,
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

// ── Ready — tap to record ─────────────────────────────────────────────────────

@Composable
private fun AudioReadyContent(onStartRecording: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Toque para gravar",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurfaceLight,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Faça sua pergunta técnica em voz alta",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        BigMicButton(
            isRecording = false,
            onClick = onStartRecording,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "O áudio é processado apenas durante a sessão. Nada é gravado em segundo plano.",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Recording — pulsing mic + stop button ─────────────────────────────────────

@Composable
private fun AudioRecordingContent(durationSecs: Int, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Gravando...",
            style = MaterialTheme.typography.headlineSmall,
            color = ErrorRed,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            formatDuration(durationSecs),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurfaceLight,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(40.dp))
        BigMicButton(isRecording = true, onClick = onStop)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Stop,
                contentDescription = null,
                tint = OnSurfaceMedium,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Toque para parar e analisar",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceMedium,
            )
        }
    }
}

// ── Uploading / transcribing ───────────────────────────────────────────────────

@Composable
private fun AudioUploadingContent() {
    Box(
        modifier = Modifier.fillMaxSize().background(SurfaceDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AccentCyan,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Transcrevendo e analisando áudio...",
                style = MaterialTheme.typography.titleMedium,
                color = AccentCyan,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Isso pode levar alguns segundos",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
            )
        }
    }
}

// ── Result ─────────────────────────────────────────────────────────────────────

@Composable
private fun AudioResultContent(result: AudioAnalysisResult, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Transcription card — shown prominently first
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Transcrição",
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    result.transcription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Analysis cards
        AudioInfoCard(title = "Tema Identificado", accent = CardTheme) {
            Text(result.detectedTheme, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceLight, fontWeight = FontWeight.SemiBold)
        }
        AudioInfoCard(title = "Dica Rápida", accent = CardTip) {
            Text(result.quickTip, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        }
        AudioInfoCard(title = "Resposta Curta", accent = CardShort) {
            Text(result.shortAnswer, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        }
        AudioInfoCard(title = "Resposta para Entrevista", accent = CardInterview) {
            Text(result.interviewAnswer, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        }
        if (result.commonErrors.isNotEmpty()) {
            AudioInfoCard(title = "Erros Comuns", accent = CardErrors) {
                result.commonErrors.forEach { error ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = CardErrors, fontWeight = FontWeight.Bold)
                        Text(error, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMedium)
                    }
                }
            }
        }
        if (result.studySuggestions.isNotEmpty()) {
            AudioInfoCard(title = "Sugestões de Estudo", accent = CardStudy) {
                result.studySuggestions.forEach { suggestion ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("→ ", color = CardStudy, fontWeight = FontWeight.Bold)
                        Text(suggestion, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMedium)
                    }
                }
            }
        }

        // Confidence + latency
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Confiança da IA", style = MaterialTheme.typography.labelLarge, color = OnSurfaceMedium)
                    Text(
                        "${(result.confidenceScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { result.confidenceScore },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = AccentBlue,
                    trackColor = OnSurfaceDim.copy(alpha = 0.3f),
                )
                Spacer(Modifier.height(4.dp))
                Text("${result.processingTimeMs} ms", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nova Gravação")
        }
    }
}

@Composable
private fun AudioInfoCard(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

// ── Error ──────────────────────────────────────────────────────────────────────

@Composable
private fun AudioErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Erro na análise", style = MaterialTheme.typography.titleLarge, color = ErrorRed, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium, textAlign = TextAlign.Center)
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

// ── Big mic button ─────────────────────────────────────────────────────────────

@Composable
private fun BigMicButton(isRecording: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "mic_scale",
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isRecording) 0.1f else 0.6f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "ring_alpha",
    )

    val ringColor = if (isRecording) ErrorRed else AccentBlue
    val bgColor = if (isRecording) ErrorRed else AccentBlue

    Box(contentAlignment = Alignment.Center) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = ringAlpha)),
        )
        // Button
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(96.dp)) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Parar gravação" else "Iniciar gravação",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}

// ── Mic active indicator ───────────────────────────────────────────────────────

@Composable
private fun MicActiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_indicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "mic_alpha",
    )
    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ErrorRed.copy(alpha = 0.15f))
            .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ErrorRed)
                .alpha(alpha),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "MIC ATIVO",
            style = MaterialTheme.typography.labelSmall,
            color = ErrorRed,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun formatDuration(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return "%d:%02d".format(m, s)
}

private fun Context.hasAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

@Composable
private fun rememberAudioPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean> {
    return androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onGranted() else onDenied()
    }
}
