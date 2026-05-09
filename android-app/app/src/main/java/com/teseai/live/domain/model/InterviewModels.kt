package com.teseai.live.domain.model

data class InterviewSession(
    val sessionId: String,
    val interviewId: String,
    val firstQuestion: String,
    val totalQuestions: Int,
    val area: String,
    val level: String,
)

data class InterviewEvaluation(
    val questionNumber: Int,
    val totalQuestions: Int,
    val question: String,
    val score: Float,
    val accuracy: Float,
    val clarity: Float,
    val depth: Float,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val improvedAnswer: String,
    val nextQuestion: String?,
    val completed: Boolean,
)

data class InterviewReport(
    val area: String,
    val level: String,
    val totalQuestions: Int,
    val averageScore: Float,
    val averageAccuracy: Float,
    val averageClarity: Float,
    val averageDepth: Float,
    val evaluations: List<InterviewEvaluation>,
    val topStrengths: List<String>,
    val topWeaknesses: List<String>,
)
