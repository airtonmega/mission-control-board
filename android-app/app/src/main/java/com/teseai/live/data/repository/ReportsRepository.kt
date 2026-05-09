package com.teseai.live.data.repository

import com.teseai.live.data.remote.TeseAIApiService
import com.teseai.live.data.remote.dto.SessionReportResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportsRepository @Inject constructor(
    private val api: TeseAIApiService,
) {
    suspend fun getSessionReport(sessionId: String): SessionReportResponse =
        api.getSessionReport(sessionId)
}
