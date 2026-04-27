package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.service.AiProviderCatalog;
import com.xiyu.bid.settings.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OpenAiBidAgentConfigurationResolverTest {

    @Test
    void resolve_shouldPreferSpringConfiguredApiKey() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, environment, " sk-env ");

        OpenAiBidAgentRequestConfig config = resolver.resolve("bid draft generation");

        assertThat(config.apiKey()).isEqualTo("sk-env");
        assertThat(config.baseUrl()).isEqualTo("https://api.example.test/v1");
        assertThat(config.model()).isEqualTo("gpt-test");
        assertThat(config.timeout()).isEqualTo(Duration.ofSeconds(90));
        assertThat(config.apiStyle()).isEqualTo(OpenAiBidAgentApiStyle.RESPONSES);
        verifyNoInteractions(settingsService, environment);
    }

    @Test
    void resolve_shouldUseActiveProviderConfigAndEnvironmentKeyWhenSpringKeyMissing() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getInternalAiModelConfig()).thenReturn(aiModelConfig(
                "deepseek",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat"
        ));
        when(settingsService.resolveAiApiKey("deepseek")).thenReturn(null);
        when(environment.getProperty("DEEPSEEK_API_KEY")).thenReturn(" sk-deepseek ");
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, environment, "");

        OpenAiBidAgentRequestConfig config = resolver.resolve("tender document analysis");

        assertThat(config.apiKey()).isEqualTo("sk-deepseek");
        assertThat(config.baseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(config.model()).isEqualTo("deepseek-chat");
        assertThat(config.apiStyle()).isEqualTo(OpenAiBidAgentApiStyle.CHAT_COMPLETIONS);
        verify(settingsService, never()).getSettings();
    }

    @Test
    void resolve_shouldRaiseTimeoutFloorForChatCompletionProviders() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getInternalAiModelConfig()).thenReturn(aiModelConfig(
                "deepseek",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat"
        ));
        when(settingsService.resolveAiApiKey("deepseek")).thenReturn(" sk-deepseek ");
        OpenAiBidAgentConfigurationResolver resolver = new OpenAiBidAgentConfigurationResolver(
                settingsService,
                new AiProviderCatalog(),
                environment,
                "",
                "https://api.openai.com/v1",
                "gpt-5.2",
                Duration.ofSeconds(30)
        );

        OpenAiBidAgentRequestConfig config = resolver.resolve("tender document analysis");

        assertThat(config.timeout()).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    void resolve_shouldKeepResponsesApiForActiveOpenAiProvider() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getInternalAiModelConfig()).thenReturn(aiModelConfig(
                "openai",
                "https://api.openai.com/v1/chat/completions",
                "gpt-4o-mini"
        ));
        when(settingsService.resolveAiApiKey("openai")).thenReturn(" sk-openai ");
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, environment, "");

        OpenAiBidAgentRequestConfig config = resolver.resolve("tender document analysis");

        assertThat(config.apiStyle()).isEqualTo(OpenAiBidAgentApiStyle.RESPONSES);
        assertThat(config.baseUrl()).isEqualTo("https://api.openai.com/v1");
    }

    @Test
    void resolve_shouldUseSystemSettingsApiKeyWhenSpringKeyMissing() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getSettings()).thenReturn(settingsWithApiKey(" sk-system "));
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, environment, "");

        OpenAiBidAgentRequestConfig config = resolver.resolve("tender document analysis");

        assertThat(config.apiKey()).isEqualTo("sk-system");
        assertThat(config.apiStyle()).isEqualTo(OpenAiBidAgentApiStyle.RESPONSES);
    }

    @Test
    void resolve_shouldUseSystemSettingsGatewayWhenSpringValuesAreDefault() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getSettings()).thenReturn(settingsWithAiConfig(
                "sk-system",
                " https://gateway.example.test/v1 ",
                " gpt-system "
        ));
        OpenAiBidAgentConfigurationResolver resolver = new OpenAiBidAgentConfigurationResolver(
                settingsService,
                new AiProviderCatalog(),
                environment,
                "",
                "https://api.openai.com/v1",
                "gpt-5.2",
                Duration.ofSeconds(90)
        );

        OpenAiBidAgentRequestConfig config = resolver.resolve("tender document analysis");

        assertThat(config.baseUrl()).isEqualTo("https://gateway.example.test/v1");
        assertThat(config.model()).isEqualTo("gpt-system");
        assertThat(config.apiStyle()).isEqualTo(OpenAiBidAgentApiStyle.RESPONSES);
    }

    @Test
    void resolve_shouldRejectMissingAndPlaceholderApiKeys() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getSettings()).thenReturn(settingsWithApiKey("sk_xiyu_bid_server_default"));
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, environment, "");

        assertThatThrownBy(() -> resolver.resolve("bid draft generation"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ai.openai.api-key must be configured for bid draft generation");
    }

    @Test
    void resolveTenderIntake_shouldUseDeepSeekProviderSettingsAndSettingsApiKey() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getInternalAiModelConfig()).thenReturn(aiModelConfig(
                "openai",
                "deepseek",
                "https://api.deepseek.com/chat/completions",
                "deepseek-reasoner"
        ));
        when(settingsService.resolveAiApiKey("deepseek")).thenReturn(" sk-deepseek-settings ");
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, environment, " sk-openai ");

        OpenAiBidAgentRequestConfig config = resolver.resolveTenderIntake();

        assertThat(config.apiKey()).isEqualTo("sk-deepseek-settings");
        assertThat(config.baseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(config.model()).isEqualTo("deepseek-reasoner");
        assertThat(config.timeout()).isEqualTo(Duration.ofSeconds(90));
        assertThat(config.apiStyle()).isEqualTo(OpenAiBidAgentApiStyle.CHAT_COMPLETIONS);
        verify(settingsService, never()).resolveAiApiKey("openai");
        verify(settingsService, never()).getSettings();
    }

    @Test
    void resolveTenderIntake_shouldUseDeepSeekEnvAndDefaultsWhenSettingsProviderMissing() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        when(settingsService.getInternalAiModelConfig()).thenReturn(null);
        when(settingsService.resolveAiApiKey("deepseek")).thenReturn(null);
        when(environment.getProperty("DEEPSEEK_API_KEY")).thenReturn(" sk-deepseek-env ");
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, environment, "");

        OpenAiBidAgentRequestConfig config = resolver.resolveTenderIntake();

        assertThat(config.apiKey()).isEqualTo("sk-deepseek-env");
        assertThat(config.baseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(config.model()).isEqualTo("deepseek-chat");
        assertThat(config.apiStyle()).isEqualTo(OpenAiBidAgentApiStyle.CHAT_COMPLETIONS);
        verify(environment, never()).getProperty("OPENAI_API_KEY");
    }

    @Test
    void resolveTenderIntake_missingKey_shouldMentionDeepSeekEnvironmentKey() {
        SettingsService settingsService = mock(SettingsService.class);
        Environment environment = mock(Environment.class);
        AiProviderCatalog aiProviderCatalog = mock(AiProviderCatalog.class);
        when(aiProviderCatalog.normalize("deepseek")).thenReturn("deepseek");
        when(aiProviderCatalog.environmentKeys("deepseek")).thenReturn(List.of("DEEPSEEK_API_KEY_MISSING_TEST"));
        when(settingsService.getInternalAiModelConfig()).thenReturn(aiModelConfig(
                "deepseek",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat"
        ));
        when(settingsService.resolveAiApiKey("deepseek")).thenReturn(null);
        OpenAiBidAgentConfigurationResolver resolver = new OpenAiBidAgentConfigurationResolver(
                settingsService,
                aiProviderCatalog,
                environment,
                "",
                "https://api.example.test/v1",
                "gpt-test",
                Duration.ofSeconds(90)
        );

        assertThatThrownBy(resolver::resolveTenderIntake)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DeepSeek")
                .hasMessageContaining("DEEPSEEK_API_KEY");

        verify(settingsService, never()).getSettings();
    }

    private OpenAiBidAgentConfigurationResolver resolver(
            SettingsService settingsService,
            Environment environment,
            String configuredApiKey
    ) {
        return new OpenAiBidAgentConfigurationResolver(
                settingsService,
                new AiProviderCatalog(),
                environment,
                configuredApiKey,
                "https://api.example.test/v1",
                "gpt-test",
                Duration.ofSeconds(90)
        );
    }

    private SettingsResponse settingsWithApiKey(String apiKey) {
        return settingsWithAiConfig(apiKey, null, null);
    }

    private SettingsResponse settingsWithAiConfig(String apiKey, String aiBaseUrl, String aiModel) {
        return SettingsResponse.builder()
                .integrationConfig(SettingsResponse.IntegrationConfig.builder()
                        .apiKey(apiKey)
                        .aiBaseUrl(aiBaseUrl)
                        .aiModel(aiModel)
                        .build())
                .build();
    }

    private SettingsResponse.AiModelConfig aiModelConfig(String activeProvider, String baseUrl, String model) {
        return aiModelConfig(activeProvider, activeProvider, baseUrl, model);
    }

    private SettingsResponse.AiModelConfig aiModelConfig(
            String activeProvider,
            String providerCode,
            String baseUrl,
            String model
    ) {
        return SettingsResponse.AiModelConfig.builder()
                .activeProvider(activeProvider)
                .providers(List.of(SettingsResponse.AiProviderSetting.builder()
                        .providerCode(providerCode)
                        .providerName(providerCode)
                        .enabled(true)
                        .baseUrl(baseUrl)
                        .model(model)
                        .lastTestAt(Instant.now())
                        .build()))
                .build();
    }
}
