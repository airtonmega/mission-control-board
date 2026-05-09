package com.teseai.live.domain.model

data class SessionReportModel(
    val sessionId: String,
    val totalQueries: Int,
    val topicsCovered: List<String>,
    val averageConfidence: Float,
    val durationSeconds: Int,
    val highlights: List<String>,
    val textCount: Int,
    val imageCount: Int,
    val audioCount: Int,
    val interviewCount: Int,
    val averageLatencyMs: Float?,
    val interviewAverageScore: Float?,
)
