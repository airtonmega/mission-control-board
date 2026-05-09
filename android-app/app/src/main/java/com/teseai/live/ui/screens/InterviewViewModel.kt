package com.teseai.live.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teseai.live.data.repository.AnalyzeRepository
import com.teseai.live.data.repository.InterviewRepository
import com.teseai.live.domain.model.InterviewEvaluation
import com.teseai.live.domain.model.InterviewReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InterviewUiState {
    data object Loading : InterviewUiState
    data class Question(
        val question: String,
        val questionNumber: Int,
        val totalQuestions: Int,
        val interviewId: String,
        val area: String,
        val level: String,
    ) : InterviewUiState
    data class Evaluating(
        val questionNumber: Int,
        val totalQuestions: Int,
    ) : InterviewUiState
    data class EvaluationResult(
        val evaluation: InterviewEvaluation,
        val interviewId: String,
        val area: String,
        val level: String,
    ) : InterviewUiState
    data class Report(val report: InterviewReport) : InterviewUiState
    data class Error(val message: String) : InterviewUiState
}

@HiltViewModel
class InterviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val interviewRepo: InterviewRepository,
    private val analyzeRepo: AnalyzeRepository,
) : ViewModel() {

    private val area: String = savedStateHandle["area"] ?: "Android"
    private val level: String = savedStateHandle["level"] ?: "Pleno"

    private val _uiState = MutableStateFlow<InterviewUiState>(InterviewUiState.Loading)
    val uiState: StateFlow<InterviewUiState> = _uiState.asStateFlow()

    private val evaluations = mutableListOf<InterviewEvaluation>()

    init {
        startInterview()
    }

    private fun startInterview() {
        viewModelScope.launch {
            _uiState.value = InterviewUiState.Loading
            try {
                val sessionId = analyzeRepo.getSessionId()
                val response = interviewRepo.startInterview(sessionId, area, level)
                _uiState.value = InterviewUiState.Question(
                    question = response.firstQuestion,
                    questionNumber = 1,
                    totalQuestions = response.totalQuestions,
                    interviewId = response.interviewId,
                    area = area,
                    level = level,
                )
            } catch (e: Exception) {
                _uiState.value = InterviewUiState.Error(e.message ?: "Erro ao iniciar entrevista")
            }
        }
    }

    fun submitAnswer(answer: String) {
        val s = _uiState.value as? InterviewUiState.Question ?: return
        viewModelScope.launch {
            _uiState.value = InterviewUiState.Evaluating(s.questionNumber, s.totalQuestions)
            try {
                val sessionId = analyzeRepo.getSessionId()
                val resp = interviewRepo.evaluateAnswer(
                    sessionId = sessionId,
                    interviewId = s.interviewId,
                    question = s.question,
                    answer = answer,
                    questionNumber = s.questionNumber,
                    totalQuestions = s.totalQuestions,
                    area = s.area,
                    level = s.level,
                )
                val eval = InterviewEvaluation(
                    questionNumber = resp.questionNumber,
                    totalQuestions = resp.totalQuestions,
                    question = s.question,
                    score = resp.score,
                    accuracy = resp.accuracy,
                    clarity = resp.clarity,
                    depth = resp.depth,
                    strengths = resp.strengths,
                    weaknesses = resp.weaknesses,
                    improvedAnswer = resp.improvedAnswer,
                    nextQuestion = resp.nextQuestion,
                    completed = resp.completed,
                )
                evaluations.add(eval)
                _uiState.value = InterviewUiState.EvaluationResult(
                    evaluation = eval,
                    interviewId = s.interviewId,
                    area = s.area,
                    level = s.level,
                )
            } catch (e: Exception) {
                _uiState.value = InterviewUiState.Error(e.message ?: "Erro ao avaliar resposta")
            }
        }
    }

    fun nextQuestion() {
        val s = _uiState.value as? InterviewUiState.EvaluationResult ?: return
        if (s.evaluation.completed || s.evaluation.nextQuestion == null) {
            buildReport()
            return
        }
        _uiState.value = InterviewUiState.Question(
            question = s.evaluation.nextQuestion,
            questionNumber = s.evaluation.questionNumber + 1,
            totalQuestions = s.evaluation.totalQuestions,
            interviewId = s.interviewId,
            area = s.area,
            level = s.level,
        )
    }

    fun finishInterview() {
        buildReport()
    }

    private fun buildReport() {
        if (evaluations.isEmpty()) return
        fun avg(f: (InterviewEvaluation) -> Float) = evaluations.map(f).average().toFloat()
        val report = InterviewReport(
            area = area,
            level = level,
            totalQuestions = evaluations.size,
            averageScore = avg { it.score },
            averageAccuracy = avg { it.accuracy },
            averageClarity = avg { it.clarity },
            averageDepth = avg { it.depth },
            evaluations = evaluations.toList(),
            topStrengths = evaluations.flatMap { it.strengths }.distinct().take(4),
            topWeaknesses = evaluations.flatMap { it.weaknesses }.distinct().take(3),
        )
        _uiState.value = InterviewUiState.Report(report)
    }

    fun retry() {
        evaluations.clear()
        startInterview()
    }
}
