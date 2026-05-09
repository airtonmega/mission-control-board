package com.teseai.live.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InterviewStartRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("area") val area: String,
    @SerializedName("level") val level: String,
    @SerializedName("duration_minutes") val durationMinutes: Int = 30,
)

data class InterviewStartResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("interview_id") val interviewId: String,
    @SerializedName("first_question") val firstQuestion: String,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("area") val area: String,
    @SerializedName("level") val level: String,
)

data class InterviewEvaluateRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("interview_id") val interviewId: String,
    @SerializedName("question") val question: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("question_number") val questionNumber: Int,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("area") val area: String,
    @SerializedName("level") val level: String,
)

data class InterviewEvaluateResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("interview_id") val interviewId: String,
    @SerializedName("question_number") val questionNumber: Int,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("score") val score: Float,
    @SerializedName("accuracy") val accuracy: Float,
    @SerializedName("clarity") val clarity: Float,
    @SerializedName("depth") val depth: Float,
    @SerializedName("strengths") val strengths: List<String>,
    @SerializedName("weaknesses") val weaknesses: List<String>,
    @SerializedName("improved_answer") val improvedAnswer: String,
    @SerializedName("next_question") val nextQuestion: String?,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("mock") val mock: Boolean,
)
