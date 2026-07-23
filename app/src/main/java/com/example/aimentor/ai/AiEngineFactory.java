package com.example.aimentor.ai;

import com.example.aimentor.BuildConfig;

/** Selects the configured AI engine without exposing credentials to callers. */
public final class AiEngineFactory {

    private AiEngineFactory() { }

    public static AiEngine create() {
        String key = BuildConfig.HCNSEC_API_KEY == null
                ? "" : BuildConfig.HCNSEC_API_KEY.trim();
        if (key.isEmpty()) {
            return new LocalAiEngine();
        }
        return new RemoteAiEngine(BuildConfig.HCNSEC_BASE_URL, key);
    }
}
