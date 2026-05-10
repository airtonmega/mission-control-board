package com.teseai.live.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teseai.live.data.remote.dto.SessionReportResponse
import com.teseai.live.data.repository.AnalyzeRepository
import com.teseai.live.data.repository.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Success(val report: SessionReportResponse) : ReportsUiState
    data class Error(val message: String) : ReportsUiState
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsRepo: ReportsRepository,
    private val analyzeRepo: AnalyzeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    fun loadReport() {
        viewModelScope.launch {
            _uiState.value = ReportsUiState.Loading
            try {
                val sessionId = analyzeRepo.getSessionId()
                val report = reportsRepo.getSessionReport(sessionId)
                _uiState.value = ReportsUiState.Success(report)
            } catch (e: Exception) {
                _uiState.value = ReportsUiState.Error(e.message ?: "Erro ao carregar relatório")
            }
        }
    }

    fun buildJsonExport(report: SessionReportResponse): String {
        return buildString {
            appendLine("{")
            appendLine("  \"session_id\": \"${report.sessionId}\",")
            appendLine("  \"total_queries\": ${report.totalQueries},")
            appendLine("  \"average_confidence\": ${report.averageConfidence},")
            appendLine("  \"duration_seconds\": ${report.durationSeconds},")
            appendLine("  \"average_latency_ms\": ${report.averageLatencyMs},")
            appendLine("  \"interview_average_score\": ${report.interviewAverageScore},")
            appendLine("  \"input_modes\": {")
            appendLine("    \"text\": ${report.inputModes.text},")
            appendLine("    \"image\": ${report.inputModes.image},")
            appendLine("    \"audio\": ${report.inputModes.audio},")
            appendLine("    \"interview\": ${report.inputModes.interview}")
            appendLine("  },")
            appendLine("  \"topics_covered\": [${report.topicsCovered.joinToString(", ") { "\"$it\"" }}],")
            appendLine("  \"highlights\": [${report.highlights.joinToString(", ") { "\"$it\"" }}],")
            appendLine("  \"created_at\": \"${report.createdAt}\"")
            append("}")
        }
    }
}
