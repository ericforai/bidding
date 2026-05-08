package com.xiyu.bid.integration.organization.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizationWebhookSignatureVerifier — HMAC validation")
class OrganizationWebhookSignatureVerifierTest {

    @Test
    @DisplayName("accepts valid hex HMAC signature")
    void valid_acceptsMatchingSignature() throws Exception {
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        properties.setWebhookSecret("secret");
        OrganizationWebhookSignatureVerifier verifier = new OrganizationWebhookSignatureVerifier(
                new OrganizationIntegrationSettingsResolver(null, properties)
        );
        String payload = "{\"userCode\":\"u001\"}";

        boolean result = verifier.valid("trace-1", "customer-org", payload, sign("secret", "trace-1.customer-org." + payload));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("rejects blank secret and malformed hex signatures")
    void valid_rejectsBlankSecretAndMalformedSignature() {
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        OrganizationWebhookSignatureVerifier verifier = new OrganizationWebhookSignatureVerifier(
                new OrganizationIntegrationSettingsResolver(null, properties)
        );

        assertThat(verifier.valid("trace-1", "customer-org", "{}", "not-hex")).isFalse();

        properties.setWebhookSecret("secret");
        assertThat(verifier.valid("trace-1", "customer-org", "{}", "not-hex")).isFalse();
    }

    @Test
    @DisplayName("uses ip whitelist from resolved settings")
    void ipAllowed_checksWhitelist() {
        OrganizationIntegrationProperties properties = new OrganizationIntegrationProperties();
        properties.setIpWhitelist("10.0.0.1, 10.0.0.2");
        OrganizationWebhookSignatureVerifier verifier = new OrganizationWebhookSignatureVerifier(
                new OrganizationIntegrationSettingsResolver(null, properties)
        );

        assertThat(verifier.ipAllowed("10.0.0.1")).isTrue();
        assertThat(verifier.ipAllowed("10.0.0.9")).isFalse();
    }

    private String sign(String secret, String canonical) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }
}
