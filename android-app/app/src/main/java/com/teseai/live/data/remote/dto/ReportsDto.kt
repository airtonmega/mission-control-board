package com.teseai.live.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InputModeCount(
    @SerializedName("text") val text: Int = 0,
    @SerializedName("image") val image: Int = 0,
    @SerializedName("audio") val audio: Int = 0,
    @SerializedName("interview") val interview: Int = 0,
)

data class SessionReportResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("total_queries") val totalQueries: Int,
    @SerializedName("topics_covered") val topicsCovered: List<String>,
    @SerializedName("average_confidence") val averageConfidence: Float,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("highlights") val highlights: List<String>,
    @SerializedName("input_modes") val inputModes: InputModeCount,
    @SerializedName("average_latency_ms") val averageLatencyMs: Float?,
    @SerializedName("interview_average_score") val interviewAverageScore: Float?,
    @SerializedName("events") val events: List<Map<String, Any>> = emptyList(),
)
