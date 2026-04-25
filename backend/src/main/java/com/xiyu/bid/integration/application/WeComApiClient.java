package com.xiyu.bid.integration.application;

import com.xiyu.bid.integration.domain.WeComApiErrCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Low-level HTTP client for the WeCom (企业微信) API.
 * Single responsibility: make HTTP calls and parse raw responses.
 * No caching, no orchestration, no retry logic here.
 */
@Component
@Slf4j
public class WeComApiClient {

    /**
     * Raw response from WeCom gettoken endpoint.
     */
    public record WeComAccessTokenResponse(
            String token,
            long expiresIn,
            int errcode,
            String errmsg
    ) {
    }

    /**
     * Raw response from WeCom message send endpoint.
     */
    public record WeComSendResponse(
            int errcode,
            String errmsg,
            String invaliduser
    ) {
    }

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public WeComApiClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${wecom.api.base-url:https://qyapi.weixin.qq.com}") String baseUrl,
            @Value("${wecom.http.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${wecom.http.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /**
     * Fetches an access token from the WeCom API.
     * Returns the raw response including errcode — caller decides what to do with non-OK codes.
     *
     * @throws WeComApiException on HTTP 5xx or network/timeout errors
     */
    public WeComAccessTokenResponse requestAccessToken(String corpId, String corpSecret) {
        String url = baseUrl + "/cgi-bin/gettoken?corpid={corpId}&corpsecret={corpSecret}";
        try {
            var response = restTemplate.getForObject(url, Map.class, corpId, corpSecret);
            return parseTokenResponse(response);
        } catch (HttpStatusCodeException ex) {
            log.warn("WeCom gettoken HTTP error: {}", ex.getStatusCode());
            throw new WeComApiException(WeComApiErrCode.UNKNOWN.code(),
                    "WeCom gettoken HTTP error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.warn("WeCom gettoken request failed: {}", ex.getMessage());
            throw new WeComApiException(WeComApiErrCode.UNKNOWN.code(),
                    "WeCom gettoken request failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Sends an application message via the WeCom API.
     *
     * @throws WeComApiException on HTTP 5xx or network/timeout errors
     */
    public WeComSendResponse sendAppMessage(String accessToken, Map<String, Object> payload) {
        String url = baseUrl + "/cgi-bin/message/send?access_token={token}";
        try {
            var response = restTemplate.postForObject(url, payload, Map.class, accessToken);
            return parseSendResponse(response);
        } catch (HttpStatusCodeException ex) {
            log.warn("WeCom sendmessage HTTP error: {}", ex.getStatusCode());
            throw new WeComApiException(WeComApiErrCode.UNKNOWN.code(),
                    "WeCom sendmessage HTTP error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.warn("WeCom sendmessage request failed: {}", ex.getMessage());
            throw new WeComApiException(WeComApiErrCode.UNKNOWN.code(),
                    "WeCom sendmessage request failed: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private WeComAccessTokenResponse parseTokenResponse(Map<?, ?> body) {
        if (body == null) {
            throw new WeComApiException(WeComApiErrCode.UNKNOWN.code(), "WeCom gettoken returned null body");
        }
        int errcode = body.containsKey("errcode") ? toInt(body.get("errcode")) : 0;
        String errmsg = body.containsKey("errmsg") ? String.valueOf(body.get("errmsg")) : "";
        String token = body.containsKey("access_token") ? String.valueOf(body.get("access_token")) : null;
        long expiresIn = body.containsKey("expires_in") ? toLong(body.get("expires_in")) : 0L;
        return new WeComAccessTokenResponse(token, expiresIn, errcode, errmsg);
    }

    @SuppressWarnings("unchecked")
    private WeComSendResponse parseSendResponse(Map<?, ?> body) {
        if (body == null) {
            throw new WeComApiException(WeComApiErrCode.UNKNOWN.code(), "WeCom sendmessage returned null body");
        }
        int errcode = body.containsKey("errcode") ? toInt(body.get("errcode")) : 0;
        String errmsg = body.containsKey("errmsg") ? String.valueOf(body.get("errmsg")) : "";
        String invaliduser = body.containsKey("invaliduser") ? String.valueOf(body.get("invaliduser")) : null;
        return new WeComSendResponse(errcode, errmsg, invaliduser);
    }

    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }
}
