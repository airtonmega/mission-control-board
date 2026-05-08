package com.teseai.live.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyzeTextRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("question") val question: String,
    @SerializedName("context") val context: String? = null,
    @SerializedName("language") val language: String = "pt-BR",
)

data class ConsentAcceptRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("accepted") val accepted: Boolean,
    @SerializedName("timestamp") val timestamp: String,
)
