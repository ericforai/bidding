// Input: versionhistory repositories, DTOs, and support services
// Output: Version History business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.versionhistory.service;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.service.AuditLogService;
import com.xiyu.bid.service.IAuditLogService;
import com.xiyu.bid.versionhistory.dto.DocumentVersionDTO;
import com.xiyu.bid.versionhistory.dto.VersionCreateRequest;
import com.xiyu.bid.versionhistory.dto.VersionDiffDTO;
import com.xiyu.bid.versionhistory.entity.DocumentVersion;
import com.xiyu.bid.versionhistory.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 版本历史服务
 * 提供文档版本管理功能，包括创建、查询、比较和回滚
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionHistoryService {

    private final DocumentVersionRepository repository;
    private final IAuditLogService auditLogService;

    /**
     * 创建新版本
     * 使用悲观锁防止并发创建版本时的竞态条件
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized DocumentVersionDTO createVersion(VersionCreateRequest request) {
        // 验证输入
        if (request.getProjectId() == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        if (request.getCreatedBy() == null) {
            throw new IllegalArgumentException("Created by is required");
        }

        // 获取下一个版本号
        Integer nextVersionNumber = repository.getNextVersionNumber(request.getProjectId());

        // 标记当前版本为非当前
        repository.findCurrentVersionByProjectId(request.getProjectId())
                .ifPresent(currentVersion -> {
                    currentVersion.setIsCurrent(false);
                    repository.save(currentVersion);
                });

        // 创建新版本
        DocumentVersion version = DocumentVersion.builder()
                .projectId(request.getProjectId())
                .documentId(request.getDocumentId())
                .versionNumber(nextVersionNumber)
                .content(request.getContent())
                .filePath(request.getFilePath())
                .changeSummary(request.getChangeSummary() != null
                        ? request.getChangeSummary()
                        : "Version " + nextVersionNumber)
                .createdBy(request.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .isCurrent(true)
                .build();

        DocumentVersion savedVersion = repository.save(version);

        // 记录审计日志
        logAudit("CREATE", "DocumentVersion", savedVersion.getId().toString(),
                "Created version " + nextVersionNumber + " for project " + request.getProjectId(),
                null, savedVersion.toString(), request.getCreatedBy());

        return DocumentVersionDTO.fromEntity(savedVersion);
    }

    /**
     * 获取项目的所有版本
     */
    public List<DocumentVersionDTO> getVersionsByProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        return repository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(DocumentVersionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定版本
     */
    public DocumentVersionDTO getVersion(Long versionId) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID is required");
        }

        DocumentVersion version = repository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId));

        return DocumentVersionDTO.fromEntity(version);
    }

    /**
     * 获取项目的最新版本
     */
    public DocumentVersionDTO getLatestVersion(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        DocumentVersion version = repository.findCurrentVersionByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No versions found for project: " + projectId));

        return DocumentVersionDTO.fromEntity(version);
    }

    /**
     * 比较两个版本的差异
     */
    public VersionDiffDTO compareVersions(Long versionId1, Long versionId2) {
        if (versionId1 == null || versionId2 == null) {
            throw new IllegalArgumentException("Both version IDs are required");
        }

        DocumentVersion version1 = repository.findById(versionId1)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId1));
        DocumentVersion version2 = repository.findById(versionId2)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId2));

        List<String> differences = computeDiff(version1.getContent(), version2.getContent());

        return VersionDiffDTO.builder()
                .version1Id(version1.getId())
                .version2Id(version2.getId())
                .version1Number(version1.getVersionNumber())
                .version2Number(version2.getVersionNumber())
                .content1(version1.getContent())
                .content2(version2.getContent())
                .differences(differences)
                .build();
    }

    /**
     * 回滚到指定版本
     */
    @Transactional
    public DocumentVersionDTO rollbackToVersion(Long projectId, Long versionId, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        // 获取目标版本
        DocumentVersion targetVersion = repository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId));

        // 验证版本属于该项目
        if (!targetVersion.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Version does not belong to the specified project");
        }

        // 获取当前版本
        DocumentVersion currentVersion = repository.findCurrentVersionByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No current version found for project: " + projectId));

        // 标记当前版本为非当前
        currentVersion.setIsCurrent(false);
        repository.save(currentVersion);

        // 创建新版本（内容来自目标版本）
        Integer nextVersionNumber = repository.getNextVersionNumber(projectId);
        DocumentVersion newVersion = DocumentVersion.builder()
                .projectId(projectId)
                .documentId(targetVersion.getDocumentId())
                .versionNumber(nextVersionNumber)
                .content(targetVersion.getContent())
                .filePath(targetVersion.getFilePath())
                .changeSummary("Rollback to version " + targetVersion.getVersionNumber())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .isCurrent(true)
                .build();

        DocumentVersion savedVersion = repository.save(newVersion);

        // 记录审计日志
        logAudit("ROLLBACK", "DocumentVersion", savedVersion.getId().toString(),
                "Rolled back to version " + targetVersion.getVersionNumber(),
                currentVersion.toString(), savedVersion.toString(), userId);

        return DocumentVersionDTO.fromEntity(savedVersion);
    }

    /**
     * 标记指定版本为当前版本
     */
    @Transactional
    public void markAsCurrent(Long versionId) {
        if (versionId == null) {
            throw new IllegalArgumentException("Version ID is required");
        }

        // 获取目标版本
        DocumentVersion targetVersion = repository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId));

        // 获取并更新当前版本
        repository.findCurrentVersionByProjectId(targetVersion.getProjectId())
                .ifPresent(current -> {
                    current.setIsCurrent(false);
                    repository.save(current);
                });

        // 标记目标版本为当前
        targetVersion.setIsCurrent(true);
        repository.save(targetVersion);
    }

    /**
     * 计算两个文本之间的差异
     * 使用简单的行比较算法
     */
    private List<String> computeDiff(String content1, String content2) {
        List<String> differences = new ArrayList<>();

        if (content1 == null && content2 == null) {
            return differences;
        }
        if (content1 == null) {
            String sanitizedContent2 = sanitizeForLog(content2);
            differences.add("Content added: " + sanitizedContent2);
            return differences;
        }
        if (content2 == null) {
            String sanitizedContent1 = sanitizeForLog(content1);
            differences.add("Content removed: " + sanitizedContent1);
            return differences;
        }

        String[] lines1 = content1.split("\\n");
        String[] lines2 = content2.split("\\n");

        int maxLines = Math.max(lines1.length, lines2.length);

        for (int i = 0; i < maxLines; i++) {
            if (i >= lines1.length) {
                String sanitizedLine = sanitizeForLog(lines2[i]);
                differences.add("Line " + (i + 1) + " added: " + sanitizedLine);
            } else if (i >= lines2.length) {
                String sanitizedLine = sanitizeForLog(lines1[i]);
                differences.add("Line " + (i + 1) + " removed: " + sanitizedLine);
            } else if (!lines1[i].equals(lines2[i])) {
                String sanitizedLine1 = sanitizeForLog(lines1[i]);
                String sanitizedLine2 = sanitizeForLog(lines2[i]);
                differences.add("Line " + (i + 1) + " changed from '" + sanitizedLine1 + "' to '" + sanitizedLine2 + "'");
            }
        }

        return differences;
    }

    /**
     * 清理内容以防止日志注入攻击
     * 移除换行符、回车符和其他控制字符
     */
    private String sanitizeForLog(String content) {
        if (content == null) {
            return "";
        }
        // 移除可能用于日志注入的换行符、制表符和其他控制字符
        return content.replaceAll("[\\r\\n\\t\\x00\\x1b]", "");
    }

    /**
     * 记录审计日志
     */
    private void logAudit(String action, String entityType, String entityId,
                         String description, String oldValue, String newValue, Long userId) {
        try {
            AuditLogService.AuditLogEntry entry = AuditLogService.AuditLogEntry.builder()
                    .userId(userId != null ? userId.toString() : null)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .success(true)
                    .build();

            auditLogService.log(entry);
        } catch (Exception e) {
            log.error("Failed to log audit entry", e);
        }
    }
}
