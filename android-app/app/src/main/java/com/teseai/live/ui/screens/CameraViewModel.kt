package com.teseai.live.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teseai.live.data.repository.AnalyzeRepository
import com.teseai.live.domain.model.ImageAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface CameraUiState {
    data object PermissionRequired : CameraUiState
    data object Ready : CameraUiState
    data object Capturing : CameraUiState
    data object Analyzing : CameraUiState
    data class Result(val result: ImageAnalysisResult) : CameraUiState
    data class Error(val message: String) : CameraUiState
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: AnalyzeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<CameraUiState>(CameraUiState.PermissionRequired)
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    fun onPermissionGranted() {
        _state.value = CameraUiState.Ready
    }

    fun onPermissionDenied() {
        _state.value = CameraUiState.PermissionRequired
    }

    fun onCapturing() {
        _state.value = CameraUiState.Capturing
    }

    fun analyzeImage(imageFile: File) {
        _state.value = CameraUiState.Analyzing
        viewModelScope.launch {
            repository.analyzeImage(imageFile)
                .onSuccess { result -> _state.value = CameraUiState.Result(result) }
                .onFailure { error ->
                    _state.value = CameraUiState.Error(
                        error.localizedMessage ?: "Erro ao analisar imagem"
                    )
                }
        }
    }

    fun reset() {
        _state.value = CameraUiState.Ready
    }
}
