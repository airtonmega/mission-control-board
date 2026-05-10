package com.teseai.live.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teseai.live.data.repository.AnalyzeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConsentUiState(
    val isLoading: Boolean = false,
    val consentRecorded: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val repository: AnalyzeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initSession()
        }
    }

    fun acceptConsent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.recordConsent(accepted = true)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, consentRecorded = true) }
                }
                .onFailure { throwable ->
                    // Accept consent locally even if API fails — offline resilience
                    _uiState.update {
                        it.copy(isLoading = false, consentRecorded = true, error = null)
                    }
                }
        }
    }
}
