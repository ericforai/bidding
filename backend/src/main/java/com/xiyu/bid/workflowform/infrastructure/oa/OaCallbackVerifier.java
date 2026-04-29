package com.xiyu.bid.workflowform.infrastructure.oa;

import com.xiyu.bid.workflowform.dto.OaCallbackRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Component
public class OaCallbackVerifier {

    private final String callbackSecret;

    public OaCallbackVerifier(@Value("${oa.workflow.callback.secret:}") String callbackSecret) {
        this.callbackSecret = callbackSecret == null ? "" : callbackSecret;
    }

    public void verify(OaCallbackRequest request) {
        if (callbackSecret.isBlank()) {
            throw unauthorized();
        }
        long timestamp = parseTimestamp(request.timestamp());
        if (Math.abs(Instant.now().getEpochSecond() - timestamp) > 300) {
            throw unauthorized();
        }
        String payload = String.join("|", request.oaInstanceId(), request.status().name(), request.timestamp(), request.nonce(), request.eventId());
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), request.signature().getBytes(StandardCharsets.UTF_8))) {
            throw unauthorized();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (InvalidKeyException | NoSuchAlgorithmException exception) {
            throw unauthorized();
        }
    }

    private long parseTimestamp(String timestamp) {
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw unauthorized();
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OA 回调验证失败");
    }
}
