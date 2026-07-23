package com.example.aimentor.network;

import com.example.aimentor.network.model.ChatCompletionRequest;
import com.example.aimentor.network.model.ChatCompletionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/** OpenAI-compatible Groq chat-completions endpoint. */
public interface GroqApiService {

    @Headers("Content-Type: application/json")
    @POST("openai/v1/chat/completions")
    Call<ChatCompletionResponse> createCompletion(
            @Header("Authorization") String authorization,
            @Body ChatCompletionRequest request);
}
