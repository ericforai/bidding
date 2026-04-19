package com.xiyu.bid.templatecatalog.domain.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TemplateVersionPolicy {

    public String initialVersion() {
        return "1.0";
    }

    public String nextVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return initialVersion();
        }
        try {
            return BigDecimal.valueOf(Double.parseDouble(currentVersion))
                    .add(BigDecimal.valueOf(0.1))
                    .setScale(1, RoundingMode.HALF_UP)
                    .toPlainString();
        } catch (NumberFormatException ignored) {
            return currentVersion + ".1";
        }
    }
}
