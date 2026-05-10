package com.teseai.live.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teseai.live.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    var darkThemeEnabled by remember { mutableStateOf(true) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var micPermissionGranted by remember { mutableStateOf(false) }
    var apiUrl by remember { mutableStateOf("http://10.0.2.2:8000") }

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text("Configurações", color = OnSurfaceLight) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection(title = "Conexão com Backend") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("URL da API", style = MaterialTheme.typography.labelLarge, color = OnSurfaceMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = AccentBlue)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = OnSurfaceDim,
                            focusedTextColor = OnSurfaceLight,
                            unfocusedTextColor = OnSurfaceLight,
                            cursorColor = AccentBlue,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Use 10.0.2.2:8000 para emulador Android ou o IP da máquina na rede local.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceDim,
                    )
                }
            }

            SettingsSection(title = "Aparência") {
                SettingsToggleRow(
                    icon = Icons.Default.DarkMode,
                    iconColor = AccentCyan,
                    title = "Tema Escuro",
                    subtitle = "Recomendado para uso prolongado",
                    checked = darkThemeEnabled,
                    onCheckedChange = { darkThemeEnabled = it },
                )
            }

            SettingsSection(title = "Permissões") {
                SettingsToggleRow(
                    icon = Icons.Default.CameraAlt,
                    iconColor = AccentBlue,
                    title = "Câmera",
                    subtitle = "Necessária para análise de imagem",
                    checked = cameraPermissionGranted,
                    onCheckedChange = { cameraPermissionGranted = it },
                )
                HorizontalDivider(color = OnSurfaceDim.copy(alpha = 0.2f))
                SettingsToggleRow(
                    icon = Icons.Default.Mic,
                    iconColor = AccentBlue,
                    title = "Microfone",
                    subtitle = "Necessário para modo de voz",
                    checked = micPermissionGranted,
                    onCheckedChange = { micPermissionGranted = it },
                )
            }

            SettingsSection(title = "Sobre") {
                InfoRow(icon = Icons.Default.Info, label = "Versão", value = "1.0.0-alpha")
                HorizontalDivider(color = OnSurfaceDim.copy(alpha = 0.2f))
                InfoRow(icon = Icons.Default.School, label = "Finalidade", value = "Pesquisa de Mestrado")
                HorizontalDivider(color = OnSurfaceDim.copy(alpha = 0.2f))
                InfoRow(icon = Icons.Default.Code, label = "Stack", value = "Kotlin · Compose · FastAPI")
                HorizontalDivider(color = OnSurfaceDim.copy(alpha = 0.2f))
                InfoRow(icon = Icons.Default.Gavel, label = "Uso", value = "Apenas para fins éticos")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceDim,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceLight, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnSurfaceLight,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = OnSurfaceDim,
                uncheckedTrackColor = PrimaryMedium,
            ),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = OnSurfaceDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceLight, fontWeight = FontWeight.Medium)
    }
}
