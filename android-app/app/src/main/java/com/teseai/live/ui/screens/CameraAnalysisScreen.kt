package com.teseai.live.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teseai.live.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAnalysisScreen(onBack: () -> Unit) {
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
                                .background(ActiveIndicator),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("CAM ATIVA", style = MaterialTheme.typography.labelSmall, color = ActiveIndicator, fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(PrimaryMedium)
                    .border(2.dp, AccentCyan, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(56.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Análise por Câmera",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurfaceLight,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fase 2 — Em Desenvolvimento",
                style = MaterialTheme.typography.bodyLarge,
                color = AccentCyan,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                    val features = listOf(
                        "Captura de frames em tempo real com CameraX",
                        "Detecção de texto em imagens (OCR)",
                        "Análise de diagramas e whiteboards",
                        "Envio para backend /analyze/image",
                        "Indicador visual de câmera ativa",
                    )
                    features.forEach { feature ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("→ ", color = AccentCyan, fontWeight = FontWeight.Bold)
                            Text(feature, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voltar ao Cockpit")
            }
        }
    }
}
