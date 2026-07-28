package com.example.aimentor.network;

import com.example.aimentor.network.model.ChatCompletionRequest;
import com.example.aimentor.network.model.ChatCompletionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/** Mistral chat-completions endpoint used only for one-image study questions. */
public interface MistralApiService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    Call<ChatCompletionResponse> createCompletion(
            @Header("Authorization") String authorization,
            @Body ChatCompletionRequest request);
}
