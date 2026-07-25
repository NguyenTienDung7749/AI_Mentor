package com.example.aimentor.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** A request or response message in the OpenAI-compatible chat format. */
public class ChatMessage {
    @SerializedName("role")
    public String role;

    @SerializedName("content")
    public Object content;

    @SerializedName("reasoning_content")
    public String reasoningContent;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public ChatMessage(String role, List<ContentPart> content) {
        this.role = role;
        this.content = content;
    }

    public String textContent() {
        return content instanceof String ? (String) content : "";
    }

    public static final class ContentPart {
        @SerializedName("type")
        public final String type;

        @SerializedName("text")
        public final String text;

        @SerializedName("image_url")
        public final ImageUrl imageUrl;

        private ContentPart(String type, String text, ImageUrl imageUrl) {
            this.type = type;
            this.text = text;
            this.imageUrl = imageUrl;
        }

        public static ContentPart text(String text) {
            return new ContentPart("text", text, null);
        }

        public static ContentPart image(String dataUrl) {
            return new ContentPart("image_url", null, new ImageUrl(dataUrl));
        }
    }

    public static final class ImageUrl {
        @SerializedName("url")
        public final String url;

        ImageUrl(String url) {
            this.url = url;
        }
    }
}
