package com.xiyu.bid.tendersource.domain;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.net.ConnectException;
import java.net.UnknownHostException;

/**
 * 标讯源连接测试策略（Pure Core）
 * 
 * 纯核心业务逻辑：验证标讯源API端点的连通性
 * - 不修改入参
 * - 不读写数据库
 * - 不记录日志
 * - 返回可预测的结果
 */
public final class TenderSourceConnectionTestPolicy {

    private static final String THIRD_PARTY_PLATFORM = "第三方商机服务";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private TenderSourceConnectionTestPolicy() {
    }

    /**
     * 测试第三方商机服务的API连接
     * 
     * @param platform 平台名称
     * @param apiEndpoint API端点地址
     * @param apiKey API密钥
     * @return 连接测试结果
     */
    public static TenderSourceConnectionResult testThirdPartyConnection(
            String platform,
            String apiEndpoint,
            String apiKey) {
        
        if (!THIRD_PARTY_PLATFORM.equals(platform)) {
            return TenderSourceConnectionResult.failure("仅支持测试「第三方商机服务」平台的连接");
        }

        if (apiEndpoint == null || apiEndpoint.isBlank()) {
            return TenderSourceConnectionResult.failure("API端点不能为空");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return TenderSourceConnectionResult.failure("API密钥不能为空");
        }

        URI uri;
        try {
            uri = URI.create(apiEndpoint.trim());
        } catch (IllegalArgumentException e) {
            return TenderSourceConnectionResult.failure("API端点格式无效");
        }

        return performHttpTest(uri, apiKey.trim());
    }

    private static TenderSourceConnectionResult performHttpTest(URI uri, String apiKey) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return TenderSourceConnectionResult.success();
            } else if (statusCode == 401 || statusCode == 403) {
                return TenderSourceConnectionResult.failure("认证失败，请检查API密钥");
            } else if (statusCode == 404) {
                return TenderSourceConnectionResult.failure("API端点未找到，请检查URL");
            } else {
                return TenderSourceConnectionResult.failure("服务器返回错误状态码: " + statusCode);
            }
        } catch (HttpTimeoutException e) {
            return TenderSourceConnectionResult.failure("连接超时，请检查API端点是否可访问");
        } catch (ConnectException e) {
            return TenderSourceConnectionResult.failure("无法连接到服务器，请检查API端点");
        } catch (UnknownHostException e) {
            return TenderSourceConnectionResult.failure("无法解析域名，请检查API端点地址");
        } catch (IllegalArgumentException e) {
            return TenderSourceConnectionResult.failure("HTTP请求构建失败: " + e.getMessage());
        } catch (RuntimeException e) {
            return TenderSourceConnectionResult.failure("连接失败: " + extractRootMessage(e));
        }
    }

    private static String extractRootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return message != null && !message.isBlank() ? message : "未知错误";
    }
}
