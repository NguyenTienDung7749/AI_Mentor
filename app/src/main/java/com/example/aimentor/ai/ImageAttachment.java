package com.example.aimentor.ai;

/** A single compressed image prepared locally for a multimodal AI request. */
public final class ImageAttachment {

    private final String mimeType;
    private final String base64Data;

    public ImageAttachment(String mimeType, String base64Data) {
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("A valid image MIME type is required.");
        }
        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException("Image data is required.");
        }
        this.mimeType = mimeType;
        this.base64Data = base64Data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public String toDataUrl() {
        return "data:" + mimeType + ";base64," + base64Data;
    }
}
