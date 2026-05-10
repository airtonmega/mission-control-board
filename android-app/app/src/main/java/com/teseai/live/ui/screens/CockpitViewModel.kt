package com.teseai.live.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teseai.live.data.repository.AnalyzeRepository
import com.teseai.live.domain.model.AnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CockpitUiState {
    data object Idle : CockpitUiState
    data object Loading : CockpitUiState
    data class Success(val result: AnalysisResult) : CockpitUiState
    data class Error(val message: String) : CockpitUiState
}

data class CockpitState(
    val inputText: String = "",
    val analysisState: CockpitUiState = CockpitUiState.Idle,
    val isCameraActive: Boolean = false,
    val isMicActive: Boolean = false,
    val sessionId: String = "",
)

@HiltViewModel
class CockpitViewModel @Inject constructor(
    private val repository: AnalyzeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CockpitState())
    val state: StateFlow<CockpitState> = _state.asStateFlow()

    fun onInputChanged(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun analyzeText() {
        val question = _state.value.inputText.trim()
        if (question.length < 3) return

        viewModelScope.launch {
            _state.update { it.copy(analysisState = CockpitUiState.Loading) }
            repository.analyzeText(question)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            analysisState = CockpitUiState.Success(result),
                            sessionId = result.sessionId,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            analysisState = CockpitUiState.Error(
                                throwable.localizedMessage ?: "Erro desconhecido"
                            )
                        )
                    }
                }
        }
    }

    fun clearResult() {
        _state.update { it.copy(analysisState = CockpitUiState.Idle, inputText = "") }
    }

    fun toggleCamera(active: Boolean) {
        _state.update { it.copy(isCameraActive = active) }
    }

    fun toggleMic(active: Boolean) {
        _state.update { it.copy(isMicActive = active) }
    }
}
