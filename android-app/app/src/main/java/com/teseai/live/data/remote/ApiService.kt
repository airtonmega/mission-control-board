package com.teseai.live.data.remote

import com.teseai.live.data.remote.dto.AnalyzeAudioResponse
import com.teseai.live.data.remote.dto.AnalyzeImageResponse
import com.teseai.live.data.remote.dto.AnalyzeTextRequest
import com.teseai.live.data.remote.dto.AnalyzeTextResponse
import com.teseai.live.data.remote.dto.AnonymousAuthResponse
import com.teseai.live.data.remote.dto.ConsentAcceptRequest
import com.teseai.live.data.remote.dto.ConsentAcceptResponse
import com.teseai.live.data.remote.dto.InterviewEvaluateRequest
import com.teseai.live.data.remote.dto.InterviewEvaluateResponse
import com.teseai.live.data.remote.dto.InterviewStartRequest
import com.teseai.live.data.remote.dto.InterviewStartResponse
import com.teseai.live.data.remote.dto.RealtimeSessionRequest
import com.teseai.live.data.remote.dto.RealtimeSessionResponse
import com.teseai.live.data.remote.dto.SessionReportResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface TeseAIApiService {
    @POST("auth/anonymous")
    suspend fun createAnonymousSession(): AnonymousAuthResponse

    @POST("consent/accept")
    suspend fun acceptConsent(@Body request: ConsentAcceptRequest): ConsentAcceptResponse

    @POST("analyze/text")
    suspend fun analyzeText(@Body request: AnalyzeTextRequest): AnalyzeTextResponse

    @Multipart
    @POST("analyze/image")
    suspend fun analyzeImage(
        @Part("session_id") sessionId: RequestBody,
        @Part image: MultipartBody.Part,
    ): AnalyzeImageResponse

    @Multipart
    @POST("analyze/audio")
    suspend fun analyzeAudio(
        @Part("session_id") sessionId: RequestBody,
        @Part audio: MultipartBody.Part,
    ): AnalyzeAudioResponse

    @POST("interview/start")
    suspend fun startInterview(@Body request: InterviewStartRequest): InterviewStartResponse

    @POST("interview/evaluate")
    suspend fun evaluateInterview(@Body request: InterviewEvaluateRequest): InterviewEvaluateResponse

    @GET("reports/session/{session_id}")
    suspend fun getSessionReport(@Path("session_id") sessionId: String): SessionReportResponse

    @POST("realtime/session")
    suspend fun createRealtimeSession(@Body request: RealtimeSessionRequest): RealtimeSessionResponse
}
