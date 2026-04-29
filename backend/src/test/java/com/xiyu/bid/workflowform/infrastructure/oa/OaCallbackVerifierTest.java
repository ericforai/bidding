package com.xiyu.bid.workflowform.infrastructure.oa;

import com.xiyu.bid.workflowform.domain.OaApprovalStatus;
import com.xiyu.bid.workflowform.dto.OaCallbackRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OaCallbackVerifierTest {

    private static final String SECRET = "oa-callback-secret";

    @Test
    void accepts_valid_signature_inside_time_window() {
        OaCallbackVerifier verifier = new OaCallbackVerifier(SECRET);
        OaCallbackRequest request = request(String.valueOf(Instant.now().getEpochSecond()), "evt-1", null);

        assertThatCode(() -> verifier.verify(request)).doesNotThrowAnyException();
    }

    @Test
    void rejects_bad_signature() {
        OaCallbackVerifier verifier = new OaCallbackVerifier(SECRET);
        OaCallbackRequest request = request(String.valueOf(Instant.now().getEpochSecond()), "evt-2", "bad-signature");

        assertThatThrownBy(() -> verifier.verify(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejects_expired_timestamp() {
        OaCallbackVerifier verifier = new OaCallbackVerifier(SECRET);
        OaCallbackRequest request = request(String.valueOf(Instant.now().minusSeconds(301).getEpochSecond()), "evt-3", null);

        assertThatThrownBy(() -> verifier.verify(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejects_non_numeric_timestamp() {
        OaCallbackVerifier verifier = new OaCallbackVerifier(SECRET);
        OaCallbackRequest request = request("not-a-timestamp", "evt-4", "bad-signature");

        assertThatThrownBy(() -> verifier.verify(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejects_blank_secret_configuration() {
        OaCallbackVerifier verifier = new OaCallbackVerifier("");
        OaCallbackRequest request = request(String.valueOf(Instant.now().getEpochSecond()), "evt-5", null);

        assertThatThrownBy(() -> verifier.verify(request)).isInstanceOf(ResponseStatusException.class);
    }

    private static OaCallbackRequest request(String timestamp, String eventId, String signatureOverride) {
        String signature = signatureOverride == null
                ? sign(String.join("|", "OA-1", OaApprovalStatus.APPROVED.name(), timestamp, "nonce-1", eventId))
                : signatureOverride;
        return new OaCallbackRequest("OA-1", OaApprovalStatus.APPROVED, "经理", "同意", timestamp, "nonce-1", signature, eventId);
    }

    private static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
