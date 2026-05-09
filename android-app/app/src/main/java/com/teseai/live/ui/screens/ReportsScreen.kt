package com.teseai.live.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teseai.live.data.remote.dto.InputModeCount
import com.teseai.live.data.remote.dto.SessionReportResponse
import com.teseai.live.ui.theme.AccentBlue
import com.teseai.live.ui.theme.ErrorRed
import com.teseai.live.ui.theme.OnSurfaceDim
import com.teseai.live.ui.theme.OnSurfaceLight
import com.teseai.live.ui.theme.OnSurfaceMedium
import com.teseai.live.ui.theme.PrimaryDark
import com.teseai.live.ui.theme.SuccessGreen
import com.teseai.live.ui.theme.SurfaceCard
import com.teseai.live.ui.theme.SurfaceDark
import com.teseai.live.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text("Relatórios", color = OnSurfaceLight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OnSurfaceLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark),
            )
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is ReportsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue)
                        Spacer(Modifier.height(16.dp))
                        Text("Carregando relatório...", color = OnSurfaceMedium)
                    }
                }
            }

            is ReportsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            "Erro ao carregar",
                            style = MaterialTheme.typography.titleLarge,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(state.message, color = OnSurfaceMedium, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadReport() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        ) {
                            Text("Recarregar", color = Color.White)
                        }
                    }
                }
            }

            is ReportsUiState.Success -> {
                ReportsDashboard(
                    paddingValues = paddingValues,
                    report = state.report,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun ReportsDashboard(
    paddingValues: PaddingValues,
    report: SessionReportResponse,
    viewModel: ReportsViewModel,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.BarChart, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Sessão: ...${report.sessionId.takeLast(12)}",
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceDim,
        )
        Text(
            formatDate(report.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceDim,
        )
        Spacer(Modifier.height(20.dp))

        // Summary cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReportStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.QuestionAnswer,
                label = "Perguntas",
                value = "${report.totalQueries}",
                color = AccentBlue,
            )
            ReportStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                label = "Confiança",
                value = "${(report.averageConfidence * 100).toInt()}%",
                color = SuccessGreen,
            )
            ReportStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                label = "Duração",
                value = formatDuration(report.durationSeconds),
                color = WarningAmber,
            )
        }
        Spacer(Modifier.height(16.dp))

        // Latency card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Latência Média", color = OnSurfaceMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (report.averageLatencyMs != null) "${report.averageLatencyMs.toInt()} ms" else "—",
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Input modes card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Modo de Entrada",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceLight,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                val modes = report.inputModes
                val total = (modes.text + modes.image + modes.audio + modes.interview).coerceAtLeast(1)
                InputModeRow("Texto", modes.text, total, AccentBlue)
                InputModeRow("Imagem", modes.image, total, SuccessGreen)
                InputModeRow("Áudio", modes.audio, total, WarningAmber)
                InputModeRow("Entrevista", modes.interview, total, ErrorRed)
            }
        }
        Spacer(Modifier.height(16.dp))

        // Interview average score card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Nota de Entrevista", color = OnSurfaceMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (report.interviewAverageScore != null) {
                        String.format("%.1f", report.interviewAverageScore) + "/10"
                    } else {
                        "—"
                    },
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Topics covered
        if (report.topicsCovered.isNotEmpty()) {
            Text(
                "Temas Abordados",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            TopicsChipRow(topics = report.topicsCovered)
            Spacer(Modifier.height(16.dp))
        }

        // Highlights
        if (report.highlights.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Destaques",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceLight,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    report.highlights.forEach { highlight ->
                        Text(
                            "• $highlight",
                            color = OnSurfaceMedium,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Export JSON button
        Button(
            onClick = {
                val json = viewModel.buildJsonExport(report)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Exportar Relatório"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Exportar JSON", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun InputModeRow(label: String, count: Int, total: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = OnSurfaceMedium, style = MaterialTheme.typography.bodySmall)
            Text("$count", color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { count.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = OnSurfaceDim.copy(alpha = 0.2f),
        )
    }
}

@Composable
private fun TopicsChipRow(topics: List<String>) {
    val rows = topics.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowTopics ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTopics.forEach { topic ->
                    Surface(
                        color = AccentBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            topic,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = AccentBlue,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMedium)
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s}s"
}

private fun formatDate(raw: String): String = try {
    raw.substringBefore("T").let { date ->
        val parts = date.split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else raw
    }
} catch (_: Exception) {
    raw
}
