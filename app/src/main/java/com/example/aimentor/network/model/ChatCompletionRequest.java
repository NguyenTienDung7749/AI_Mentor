package com.example.aimentor.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Request body for POST /chat/completions. */
public class ChatCompletionRequest {
    @SerializedName("model")
    public String model;

    @SerializedName("messages")
    public List<ChatMessage> messages;

    @SerializedName("temperature")
    public double temperature;

    @SerializedName("stream")
    public boolean stream;

    @SerializedName("max_tokens")
    public int maxTokens;

    @SerializedName("reasoning_effort")
    public String reasoningEffort;

    @SerializedName("reasoning_format")
    public String reasoningFormat;

    @SerializedName("response_format")
    public Object responseFormat;

    public ChatCompletionRequest(String model, List<ChatMessage> messages,
                                 double temperature, int maxTokens, boolean stream) {
        this(model, messages, temperature, maxTokens, stream,
                null, null, null);
    }

    public ChatCompletionRequest(String model, List<ChatMessage> messages,
                                 double temperature, int maxTokens, boolean stream,
                                 String reasoningEffort, String reasoningFormat,
                                 Object responseFormat) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
        this.reasoningEffort = reasoningEffort;
        this.reasoningFormat = reasoningFormat;
        this.responseFormat = responseFormat;
    }
}
