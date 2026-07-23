package com.example.aimentor.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Minimal response model; fields not needed by the app are intentionally omitted. */
public class ChatCompletionResponse {
    public String id;
    public String model;
    public List<Choice> choices;
    public Usage usage;

    public static class Choice {
        public int index;
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
