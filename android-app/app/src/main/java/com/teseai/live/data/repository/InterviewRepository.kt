package com.teseai.live.data.repository

import com.teseai.live.data.remote.TeseAIApiService
import com.teseai.live.data.remote.dto.InterviewEvaluateRequest
import com.teseai.live.data.remote.dto.InterviewEvaluateResponse
import com.teseai.live.data.remote.dto.InterviewStartRequest
import com.teseai.live.data.remote.dto.InterviewStartResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterviewRepository @Inject constructor(
    private val api: TeseAIApiService,
) {
    suspend fun startInterview(
        sessionId: String,
        area: String,
        level: String,
    ): InterviewStartResponse = api.startInterview(
        InterviewStartRequest(sessionId = sessionId, area = area, level = level)
    )

    suspend fun evaluateAnswer(
        sessionId: String,
        interviewId: String,
        question: String,
        answer: String,
        questionNumber: Int,
        totalQuestions: Int,
        area: String,
        level: String,
    ): InterviewEvaluateResponse = api.evaluateInterview(
        InterviewEvaluateRequest(
            sessionId = sessionId,
            interviewId = interviewId,
            question = question,
            answer = answer,
            questionNumber = questionNumber,
            totalQuestions = totalQuestions,
            area = area,
            level = level,
        )
    )
}
