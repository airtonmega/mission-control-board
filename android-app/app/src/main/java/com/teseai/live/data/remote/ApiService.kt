package com.teseai.live.data.remote

import com.teseai.live.data.remote.dto.AnalyzeImageResponse
import com.teseai.live.data.remote.dto.AnalyzeTextRequest
import com.teseai.live.data.remote.dto.AnalyzeTextResponse
import com.teseai.live.data.remote.dto.AnonymousAuthResponse
import com.teseai.live.data.remote.dto.ConsentAcceptRequest
import com.teseai.live.data.remote.dto.ConsentAcceptResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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
}
