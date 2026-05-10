package com.teseai.live.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teseai.live.ui.theme.*

@Composable
fun EthicsConsentScreen(
    onConsentAccepted: () -> Unit,
    viewModel: ConsentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.consentRecorded) {
        if (uiState.consentRecorded) onConsentAccepted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TeseAI Live",
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurfaceLight,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Assistente Acadêmico Multimodal",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceMedium,
            )
            Spacer(modifier = Modifier.height(32.dp))

            EthicsCard()

            Spacer(modifier = Modifier.height(24.dp))

            var checked by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentBlue,
                        uncheckedColor = OnSurfaceDim,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Li, entendi e concordo com os termos de uso ético acima.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.acceptConsent() },
                enabled = checked && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnSurfaceLight,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        "Entendi e Aceito",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EthicsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Gavel, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Termo de Uso Ético", style = MaterialTheme.typography.titleMedium, color = WarningAmber, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        EthicsItem(
            icon = Icons.Default.School,
            title = "Finalidade Exclusivamente Acadêmica",
            body = "Este aplicativo destina-se a pesquisa acadêmica de mestrado, treinamento e simulações autorizadas. Não deve ser usado em entrevistas reais, provas ou avaliações sem consentimento explícito dos avaliadores.",
        )
        Spacer(modifier = Modifier.height(12.dp))
        EthicsItem(
            icon = Icons.Default.Warning,
            title = "Transparência Obrigatória",
            body = "Indicadores visuais mostrarão quando câmera e microfone estiverem ativos. Não há captura oculta. Todo o processamento é declarado.",
        )
        Spacer(modifier = Modifier.height(12.dp))
        EthicsItem(
            icon = Icons.Default.Gavel,
            title = "Responsabilidade do Usuário",
            body = "O uso indevido desta ferramenta é de responsabilidade exclusiva do usuário. O uso ético é pré-requisito para o funcionamento do app.",
        )
    }
}

@Composable
private fun EthicsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = OnSurfaceLight, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium)
        }
    }
}
