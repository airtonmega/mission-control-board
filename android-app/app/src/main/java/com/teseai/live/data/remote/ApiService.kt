package com.teseai.live.data.remote

import com.teseai.live.data.remote.dto.AnalyzeTextRequest
import com.teseai.live.data.remote.dto.AnalyzeTextResponse
import com.teseai.live.data.remote.dto.AnonymousAuthResponse
import com.teseai.live.data.remote.dto.ConsentAcceptRequest
import com.teseai.live.data.remote.dto.ConsentAcceptResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TeseAIApiService {
    @POST("auth/anonymous")
    suspend fun createAnonymousSession(): AnonymousAuthResponse

    @POST("consent/accept")
    suspend fun acceptConsent(@Body request: ConsentAcceptRequest): ConsentAcceptResponse

    @POST("analyze/text")
    suspend fun analyzeText(@Body request: AnalyzeTextRequest): AnalyzeTextResponse
}
