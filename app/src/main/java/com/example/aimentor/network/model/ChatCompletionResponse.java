package com.example.aimentor.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Minimal response model; fields not needed by the app are intentionally omitted. */
public class ChatCompletionResponse {
    @SerializedName("id")
    public String id;

    @SerializedName("model")
    public String model;

    @SerializedName("choices")
    public List<Choice> choices;

    @SerializedName("usage")
    public Usage usage;

    public static class Choice {
        @SerializedName("index")
        public int index;

        @SerializedName("message")
        public ChatMessage message;

        @SerializedName("finish_reason")
        public String finishReason;
    }

    public static class Usage {
        @SerializedName("prompt_tokens")
        public int promptTokens;

        @SerializedName("completion_tokens")
        public int completionTokens;

        @SerializedName("total_tokens")
        public int totalTokens;
    }
}
