package com.teseai.live.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teseai.live.audio.AudioCaptureManager
import com.teseai.live.data.repository.AnalyzeRepository
import com.teseai.live.domain.model.AudioAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AudioUiState {
    data object PermissionRequired : AudioUiState
    data object Ready : AudioUiState
    data class Recording(val durationSecs: Int) : AudioUiState
    data object Uploading : AudioUiState
    data class Result(val result: AudioAnalysisResult) : AudioUiState
    data class Error(val message: String) : AudioUiState
}

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AnalyzeRepository,
    private val audioCaptureManager: AudioCaptureManager,
) : ViewModel() {

    private val _state = MutableStateFlow<AudioUiState>(AudioUiState.PermissionRequired)
    val state: StateFlow<AudioUiState> = _state.asStateFlow()

    private var durationJob: Job? = null

    fun onPermissionGranted() {
        _state.value = AudioUiState.Ready
    }

    fun onPermissionDenied() {
        _state.value = AudioUiState.PermissionRequired
    }

    fun startRecording() {
        try {
            audioCaptureManager.startRecording()
            durationJob = viewModelScope.launch {
                var secs = 0
                _state.value = AudioUiState.Recording(secs)
                while (true) {
                    delay(1000)
                    secs++
                    _state.value = AudioUiState.Recording(secs)
                }
            }
        } catch (e: Exception) {
            _state.value = AudioUiState.Error("Erro ao iniciar gravação: ${e.localizedMessage}")
        }
    }

    fun stopRecording() {
        durationJob?.cancel()
        durationJob = null
        _state.value = AudioUiState.Uploading

        viewModelScope.launch {
            val file = audioCaptureManager.stopRecording()
            if (file == null) {
                _state.value = AudioUiState.Error("Erro ao finalizar gravação. Tente novamente.")
                return@launch
            }
            repository.analyzeAudio(file)
                .onSuccess { result -> _state.value = AudioUiState.Result(result) }
                .onFailure { err ->
                    _state.value = AudioUiState.Error(
                        err.localizedMessage ?: "Erro ao analisar áudio"
                    )
                }
        }
    }

    fun reset() {
        _state.value = AudioUiState.Ready
    }

    override fun onCleared() {
        durationJob?.cancel()
        audioCaptureManager.release()
    }
}
