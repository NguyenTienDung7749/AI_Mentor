package com.example.aimentor.network.model;

import com.google.gson.annotations.SerializedName;

/** A request or response message in the OpenAI-compatible chat format. */
public class ChatMessage {
    public String role;
    public String content;

    @SerializedName("reasoning_content")
    public String reasoningContent;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
