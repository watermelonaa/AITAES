package com.example.aitaes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "aitaes.ai.ollama")
public class OllamaProperties {
    private String baseUrl = "http://localhost:11434";
    private String model = "qwen2.5:7b";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(120);
    private int maxAttempts = 3;
    private Duration retryDelay = Duration.ofSeconds(1);
    private double temperature = 0.2;
}
