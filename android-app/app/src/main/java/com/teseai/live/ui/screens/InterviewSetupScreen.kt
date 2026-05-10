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
fun InterviewSetupScreen(
    onBack: () -> Unit,
    onStartInterview: (String, String) -> Unit = { _, _ -> },
) {
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
            Spacer(Modifier.height(24.dp))

            SectionLabel("Área de Foco")
            Spacer(Modifier.height(10.dp))
            ChipGroup(options = areas, selected = selectedArea, onSelect = { selectedArea = it })

            Spacer(Modifier.height(20.dp))

            SectionLabel("Nível")
            Spacer(Modifier.height(10.dp))
            ChipGroup(options = levels, selected = selectedLevel, onSelect = { selectedLevel = it })

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onStartInterview(selectedArea, selectedLevel) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Iniciar Simulação", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
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
