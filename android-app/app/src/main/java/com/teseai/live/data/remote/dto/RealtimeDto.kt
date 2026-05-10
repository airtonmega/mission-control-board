package com.teseai.live.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RealtimeSessionRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("voice") val voice: String = "alloy",
    @SerializedName("language") val language: String = "pt",
)

data class RealtimeClientSecret(
    @SerializedName("value") val value: String,
    @SerializedName("expires_at") val expiresAt: Long,
)

data class RealtimeSessionResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("realtime_session_id") val realtimeSessionId: String,
    @SerializedName("client_secret") val clientSecret: RealtimeClientSecret,
    @SerializedName("model") val model: String,
    @SerializedName("expires_at") val expiresAt: Long,
    @SerializedName("voice") val voice: String,
)
