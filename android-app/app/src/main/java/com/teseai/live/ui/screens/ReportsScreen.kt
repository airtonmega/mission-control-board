package com.teseai.live.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teseai.live.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Relatórios de Sessão", style = MaterialTheme.typography.headlineMedium, color = OnSurfaceLight, fontWeight = FontWeight.Bold)
            Text("Fase 2 — Em Desenvolvimento", style = MaterialTheme.typography.bodyLarge, color = AccentBlue)

            Spacer(Modifier.height(24.dp))

            // Mock summary stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.QuestionAnswer, label = "Perguntas", value = "7", color = AccentBlue)
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Star, label = "Confiança", value = "84%", color = SuccessGreen)
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Timer, label = "Tempo", value = "24m", color = AccentCyan)
            }

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tópicos Cobertos (Mock)", style = MaterialTheme.typography.titleMedium, color = OnSurfaceLight, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "Kotlin Coroutines" to 0.9f,
                        "Jetpack Compose" to 0.75f,
                        "Clean Architecture" to 0.6f,
                        "MVVM Pattern" to 0.85f,
                    ).forEach { (topic, score) ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(topic, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight)
                                Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = AccentBlue, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { score },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = AccentBlue,
                                trackColor = OnSurfaceDim.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Funcionalidades Planejadas", style = MaterialTheme.typography.titleMedium, color = WarningAmber, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "Histórico completo de sessões via /reports/session/{id}",
                        "Exportação de relatório em PDF",
                        "Gráfico de evolução ao longo do tempo",
                        "Comparativo entre sessões",
                        "Insights de pontos a melhorar",
                    ).forEach { feature ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("→ ", color = WarningAmber, fontWeight = FontWeight.Bold)
                            Text(feature, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMedium)
        }
    }
}
