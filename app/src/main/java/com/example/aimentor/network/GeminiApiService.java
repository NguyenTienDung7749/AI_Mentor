package com.example.aimentor.network;

import com.example.aimentor.network.model.GeminiGenerateContentRequest;
import com.example.aimentor.network.model.GeminiGenerateContentResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/** Google Gemini multimodal generateContent endpoint used only for rescue. */
public interface GeminiApiService {

    @Headers("Content-Type: application/json")
    @POST("v1beta/models/gemini-3.6-flash:generateContent")
    Call<GeminiGenerateContentResponse> generateContent(
            @Header("x-goog-api-key") String apiKey,
            @Body GeminiGenerateContentRequest request);
}
