package com.example.aimentor.ai;

import com.example.aimentor.BuildConfig;

/** Selects the configured AI engine without exposing credentials to callers. */
public final class AiEngineFactory {

    private AiEngineFactory() { }

    public static AiEngine create() {
        String key = BuildConfig.GROQ_API_KEY == null
                ? "" : BuildConfig.GROQ_API_KEY.trim();
        if (key.isEmpty()) {
            return new UnavailableAiEngine(new LocalAiEngine());
        }
        String geminiKey = BuildConfig.GEMINI_API_KEY == null
                ? "" : BuildConfig.GEMINI_API_KEY.trim();
        return new FallbackAiEngine(
                new RemoteAiEngine(BuildConfig.GROQ_BASE_URL, key,
                        BuildConfig.GEMINI_BASE_URL, geminiKey),
                new LocalAiEngine());
    }
}
