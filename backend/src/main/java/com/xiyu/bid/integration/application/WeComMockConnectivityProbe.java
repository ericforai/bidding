package com.xiyu.bid.integration.application;

import com.xiyu.bid.integration.domain.WeComConnectivityResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Default (mock) implementation of WeComConnectivityProbe.
 * Returns a successful result without making any external network call.
 * Replace with a real implementation when WeCom SDK is integrated.
 */
@Component
public class WeComMockConnectivityProbe implements WeComConnectivityProbe {

    @Override
    public WeComConnectivityResult probe(String corpId, String agentId, String plainSecret) {
        return new WeComConnectivityResult(true, "企业微信连接探测成功（模拟）", LocalDateTime.now());
    }
}
