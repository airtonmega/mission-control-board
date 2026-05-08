package com.teseai.live.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teseai.live.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewSetupScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    var selectedArea by remember { mutableStateOf("Android") }
    var selectedLevel by remember { mutableStateOf("Pleno") }

    val areas = listOf("Android", "Backend", "Full Stack", "Data Science", "DevOps", "Arquitetura")
    val levels = listOf("Júnior", "Pleno", "Sênior", "Especialista")

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text("Configurar Entrevista", color = OnSurfaceLight) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Simulação de Entrevista",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurfaceLight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Fase 2 — Em Desenvolvimento",
                style = MaterialTheme.typography.bodyLarge,
                color = AccentBlue,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            SectionLabel("Área de Foco")
            Spacer(Modifier.height(10.dp))
            ChipGroup(options = areas, selected = selectedArea, onSelect = { selectedArea = it })

            Spacer(Modifier.height(20.dp))

            SectionLabel("Nível")
            Spacer(Modifier.height(10.dp))
            ChipGroup(options = levels, selected = selectedLevel, onSelect = { selectedLevel = it })

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Construction, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Funcionalidades Planejadas", style = MaterialTheme.typography.titleMedium, color = WarningAmber, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "Fluxo de perguntas progressivo via /interview/start",
                        "Avaliação de respostas em tempo real",
                        "Score final com feedback detalhado",
                        "Modo voz com Speech-to-Text",
                        "Relatório completo da sessão",
                    ).forEach { feature ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("→ ", color = AccentBlue, fontWeight = FontWeight.Bold)
                            Text(feature, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = PrimaryMedium,
                    disabledContentColor = OnSurfaceDim,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Iniciar Simulação — Em Breve", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.titleMedium, color = OnSurfaceMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ChipGroup(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val chunked = options.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else SurfaceCard)
                            .border(
                                1.dp,
                                if (isSelected) AccentBlue else OnSurfaceDim.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp),
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            option,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) AccentBlue else OnSurfaceMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
