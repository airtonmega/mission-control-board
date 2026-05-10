package com.teseai.live.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teseai.live.ui.theme.AccentBlue
import com.teseai.live.ui.theme.AccentCyan
import com.teseai.live.ui.theme.ErrorRed
import com.teseai.live.ui.theme.OnSurfaceDim
import com.teseai.live.ui.theme.OnSurfaceLight
import com.teseai.live.ui.theme.OnSurfaceMedium
import com.teseai.live.ui.theme.PrimaryDark
import com.teseai.live.ui.theme.SuccessGreen
import com.teseai.live.ui.theme.SurfaceCard
import com.teseai.live.ui.theme.SurfaceDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeAudioScreen(
    onBack: () -> Unit,
    onFallback: () -> Unit,
    viewModel: RealtimeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text("Modo Realtime", color = OnSurfaceLight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OnSurfaceLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(tween(300)) + slideInVertically { it / 8 } togetherWith fadeOut(tween(200))
                },
                label = "RealtimeContent",
            ) { state ->
                when (state) {
                    is RealtimeUiState.Idle -> RealtimeIdleContent(
                        onStart = { viewModel.startSession() },
                    )

                    is RealtimeUiState.RequestingToken -> RealtimeLoadingContent(
                        message = "Obtendo token seguro...",
                    )

                    is RealtimeUiState.Connecting -> RealtimeLoadingContent(
                        message = "Conectando ao serviço...",
                    )

                    is RealtimeUiState.Listening -> RealtimeListeningContent(
                        onInterrupt = { viewModel.interrupt() },
                    )

                    is RealtimeUiState.Transcribing -> RealtimeTranscribingContent(
                        partial = state.partial,
                        onInterrupt = { viewModel.interrupt() },
                    )

                    is RealtimeUiState.Responding -> RealtimeRespondingContent(
                        userTranscript = state.userTranscript,
                        aiPartial = state.partial,
                        onInterrupt = { viewModel.interrupt() },
                    )

                    is RealtimeUiState.Done -> RealtimeDoneContent(
                        userTranscript = state.userTranscript,
                        aiResponse = state.aiResponse,
                        onNewSession = { viewModel.reset() },
                        onBack = onBack,
                    )

                    is RealtimeUiState.Error -> RealtimeErrorContent(
                        message = state.message,
                        offerFallback = state.offerFallback,
                        onRetry = { viewModel.reset() },
                        onFallback = onFallback,
                    )
                }
            }
        }
    }
}

// ── Idle ───────────────────────────────────────────────────────────────────────

@Composable
private fun RealtimeIdleContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Modo Realtime",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurfaceLight,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Baixa latência, streaming em tempo real. Fale diretamente com a IA — sem gravação oculta.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "O áudio é transmitido diretamente e processado em tempo real. Nenhum arquivo de áudio é armazenado.",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Iniciar Sessão Realtime", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Loading (RequestingToken / Connecting) ─────────────────────────────────────

@Composable
private fun RealtimeLoadingContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AccentBlue,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Listening ─────────────────────────────────────────────────────────────────

@Composable
private fun RealtimeListeningContent(onInterrupt: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(ErrorRed.copy(alpha = alpha * 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = ErrorRed.copy(alpha = alpha),
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "ESCUTANDO",
            style = MaterialTheme.typography.titleLarge,
            color = ErrorRed.copy(alpha = alpha),
            fontWeight = FontWeight.Bold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Fale agora...",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceMedium,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onInterrupt,
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Interromper", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Transcribing ──────────────────────────────────────────────────────────────

@Composable
private fun RealtimeTranscribingContent(partial: String, onInterrupt: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = AccentBlue,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "TRANSCREVENDO",
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(20.dp))
        if (partial.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Sua fala:",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        partial,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceLight,
                    )
                }
            }
        } else {
            Text(
                "Detectando fala...",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim,
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onInterrupt,
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Interromper", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Responding ────────────────────────────────────────────────────────────────

@Composable
private fun RealtimeRespondingContent(
    userTranscript: String,
    aiPartial: String,
    onInterrupt: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // User transcript card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Você disse:",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    userTranscript.ifEmpty { "..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // AI response streaming card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = AccentBlue,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Resposta da IA:",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    aiPartial.ifEmpty { "..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onInterrupt,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Interromper", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Done ──────────────────────────────────────────────────────────────────────

@Composable
private fun RealtimeDoneContent(
    userTranscript: String,
    aiResponse: String,
    onNewSession: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // User transcript
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Você disse:",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    userTranscript.ifEmpty { "(sem transcrição)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // AI full response
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Resposta da IA:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    aiResponse.ifEmpty { "(sem resposta)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNewSession,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nova Sessão", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Encerrar")
        }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun RealtimeErrorContent(
    message: String,
    offerFallback: Boolean,
    onRetry: () -> Unit,
    onFallback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Erro no Modo Realtime",
            style = MaterialTheme.typography.titleLarge,
            color = ErrorRed,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tentar Novamente")
        }
        if (offerFallback) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onFallback,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Usar Modo Audio Padrao")
            }
        }
    }
}
