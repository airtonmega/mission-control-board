package com.teseai.live.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.teseai.live.data.remote.TeseAIApiService
import com.teseai.live.data.remote.dto.AnalyzeTextRequest
import com.teseai.live.data.remote.dto.ApiErrorEnvelope
import com.teseai.live.data.remote.dto.ConsentAcceptRequest
import com.teseai.live.domain.model.AnalysisResult
import com.teseai.live.domain.model.AudioAnalysisResult
import com.teseai.live.domain.model.ImageAnalysisResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
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

    suspend fun analyzeImage(imageFile: File): Result<ImageAnalysisResult> {
        return try {
            val compressed = compressImage(imageFile)
            val sessionIdBody = sessionId.ifEmpty { "anonymous" }
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "image",
                compressed.name,
                compressed.asRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            val response = api.analyzeImage(sessionIdBody, imagePart)
            Result.success(
                ImageAnalysisResult(
                    sessionId = response.sessionId,
                    detectedText = response.detectedText,
                    detectedTheme = response.detectedTheme,
                    quickTip = response.quickTip,
                    shortAnswer = response.shortAnswer,
                    interviewAnswer = response.interviewAnswer,
                    completeAnswer = response.completeAnswer,
                    commonErrors = response.commonErrors,
                    studySuggestions = response.studySuggestions,
                    confidenceScore = response.confidenceScore,
                    processingTimeMs = response.processingTimeMs,
                )
            )
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressImage(original: File, maxDimension: Int = 1024, quality: Int = 80): File {
        val bitmap = BitmapFactory.decodeFile(original.absolutePath)
            ?: return original
        val scale = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true,
            )
        } else {
            bitmap
        }
        val out = File(original.parent, "compressed_${original.name}")
        FileOutputStream(out).use { scaled.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        return out
    }

    suspend fun analyzeAudio(audioFile: File): Result<AudioAnalysisResult> {
        return try {
            val sessionIdBody = sessionId.ifEmpty { "anonymous" }
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData(
                "audio",
                audioFile.name,
                audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull()),
            )
            val response = api.analyzeAudio(sessionIdBody, audioPart)
            Result.success(
                AudioAnalysisResult(
                    sessionId = response.sessionId,
                    transcription = response.transcription,
                    detectedTheme = response.detectedTheme,
                    quickTip = response.quickTip,
                    shortAnswer = response.shortAnswer,
                    interviewAnswer = response.interviewAnswer,
                    completeAnswer = response.completeAnswer,
                    commonErrors = response.commonErrors,
                    studySuggestions = response.studySuggestions,
                    confidenceScore = response.confidenceScore,
                    processingTimeMs = response.processingTimeMs,
                )
            )
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSessionId(): String = sessionId
}
