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
