package com.vxai.chat.data.api

import com.vxai.chat.data.model.ChatRequest
import com.vxai.chat.data.model.ChatResponse
import com.vxai.chat.data.model.ModelList
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface NineRouterApi {
    @GET("models")
    suspend fun models(): ModelList

    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
