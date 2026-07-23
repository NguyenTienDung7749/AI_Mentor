package com.example.aimentor.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Request body for POST /chat/completions. */
public class ChatCompletionRequest {
    public String model;
    public List<ChatMessage> messages;
    public double temperature;
    public boolean stream;

    @SerializedName("max_tokens")
    public int maxTokens;

    public ChatCompletionRequest(String model, List<ChatMessage> messages,
                                 double temperature, int maxTokens, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
    }
}
