package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.service.SettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class OpenAiBidAgentConfigurationResolver {

    private static final String DEFAULT_SETTINGS_API_KEY = "sk_xiyu_bid_server_default";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-5.2";

    private final SettingsService settingsService;
    private final String configuredApiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;

    public OpenAiBidAgentConfigurationResolver(
            SettingsService settingsService,
            @Value("${ai.openai.api-key:}") String configuredApiKey,
            @Value("${ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.openai.model:gpt-5.2}") String model,
            @Value("${ai.openai.timeout:PT30S}") Duration timeout
    ) {
        this.settingsService = settingsService;
        this.configuredApiKey = configuredApiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeout = timeout;
    }

    OpenAiBidAgentRequestConfig resolve(String useCase) {
        return new OpenAiBidAgentRequestConfig(
                effectiveApiKey(useCase),
                effectiveBaseUrl(),
                effectiveModel(),
                timeout
        );
    }

    private String effectiveApiKey(String useCase) {
        return usableKey(configuredApiKey)
                .or(this::settingsApiKey)
                .orElseThrow(() -> new IllegalStateException(
                        "ai.openai.api-key must be configured for " + useCase
                ));
    }

    private Optional<String> settingsApiKey() {
        return settingsIntegrationConfig()
                .flatMap(config -> usableKey(config.getApiKey()));
    }

    private String effectiveBaseUrl() {
        return nonDefaultValue(baseUrl, DEFAULT_BASE_URL)
                .or(this::settingsBaseUrl)
                .orElse(DEFAULT_BASE_URL);
    }

    private Optional<String> settingsBaseUrl() {
        return settingsIntegrationConfig()
                .flatMap(config -> usableValue(config.getAiBaseUrl()));
    }

    private String effectiveModel() {
        return nonDefaultValue(model, DEFAULT_MODEL)
                .or(this::settingsModel)
                .orElse(DEFAULT_MODEL);
    }

    private Optional<String> settingsModel() {
        return settingsIntegrationConfig()
                .flatMap(config -> usableValue(config.getAiModel()));
    }

    private Optional<SettingsResponse.IntegrationConfig> settingsIntegrationConfig() {
        SettingsResponse settings = settingsService.getSettings();
        if (settings == null || settings.getIntegrationConfig() == null) {
            return Optional.empty();
        }
        return Optional.of(settings.getIntegrationConfig());
    }

    private Optional<String> usableKey(String value) {
        return usableValue(value)
                .filter(key -> !DEFAULT_SETTINGS_API_KEY.equals(key));
    }

    private Optional<String> usableValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private Optional<String> nonDefaultValue(String value, String defaultValue) {
        return usableValue(value)
                .filter(candidate -> !defaultValue.equals(candidate));
    }
}
