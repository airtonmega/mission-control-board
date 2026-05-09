package com.teseai.live.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teseai.live.domain.model.InterviewEvaluation
import com.teseai.live.domain.model.InterviewReport
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
fun InterviewSessionScreen(
    onBack: () -> Unit,
    viewModel: InterviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = { Text("Entrevista Simulada", color = OnSurfaceLight) },
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
            AnimatedContent(targetState = uiState, label = "interview_state") { state ->
                when (state) {
                    is InterviewUiState.Loading -> InterviewLoadingContent()
                    is InterviewUiState.Question -> InterviewQuestionContent(
                        state = state,
                        onSubmit = { viewModel.submitAnswer(it) },
                    )
                    is InterviewUiState.Evaluating -> InterviewEvaluatingContent(state)
                    is InterviewUiState.EvaluationResult -> InterviewResultContent(
                        state = state,
                        onNext = { viewModel.nextQuestion() },
                    )
                    is InterviewUiState.Report -> InterviewReportContent(
                        report = state.report,
                        onFinish = onBack,
                    )
                    is InterviewUiState.Error -> InterviewErrorContent(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                    )
                }
            }
        }
    }
}

@Composable
private fun InterviewLoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentBlue)
            Spacer(Modifier.height(16.dp))
            Text("Iniciando entrevista...", color = OnSurfaceMedium)
        }
    }
}

@Composable
private fun InterviewQuestionContent(
    state: InterviewUiState.Question,
    onSubmit: (String) -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
    ) {
        Text(
            "Pergunta ${state.questionNumber} de ${state.totalQuestions}",
            color = OnSurfaceMedium,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { state.questionNumber.toFloat() / state.totalQuestions },
            modifier = Modifier.fillMaxWidth(),
            color = AccentBlue,
            trackColor = SurfaceCard,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InterviewBadge(text = state.area, color = AccentBlue)
            InterviewBadge(text = state.level, color = SuccessGreen)
        }
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                state.question,
                modifier = Modifier.padding(20.dp),
                color = OnSurfaceLight,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = answer,
            onValueChange = { if (it.length <= 1000) answer = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            placeholder = { Text("Digite sua resposta aqui...", color = OnSurfaceDim) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = OnSurfaceDim,
                focusedTextColor = OnSurfaceLight,
                unfocusedTextColor = OnSurfaceMedium,
                cursorColor = AccentBlue,
            ),
            shape = RoundedCornerShape(12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "${answer.length}/1000",
                color = OnSurfaceDim,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onSubmit(answer.trim()) },
            enabled = answer.trim().length >= 10,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Enviar Resposta", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun InterviewEvaluatingContent(state: InterviewUiState.Evaluating) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentBlue)
            Spacer(Modifier.height(16.dp))
            Text("Avaliando sua resposta...", color = OnSurfaceMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pergunta ${state.questionNumber} de ${state.totalQuestions}",
                color = OnSurfaceDim,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun InterviewResultContent(
    state: InterviewUiState.EvaluationResult,
    onNext: () -> Unit,
) {
    val eval = state.evaluation
    var showImproved by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
    ) {
        Text(
            "Resultado — Pergunta ${eval.questionNumber}",
            style = MaterialTheme.typography.titleLarge,
            color = OnSurfaceLight,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Notas",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                ScoreBar("Geral", eval.score)
                ScoreBar("Precisão", eval.accuracy)
                ScoreBar("Clareza", eval.clarity)
                ScoreBar("Profundidade", eval.depth)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (eval.strengths.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pontos Fortes", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    eval.strengths.forEach {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(it, color = OnSurfaceMedium, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (eval.weaknesses.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pontos de Melhoria", color = WarningAmber, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    eval.weaknesses.forEach {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(it, color = OnSurfaceMedium, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Card(
            onClick = { showImproved = !showImproved },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Resposta Ideal", color = AccentBlue, fontWeight = FontWeight.SemiBold)
                    Text(if (showImproved) "▲" else "▼", color = AccentBlue)
                }
                if (showImproved) {
                    Spacer(Modifier.height(10.dp))
                    Text(eval.improvedAnswer, color = OnSurfaceMedium, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                if (eval.completed || eval.nextQuestion == null) "Ver Relatório" else "Próxima Pergunta",
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun InterviewReportContent(
    report: InterviewReport,
    onFinish: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
    ) {
        Text(
            "Relatório Final",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurfaceLight,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            InterviewBadge(text = report.area, color = AccentBlue)
            InterviewBadge(text = report.level, color = SuccessGreen)
        }
        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = String.format("%.1f", report.averageScore),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentBlue,
                )
                Text("Nota Média", color = OnSurfaceMedium)
                Spacer(Modifier.height(16.dp))
                ScoreBar("Precisão Média", report.averageAccuracy)
                ScoreBar("Clareza Média", report.averageClarity)
                ScoreBar("Profundidade Média", report.averageDepth)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (report.evaluations.isNotEmpty()) {
            Text(
                "Por Pergunta",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                report.evaluations.forEach { eval ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Q${eval.questionNumber}",
                                color = OnSurfaceDim,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                String.format("%.1f", eval.score),
                                color = scoreColor(eval.score),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (report.topStrengths.isNotEmpty()) {
            Text(
                "Principais Pontos Fortes",
                style = MaterialTheme.typography.titleMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            report.topStrengths.forEach {
                Text(
                    "• $it",
                    color = OnSurfaceMedium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        if (report.topWeaknesses.isNotEmpty()) {
            Text(
                "Pontos de Melhoria",
                style = MaterialTheme.typography.titleMedium,
                color = WarningAmber,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            report.topWeaknesses.forEach {
                Text(
                    "• $it",
                    color = OnSurfaceMedium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Encerrar", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun InterviewErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                "Erro na Entrevista",
                style = MaterialTheme.typography.titleLarge,
                color = ErrorRed,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = OnSurfaceMedium, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                Text("Tentar Novamente", color = Color.White)
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Float) {
    val pct = (value / 10f).coerceIn(0f, 1f)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = OnSurfaceMedium, style = MaterialTheme.typography.bodySmall)
            Text(
                String.format("%.1f", value),
                color = scoreColor(value),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth(),
            color = scoreColor(value),
            trackColor = SurfaceCard,
        )
    }
}

@Composable
private fun InterviewBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun scoreColor(score: Float): Color = when {
    score >= 8f -> SuccessGreen
    score >= 6f -> WarningAmber
    else -> ErrorRed
}
