package com.xiyu.bid.integration.organization.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class OrganizationWebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OrganizationIntegrationSettingsResolver settingsResolver;

    public boolean valid(String traceId, String sourceApp, String payload, String signature) {
        OrganizationIntegrationSettings settings = settingsResolver.resolve();
        if (blank(settings.webhookSecret()) || blank(traceId) || blank(sourceApp) || blank(signature)) {
            return false;
        }
        byte[] expected = sign(settings.webhookSecret(), canonical(traceId, sourceApp, payload));
        byte[] provided = decodeHex(signature);
        return provided.length > 0 && MessageDigest.isEqual(expected, provided);
    }

    public boolean ipAllowed(String remoteAddress) {
        return settingsResolver.resolve().ipAllowed(remoteAddress);
    }

    private byte[] sign(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        } catch (InvalidKeyException | NoSuchAlgorithmException ex) {
            return new byte[0];
        }
    }

    private byte[] decodeHex(String signature) {
        try {
            return HexFormat.of().parseHex(signature.trim());
        } catch (IllegalArgumentException ex) {
            return new byte[0];
        }
    }

    private String canonical(String traceId, String sourceApp, String payload) {
        return traceId.trim() + "." + sourceApp.trim() + "." + (payload == null ? "" : payload);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
