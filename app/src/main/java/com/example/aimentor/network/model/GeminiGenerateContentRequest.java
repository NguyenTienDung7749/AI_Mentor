package com.example.aimentor.network.model;

import com.example.aimentor.ai.ImageAttachment;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Minimal Gemini request containing one image and one text instruction. */
public final class GeminiGenerateContentRequest {

    @SerializedName("contents")
    public final List<Content> contents;

    @SerializedName("generationConfig")
    public final GenerationConfig generationConfig;

    public GeminiGenerateContentRequest(String prompt, ImageAttachment image) {
        Part imagePart = Part.image(image.getMimeType(), image.getBase64Data());
        Part textPart = Part.text(prompt);
        this.contents = Collections.singletonList(
                new Content(Arrays.asList(imagePart, textPart)));
        this.generationConfig = new GenerationConfig();
    }

    public static final class Content {
        @SerializedName("parts")
        public final List<Part> parts;

        Content(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static final class Part {
        @SerializedName("text")
        public final String text;

        @SerializedName("inline_data")
        public final InlineData inlineData;

        private Part(String text, InlineData inlineData) {
            this.text = text;
            this.inlineData = inlineData;
        }

        static Part text(String text) {
            return new Part(text, null);
        }

        static Part image(String mimeType, String data) {
            return new Part(null, new InlineData(mimeType, data));
        }
    }

    public static final class InlineData {
        @SerializedName("mime_type")
        public final String mimeType;

        @SerializedName("data")
        public final String data;

        InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }
    }

    public static final class GenerationConfig {
        @SerializedName("maxOutputTokens")
        public final int maxOutputTokens = 3000;

        @SerializedName("responseMimeType")
        public final String responseMimeType = "application/json";
    }
}
