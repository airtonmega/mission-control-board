package com.teseai.live.data.repository

import com.teseai.live.data.remote.TeseAIApiService
import com.teseai.live.data.remote.dto.AnalyzeTextRequest
import com.teseai.live.data.remote.dto.ConsentAcceptRequest
import com.teseai.live.domain.model.AnalysisResult
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzeRepository @Inject constructor(
    private val api: TeseAIApiService,
) {
    private var sessionId: String = ""
    private var token: String = ""

    suspend fun initSession(): Result<String> = runCatching {
        val response = api.createAnonymousSession()
        sessionId = response.sessionId
        token = response.token
        sessionId
    }

    suspend fun recordConsent(accepted: Boolean): Result<Unit> = runCatching {
        api.acceptConsent(
            ConsentAcceptRequest(
                sessionId = sessionId,
                accepted = accepted,
                timestamp = Instant.now().toString(),
            )
        )
        Unit
    }

    suspend fun analyzeText(question: String): Result<AnalysisResult> = runCatching {
        val response = api.analyzeText(
            AnalyzeTextRequest(
                sessionId = sessionId.ifEmpty { "anonymous" },
                question = question,
            )
        )
        AnalysisResult(
            sessionId = response.sessionId,
            detectedTheme = response.detectedTheme,
            quickTip = response.quickTip,
            shortAnswer = response.shortAnswer,
            interviewAnswer = response.interviewAnswer,
            completeAnswer = response.completeAnswer,
            commonErrors = response.commonErrors,
            studySuggestions = response.studySuggestions,
            confidenceScore = response.confidenceScore,
            processingTimeMs = response.processingTimeMs,
            isMock = response.mock,
        )
    }

    fun getSessionId(): String = sessionId
}
