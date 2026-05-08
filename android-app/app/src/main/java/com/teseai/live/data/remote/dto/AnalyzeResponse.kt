package com.teseai.live.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyzeTextResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("detected_theme") val detectedTheme: String,
    @SerializedName("quick_tip") val quickTip: String,
    @SerializedName("short_answer") val shortAnswer: String,
    @SerializedName("interview_answer") val interviewAnswer: String,
    @SerializedName("complete_answer") val completeAnswer: String,
    @SerializedName("common_errors") val commonErrors: List<String>,
    @SerializedName("study_suggestions") val studySuggestions: List<String>,
    @SerializedName("confidence_score") val confidenceScore: Float,
    @SerializedName("processing_time_ms") val processingTimeMs: Int,
    @SerializedName("mock") val mock: Boolean,
)

data class AnonymousAuthResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("token") val token: String,
    @SerializedName("message") val message: String,
)

data class ConsentAcceptResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("consent_recorded") val consentRecorded: Boolean,
    @SerializedName("message") val message: String,
)

// ── Error response (mirrors AnalyzeErrorDetail from backend) ─────────────────

/** Inner payload of FastAPI's {"detail": {...}} error envelope. */
data class ApiErrorResponse(
    @SerializedName("error_code") val errorCode: String,
    @SerializedName("error_message") val errorMessage: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("repair_attempted") val repairAttempted: Boolean = false,
)

/** Outer FastAPI error envelope: {"detail": <ApiErrorResponse>}. */
data class ApiErrorEnvelope(
    @SerializedName("detail") val detail: ApiErrorResponse?,
)

// ── Audio analysis response (mirrors AnalyzeTextResponse + transcription) ────

data class AnalyzeAudioResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("transcription") val transcription: String,
    @SerializedName("detected_theme") val detectedTheme: String,
    @SerializedName("quick_tip") val quickTip: String,
    @SerializedName("short_answer") val shortAnswer: String,
    @SerializedName("interview_answer") val interviewAnswer: String,
    @SerializedName("complete_answer") val completeAnswer: String,
    @SerializedName("common_errors") val commonErrors: List<String>,
    @SerializedName("study_suggestions") val studySuggestions: List<String>,
    @SerializedName("confidence_score") val confidenceScore: Float,
    @SerializedName("processing_time_ms") val processingTimeMs: Int,
    @SerializedName("mock") val mock: Boolean,
)

// ── Image analysis response (mirrors AnalyzeTextResponse + detected_text) ───

data class AnalyzeImageResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("detected_text") val detectedText: String,
    @SerializedName("detected_theme") val detectedTheme: String,
    @SerializedName("quick_tip") val quickTip: String,
    @SerializedName("short_answer") val shortAnswer: String,
    @SerializedName("interview_answer") val interviewAnswer: String,
    @SerializedName("complete_answer") val completeAnswer: String,
    @SerializedName("common_errors") val commonErrors: List<String>,
    @SerializedName("study_suggestions") val studySuggestions: List<String>,
    @SerializedName("confidence_score") val confidenceScore: Float,
    @SerializedName("processing_time_ms") val processingTimeMs: Int,
    @SerializedName("mock") val mock: Boolean,
)
