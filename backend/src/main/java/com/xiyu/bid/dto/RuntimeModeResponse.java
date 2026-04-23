package com.xiyu.bid.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RuntimeModeResponse {
    String modeCode;
    String modeLabel;
    String database;
    boolean demoFusionEnabled;
    List<String> activeProfiles;
}
