package com.teseai.live.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teseai.live.domain.model.AnalysisResult
import com.teseai.live.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitLiveScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToInterview: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: CockpitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TeseAI Live", style = MaterialTheme.typography.titleLarge, color = OnSurfaceLight, fontWeight = FontWeight.Bold)
                        Text("Cockpit Acadêmico", style = MaterialTheme.typography.labelSmall, color = OnSurfaceMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark),
                actions = {
                    ActiveIndicatorChip(
                        label = "CAM",
                        active = state.isCameraActive,
                        icon = Icons.Default.Videocam,
                    )
                    Spacer(Modifier.width(4.dp))
                    ActiveIndicatorChip(
                        label = "MIC",
                        active = state.isMicActive,
                        icon = Icons.Default.Mic,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = OnSurfaceMedium)
                    }
                },
            )
        },
        bottomBar = {
            CockpitBottomBar(
                onCamera = onNavigateToCamera,
                onAudio = onNavigateToAudio,
                onInterview = onNavigateToInterview,
                onReports = onNavigateToReports,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
        ) {
            InputCard(
                text = state.inputText,
                onTextChange = viewModel::onInputChanged,
                onAnalyze = viewModel::analyzeText,
                isLoading = state.analysisState is CockpitUiState.Loading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = state.analysisState,
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 4 } togetherWith fadeOut()
                },
                label = "AnalysisContent",
            ) { uiState ->
                when (uiState) {
                    is CockpitUiState.Idle -> IdleHint()
                    is CockpitUiState.Loading -> LoadingCards()
                    is CockpitUiState.Success -> AnalysisCards(
                        result = uiState.result,
                        onClear = viewModel::clearResult,
                    )
                    is CockpitUiState.Error -> ErrorCard(message = uiState.message, onRetry = viewModel::analyzeText)
                }
            }
        }
    }
}

@Composable
private fun ActiveIndicatorChip(label: String, active: Boolean, icon: ImageVector) {
    val color = if (active) ActiveIndicator else OnSurfaceDim
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) ActiveIndicator.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ActiveIndicator),
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InputCard(
    text: String,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    isLoading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Pergunta Técnica",
                style = MaterialTheme.typography.labelLarge,
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: O que são Kotlin Coroutines?", color = OnSurfaceDim) },
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = OnSurfaceDim,
                    focusedTextColor = OnSurfaceLight,
                    unfocusedTextColor = OnSurfaceLight,
                    cursorColor = AccentBlue,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAnalyze,
                enabled = text.length >= 3 && !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnSurfaceLight, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Analisando…")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analisar com IA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnalysisCards(result: AnalysisResult, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (result.isMock) {
            MockBadge()
        }
        AnalysisCard(
            icon = Icons.Default.Label,
            accentColor = CardTheme,
            title = "Tema Detectado",
            content = { Text(result.detectedTheme, style = MaterialTheme.typography.titleMedium, color = OnSurfaceLight, fontWeight = FontWeight.SemiBold) },
        )
        AnalysisCard(
            icon = Icons.Default.Lightbulb,
            accentColor = CardTip,
            title = "Dica Rápida",
            content = { Text(result.quickTip, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight) },
        )
        AnalysisCard(
            icon = Icons.Default.ShortText,
            accentColor = CardShort,
            title = "Resposta Curta",
            content = { Text(result.shortAnswer, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight) },
        )
        AnalysisCard(
            icon = Icons.Default.RecordVoiceOver,
            accentColor = CardInterview,
            title = "Resposta para Entrevista",
            content = { Text(result.interviewAnswer, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight) },
        )
        AnalysisCard(
            icon = Icons.Default.MenuBook,
            accentColor = CardComplete,
            title = "Resposta Completa",
            content = { Text(result.completeAnswer, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight) },
        )
        AnalysisCard(
            icon = Icons.Default.BugReport,
            accentColor = CardErrors,
            title = "Erros Comuns",
            content = {
                result.commonErrors.forEach { error ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = CardErrors, fontWeight = FontWeight.Bold)
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight)
                    }
                }
            },
        )
        AnalysisCard(
            icon = Icons.Default.School,
            accentColor = CardStudy,
            title = "Sugestões de Estudo",
            content = {
                result.studySuggestions.forEach { suggestion ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("→ ", color = CardStudy, fontWeight = FontWeight.Bold)
                        Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight)
                    }
                }
            },
        )
        ConfidenceBar(score = result.confidenceScore, timeMs = result.processingTimeMs)
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.fillMaxWidth(),
            border = ButtonDefaults.outlinedButtonBorder.copy(),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nova Pergunta")
        }
    }
}

@Composable
private fun AnalysisCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = accentColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ConfidenceBar(score: Float, timeMs: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Confiança da IA", style = MaterialTheme.typography.labelLarge, color = OnSurfaceMedium)
                Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = AccentBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { score },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AccentBlue,
                trackColor = OnSurfaceDim.copy(alpha = 0.3f),
            )
            Spacer(Modifier.height(4.dp))
            Text("Processado em ${timeMs}ms", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
        }
    }
}

@Composable
private fun MockBadge() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(WarningAmber.copy(alpha = 0.15f))
            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Modo Mock ativo — sem consumo de API", style = MaterialTheme.typography.labelMedium, color = WarningAmber)
    }
}

@Composable
private fun IdleHint() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OnSurfaceDim, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Digite uma pergunta técnica para começar", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceDim)
        }
    }
}

@Composable
private fun LoadingCards() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            Card(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        color = AccentBlue,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("Erro na análise", style = MaterialTheme.typography.titleMedium, color = ErrorRed, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                Text("Tentar Novamente")
            }
        }
    }
}

@Composable
private fun CockpitBottomBar(
    onCamera: () -> Unit,
    onAudio: () -> Unit,
    onInterview: () -> Unit,
    onReports: () -> Unit,
) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = AccentBlue,
        selectedTextColor = AccentBlue,
        indicatorColor = AccentBlue.copy(alpha = 0.15f),
        unselectedIconColor = OnSurfaceDim,
        unselectedTextColor = OnSurfaceDim,
    )
    NavigationBar(containerColor = PrimaryDark) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Cockpit") },
            colors = navItemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onCamera,
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
            label = { Text("Câmera") },
            colors = navItemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onAudio,
            icon = { Icon(Icons.Default.Mic, contentDescription = null) },
            label = { Text("Voz") },
            colors = navItemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onInterview,
            icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
            label = { Text("Entrevista") },
            colors = navItemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onReports,
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            label = { Text("Relatórios") },
            colors = navItemColors,
        )
    }
}
