package com.xiyu.bid.integration.organization.infrastructure.mapper;

import com.xiyu.bid.integration.organization.application.OrganizationIntegrationProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PositionToRoleMapper {

    private final OrganizationIntegrationProperties properties;
    private List<Map.Entry<Pattern, String>> compiledMappings;

    @PostConstruct
    public void init() {
        compiledMappings = properties.getPositionToRoleMappings().stream()
                .filter(m -> m.getPositionPattern() != null && !m.getPositionPattern().isBlank()
                        && m.getRoleCode() != null && !m.getRoleCode().isBlank())
                .map(m -> Map.entry(
                        Pattern.compile(m.getPositionPattern()),
                        m.getRoleCode().trim().toLowerCase(Locale.ROOT)))
                .toList();
    }

    public String map(String positionText) {
        if (positionText == null || positionText.isBlank()) {
            return null;
        }
        return compiledMappings.stream()
                .filter(entry -> entry.getKey().matcher(positionText).find())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
