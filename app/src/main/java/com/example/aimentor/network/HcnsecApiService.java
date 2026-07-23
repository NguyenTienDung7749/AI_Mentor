package com.example.aimentor.network;

import com.example.aimentor.network.model.ChatCompletionRequest;
import com.example.aimentor.network.model.ChatCompletionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/** OpenAI-compatible chat-completions endpoint exposed by HCNSEC. */
public interface HcnsecApiService {

    @POST("chat/completions")
    Call<ChatCompletionResponse> createCompletion(
            @Header("Authorization") String authorization,
            @Body ChatCompletionRequest request);
}
