package com.careermind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {

    private String defaultProvider;

    private KimiConfig kimi;
    private OpenAIConfig openai;
    private ClaudeConfig claude;
    private WenxinConfig wenxin;
    private TongyiConfig tongyi;

    @Data
    public static class KimiConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
        private double temperature;
        private int maxTokens;
    }

    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
        private double temperature;
        private int maxTokens;
    }

    @Data
    public static class ClaudeConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
        private double temperature;
        private int maxTokens;
    }

    @Data
    public static class WenxinConfig {
        private String apiKey;
        private String secretKey;
        private String model;
    }

    @Data
    public static class TongyiConfig {
        private String apiKey;
        private String model;
    }
}
