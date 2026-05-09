package com.teseai.live.data.repository

import com.teseai.live.data.remote.TeseAIApiService
import com.teseai.live.data.remote.dto.RealtimeSessionRequest
import com.teseai.live.data.remote.dto.RealtimeSessionResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeRepository @Inject constructor(
    private val api: TeseAIApiService,
) {
    suspend fun createSession(sessionId: String): RealtimeSessionResponse =
        api.createRealtimeSession(RealtimeSessionRequest(sessionId = sessionId))
}
