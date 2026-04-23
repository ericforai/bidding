package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.xiyu.bid.settings.dto.SettingsResponse;
import com.xiyu.bid.settings.service.SettingsService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OpenAiBidAgentConfigurationResolverTest {

    @Test
    void resolve_shouldPreferSpringConfiguredApiKey() {
        SettingsService settingsService = mock(SettingsService.class);
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, " sk-env ");

        OpenAiBidAgentRequestConfig config = resolver.resolve("bid draft generation");

        assertThat(config.apiKey()).isEqualTo("sk-env");
        assertThat(config.baseUrl()).isEqualTo("https://api.example.test/v1");
        assertThat(config.model()).isEqualTo("gpt-test");
        assertThat(config.timeout()).isEqualTo(Duration.ofSeconds(90));
        verifyNoInteractions(settingsService);
    }

    @Test
    void resolve_shouldUseSystemSettingsApiKeyWhenSpringKeyMissing() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettings()).thenReturn(settingsWithApiKey(" sk-system "));
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, "");

        OpenAiBidAgentRequestConfig config = resolver.resolve("tender document analysis");

        assertThat(config.apiKey()).isEqualTo("sk-system");
    }

    @Test
    void resolve_shouldUseSystemSettingsGatewayWhenSpringValuesAreDefault() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettings()).thenReturn(settingsWithAiConfig(
                "sk-system",
                " https://gateway.example.test/v1 ",
                " gpt-system "
        ));
        OpenAiBidAgentConfigurationResolver resolver = new OpenAiBidAgentConfigurationResolver(
                settingsService,
                "",
                "https://api.openai.com/v1",
                "gpt-5.2",
                Duration.ofSeconds(90)
        );

        OpenAiBidAgentRequestConfig config = resolver.resolve("tender document analysis");

        assertThat(config.baseUrl()).isEqualTo("https://gateway.example.test/v1");
        assertThat(config.model()).isEqualTo("gpt-system");
    }

    @Test
    void resolve_shouldRejectMissingAndPlaceholderApiKeys() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettings()).thenReturn(settingsWithApiKey("sk_xiyu_bid_server_default"));
        OpenAiBidAgentConfigurationResolver resolver = resolver(settingsService, "");

        assertThatThrownBy(() -> resolver.resolve("bid draft generation"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ai.openai.api-key must be configured for bid draft generation");
    }

    private OpenAiBidAgentConfigurationResolver resolver(SettingsService settingsService, String configuredApiKey) {
        return new OpenAiBidAgentConfigurationResolver(
                settingsService,
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
}
