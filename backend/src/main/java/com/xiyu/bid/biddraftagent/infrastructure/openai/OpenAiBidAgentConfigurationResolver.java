package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.service.AiProviderCatalog;
import com.xiyu.bid.settings.service.SettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class OpenAiBidAgentConfigurationResolver {

    private static final String DEFAULT_SETTINGS_API_KEY = "sk_xiyu_bid_server_default";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-5.2";
    private static final Duration CHAT_COMPLETION_MIN_TIMEOUT = Duration.ofSeconds(90);

    private final SettingsService settingsService;
    private final AiProviderCatalog aiProviderCatalog;
    private final Environment environment;
    private final String configuredApiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;

    public OpenAiBidAgentConfigurationResolver(
            SettingsService pSettingsService,
            AiProviderCatalog pAiProviderCatalog,
            Environment pEnvironment,
            @Value("${ai.openai.api-key:}") String pConfiguredApiKey,
            @Value("${ai.openai.base-url:https://api.openai.com/v1}") String pBaseUrl,
            @Value("${ai.openai.model:gpt-5.2}") String pModel,
            @Value("${ai.openai.timeout:PT30S}") Duration pTimeout
    ) {
        this.settingsService = pSettingsService;
        this.aiProviderCatalog = pAiProviderCatalog;
        this.environment = pEnvironment;
        this.configuredApiKey = pConfiguredApiKey;
        this.baseUrl = pBaseUrl;
        this.model = pModel;
        this.timeout = pTimeout;
    }

    OpenAiBidAgentRequestConfig resolve(String useCase) {
        return springConfiguredRequest()
                .or(this::activeProviderRequest)
                .or(this::integrationConfiguredRequest)
                .orElseThrow(() -> new IllegalStateException(
                        "ai.openai.api-key must be configured for " + useCase
                ));
    }

    private Optional<OpenAiBidAgentRequestConfig> springConfiguredRequest() {
        OpenAiBidAgentApiStyle apiStyle = resolveApiStyle("openai", configuredBaseUrl().orElse(DEFAULT_BASE_URL));
        return usableKey(configuredApiKey)
                .map(apiKey -> new OpenAiBidAgentRequestConfig(
                        apiKey,
                        normalizedBaseUrl(configuredBaseUrl().orElse(DEFAULT_BASE_URL)),
                        configuredModel().orElse(DEFAULT_MODEL),
                        effectiveTimeout(apiStyle),
                        apiStyle
                ));
    }

    private Optional<OpenAiBidAgentRequestConfig> activeProviderRequest() {
        SettingsResponse.AiModelConfig aiModelConfig = settingsService.getInternalAiModelConfig();
        if (aiModelConfig == null) {
            return Optional.empty();
        }

        String providerCode = aiProviderCatalog.normalize(aiModelConfig.getActiveProvider());
        if (!aiProviderCatalog.isSupported(providerCode)) {
            return Optional.empty();
        }

        return findProvider(aiModelConfig, providerCode)
                .flatMap(provider -> {
                    OpenAiBidAgentApiStyle apiStyle = resolveApiStyle(providerCode, provider.getBaseUrl());
                    return providerApiKey(providerCode)
                            .map(apiKey -> new OpenAiBidAgentRequestConfig(
                                    apiKey,
                                    normalizedBaseUrl(firstNonBlank(provider.getBaseUrl(), DEFAULT_BASE_URL)),
                                    firstNonBlank(provider.getModel(), DEFAULT_MODEL),
                                    effectiveTimeout(apiStyle),
                                    apiStyle
                            ));
                });
    }

    private Optional<OpenAiBidAgentRequestConfig> integrationConfiguredRequest() {
        return settingsIntegrationConfig()
                .flatMap(config -> usableKey(config.getApiKey())
                        .map(apiKey -> {
                            OpenAiBidAgentApiStyle apiStyle = resolveApiStyle(null, config.getAiBaseUrl());
                            return new OpenAiBidAgentRequestConfig(
                                    apiKey,
                                    normalizedBaseUrl(firstNonBlank(config.getAiBaseUrl(), DEFAULT_BASE_URL)),
                                    firstNonBlank(config.getAiModel(), DEFAULT_MODEL),
                                    effectiveTimeout(apiStyle),
                                    apiStyle
                            );
                        }));
    }

    private Optional<SettingsResponse.AiProviderSetting> findProvider(
            SettingsResponse.AiModelConfig aiModelConfig,
            String providerCode
    ) {
        List<SettingsResponse.AiProviderSetting> providers = aiModelConfig.getProviders();
        if (providers == null || providers.isEmpty()) {
            return Optional.empty();
        }
        return providers.stream()
                .filter(provider -> providerCode.equals(aiProviderCatalog.normalize(provider.getProviderCode())))
                .filter(provider -> !Boolean.FALSE.equals(provider.getEnabled()))
                .findFirst();
    }

    private Optional<String> providerApiKey(String providerCode) {
        return usableValue(settingsService.resolveAiApiKey(providerCode))
                .or(() -> providerEnvironmentApiKey(providerCode));
    }

    private Optional<String> providerEnvironmentApiKey(String providerCode) {
        for (String keyName : aiProviderCatalog.environmentKeys(providerCode)) {
            Optional<String> propertyValue = usableValue(environment.getProperty(keyName));
            if (propertyValue.isPresent()) {
                return propertyValue;
            }
            Optional<String> envValue = usableValue(System.getenv(keyName));
            if (envValue.isPresent()) {
                return envValue;
            }
        }
        return Optional.empty();
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

    private Optional<String> configuredBaseUrl() {
        return nonDefaultValue(baseUrl, DEFAULT_BASE_URL);
    }

    private Optional<String> configuredModel() {
        return nonDefaultValue(model, DEFAULT_MODEL);
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

    private String firstNonBlank(String value, String defaultValue) {
        return usableValue(value).orElse(defaultValue);
    }

    private String normalizedBaseUrl(String candidate) {
        String trimmed = candidate == null ? DEFAULT_BASE_URL : candidate.trim();
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed.substring(0, trimmed.length() - "/chat/completions".length());
        }
        if (trimmed.endsWith("/responses")) {
            return trimmed.substring(0, trimmed.length() - "/responses".length());
        }
        return trimmed;
    }

    private OpenAiBidAgentApiStyle resolveApiStyle(String providerCode, String rawBaseUrl) {
        if ("openai".equals(aiProviderCatalog.normalize(providerCode))) {
            return OpenAiBidAgentApiStyle.RESPONSES;
        }
        String candidate = rawBaseUrl == null ? "" : rawBaseUrl.trim();
        if (candidate.endsWith("/chat/completions")) {
            return OpenAiBidAgentApiStyle.CHAT_COMPLETIONS;
        }
        if (candidate.endsWith("/responses")) {
            return OpenAiBidAgentApiStyle.RESPONSES;
        }
        return OpenAiBidAgentApiStyle.RESPONSES;
    }

    private Duration effectiveTimeout(OpenAiBidAgentApiStyle apiStyle) {
        if (apiStyle == OpenAiBidAgentApiStyle.CHAT_COMPLETIONS
                && timeout.compareTo(CHAT_COMPLETION_MIN_TIMEOUT) < 0) {
            return CHAT_COMPLETION_MIN_TIMEOUT;
        }
        return timeout;
    }
}
