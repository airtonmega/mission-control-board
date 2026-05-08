package com.teseai.live.data.repository

import com.google.gson.Gson
import com.teseai.live.data.remote.TeseAIApiService
import com.teseai.live.data.remote.dto.AnalyzeTextRequest
import com.teseai.live.data.remote.dto.ApiErrorEnvelope
import com.teseai.live.data.remote.dto.ConsentAcceptRequest
import com.teseai.live.domain.model.AnalysisResult
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzeRepository @Inject constructor(
    private val api: TeseAIApiService,
) {
    private val gson = Gson()
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

    suspend fun analyzeText(question: String): Result<AnalysisResult> {
        return try {
            val response = api.analyzeText(
                AnalyzeTextRequest(
                    sessionId = sessionId.ifEmpty { "anonymous" },
                    question = question,
                )
            )
            Result.success(
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
            )
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses the structured error body from the backend.
     * Backend returns: {"detail": {"error_code": "...", "error_message": "...", ...}}
     * Falls back to a generic message if the body is absent or malformed.
     */
    private fun parseHttpError(e: HttpException): String {
        return try {
            val raw = e.response()?.errorBody()?.string() ?: return genericErrorMessage(e.code())
            val envelope = gson.fromJson(raw, ApiErrorEnvelope::class.java)
            envelope.detail?.errorMessage ?: genericErrorMessage(e.code())
        } catch (_: Exception) {
            genericErrorMessage(e.code())
        }
    }

    private fun genericErrorMessage(code: Int): String = when (code) {
        422 -> "A IA retornou uma resposta inválida. Reformule a pergunta."
        503 -> "Serviço de IA temporariamente indisponível. Tente novamente."
        500 -> "Erro interno no servidor. Tente novamente em instantes."
        else -> "Erro ao analisar pergunta (código $code)."
    }

    fun getSessionId(): String = sessionId
}
