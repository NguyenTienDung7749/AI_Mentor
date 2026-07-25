package com.example.aimentor.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Minimal text response model for Gemini generateContent. */
public final class GeminiGenerateContentResponse {

    @SerializedName("candidates")
    public List<Candidate> candidates;

    public String firstText() {
        if (candidates == null || candidates.isEmpty()
                || candidates.get(0) == null
                || candidates.get(0).content == null
                || candidates.get(0).content.parts == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (Part part : candidates.get(0).content.parts) {
            if (part != null && part.text != null) text.append(part.text);
        }
        return text.toString().trim();
    }

    public static final class Candidate {
        @SerializedName("content")
        public Content content;

        @SerializedName("finishReason")
        public String finishReason;
    }

    public static final class Content {
        @SerializedName("parts")
        public List<Part> parts;
    }

    public static final class Part {
        @SerializedName("text")
        public String text;
    }
}
