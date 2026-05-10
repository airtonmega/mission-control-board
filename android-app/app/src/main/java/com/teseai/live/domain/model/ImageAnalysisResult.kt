package com.teseai.live.domain.model

data class ImageAnalysisResult(
    val sessionId: String,
    val detectedText: String,
    val detectedTheme: String,
    val quickTip: String,
    val shortAnswer: String,
    val interviewAnswer: String,
    val completeAnswer: String,
    val commonErrors: List<String>,
    val studySuggestions: List<String>,
    val confidenceScore: Float,
    val processingTimeMs: Int,
)
