package com.xiyu.bid.batch.service;

import com.xiyu.bid.audit.service.AuditLogService;
import com.xiyu.bid.audit.service.IAuditLogService;
import com.xiyu.bid.batch.dto.BatchOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
class BatchOperationLogService {

    private final IAuditLogService auditLogService;

    void record(BatchOperationResponse response, String itemType, String operationType, Long userId) {
        try {
            String successIds = response.getSuccessIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            Long entityId = (response.getSuccessCount() > 0 && !response.getSuccessIds().isEmpty())
                    ? response.getSuccessIds().get(0) : null;

            AuditLogService.AuditLogEntry entry = AuditLogService.AuditLogEntry.builder()
                    .entityType(itemType)
                    .action(operationType)
                    .entityId(entityId != null ? String.valueOf(entityId) : null)
                    .userId(userId != null ? String.valueOf(userId) : null)
                    .description(String.format("Batch %s: %d success, %d failed. IDs: %s",
                            operationType.toLowerCase(Locale.ROOT),
                            response.getSuccessCount(),
                            response.getFailureCount(),
                            successIds))
                    .success(response.getFailureCount() == 0)
                    .build();

            auditLogService.log(entry);
        } catch (RuntimeException exception) {
            log.error("Failed to record batch operation log: {}", exception.getMessage());
        }
    }
}
