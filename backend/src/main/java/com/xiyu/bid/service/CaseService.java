// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.service;

import com.xiyu.bid.casework.dto.CaseReferenceRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseReferenceRecordDTO;
import com.xiyu.bid.casework.dto.CaseShareRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseShareRecordDTO;
import com.xiyu.bid.casework.entity.CaseReferenceRecord;
import com.xiyu.bid.casework.entity.CaseShareRecord;
import com.xiyu.bid.casework.repository.CaseReferenceRecordRepository;
import com.xiyu.bid.casework.repository.CaseShareRecordRepository;
import com.xiyu.bid.dto.CaseDTO;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.CaseRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 案例管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseShareRecordRepository caseShareRecordRepository;
    private final CaseReferenceRecordRepository caseReferenceRecordRepository;
    private final UserRepository userRepository;

    /**
     * 创建案例
     */
    @Transactional
    public CaseDTO createCase(CaseDTO dto) {
        log.info("Creating case: {}", dto.getTitle());

        Case caseStudy = Case.builder()
                .title(dto.getTitle())
                .industry(dto.getIndustry())
                .outcome(dto.getOutcome())
                .amount(dto.getAmount())
                .projectDate(dto.getProjectDate())
                .description(dto.getDescription())
                .customerName(trimToNull(dto.getCustomerName()))
                .locationName(trimToNull(dto.getLocationName()))
                .projectPeriod(trimToNull(dto.getProjectPeriod()))
                .tags(copyList(dto.getTags()))
                .highlights(copyList(dto.getHighlights()))
                .technologies(copyList(dto.getTechnologies()))
                .viewCount(defaultLong(dto.getViewCount()))
                .useCount(defaultLong(dto.getUseCount()))
                .build();

        Case saved = caseRepository.save(caseStudy);
        log.info("Case created successfully with id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * 获取所有案例（限制返回1000条）
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getAllCases() {
        log.debug("Fetching all cases");
        return caseRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取案例
     */
    @Transactional(readOnly = true)
    public CaseDTO getCaseById(Long id) {
        log.debug("Fetching case by id: {}", id);
        Case caseStudy = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case", id.toString()));
        return toDTO(caseStudy);
    }

    /**
     * 更新案例
     */
    @Transactional
    public CaseDTO updateCase(Long id, CaseDTO dto) {
        log.info("Updating case: {}", id);

        Case existing = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case", id.toString()));

        // 使用Builder模式创建新对象而非直接修改
        Case updated = Case.builder()
                .id(existing.getId())
                .title(dto.getTitle() != null ? dto.getTitle() : existing.getTitle())
                .industry(dto.getIndustry() != null ? dto.getIndustry() : existing.getIndustry())
                .outcome(dto.getOutcome() != null ? dto.getOutcome() : existing.getOutcome())
                .amount(dto.getAmount() != null ? dto.getAmount() : existing.getAmount())
                .projectDate(dto.getProjectDate() != null ? dto.getProjectDate() : existing.getProjectDate())
                .description(dto.getDescription() != null ? dto.getDescription() : existing.getDescription())
                .customerName(dto.getCustomerName() != null ? trimToNull(dto.getCustomerName()) : existing.getCustomerName())
                .locationName(dto.getLocationName() != null ? trimToNull(dto.getLocationName()) : existing.getLocationName())
                .projectPeriod(dto.getProjectPeriod() != null ? trimToNull(dto.getProjectPeriod()) : existing.getProjectPeriod())
                .tags(dto.getTags() != null ? copyList(dto.getTags()) : copyList(existing.getTags()))
                .highlights(dto.getHighlights() != null ? copyList(dto.getHighlights()) : copyList(existing.getHighlights()))
                .technologies(dto.getTechnologies() != null ? copyList(dto.getTechnologies()) : copyList(existing.getTechnologies()))
                .viewCount(dto.getViewCount() != null ? dto.getViewCount() : existing.getViewCount())
                .useCount(dto.getUseCount() != null ? dto.getUseCount() : existing.getUseCount())
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();

        updated = caseRepository.save(updated);
        log.info("Case updated successfully: {}", id);

        return toDTO(updated);
    }

    /**
     * 删除案例
     */
    @Transactional
    public void deleteCase(Long id) {
        log.info("Deleting case: {}", id);

        if (!caseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Case", id.toString());
        }

        caseRepository.deleteById(id);
        log.info("Case deleted successfully: {}", id);
    }

    /**
     * 根据行业查找案例（限制返回1000条）
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByIndustry(Case.Industry industry) {
        log.debug("Fetching cases by industry: {}", industry);
        return caseRepository.findByIndustry(industry, org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据结果查找案例（限制返回1000条）
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByOutcome(Case.Outcome outcome) {
        log.debug("Fetching cases by outcome: {}", outcome);
        return caseRepository.findByOutcome(outcome, org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CaseShareRecordDTO> getCaseShareRecords(Long caseId) {
        requireCase(caseId);
        return caseShareRecordRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(this::toShareRecordDTO)
                .toList();
    }

    @Transactional
    public CaseShareRecordDTO createCaseShareRecord(Long caseId, CaseShareRecordCreateRequest request) {
        requireCase(caseId);
        String token = UUID.randomUUID().toString().replace("-", "");
        String baseUrl = request.getBaseUrl().trim().replaceAll("/+$", "");
        CaseShareRecord shareRecord = CaseShareRecord.builder()
                .caseId(caseId)
                .token(token)
                .url(baseUrl + "/knowledge/case/detail?id=" + caseId + "&share=" + token)
                .createdBy(request.getCreatedBy())
                .createdByName(resolveDisplayName(request.getCreatedBy(), request.getCreatedByName()))
                .expiresAt(request.getExpiresAt())
                .build();
        return toShareRecordDTO(caseShareRecordRepository.save(shareRecord));
    }

    @Transactional(readOnly = true)
    public List<CaseReferenceRecordDTO> getCaseReferenceRecords(Long caseId) {
        requireCase(caseId);
        return caseReferenceRecordRepository.findByCaseIdOrderByReferencedAtDesc(caseId).stream()
                .map(this::toReferenceRecordDTO)
                .toList();
    }

    @Transactional
    public CaseReferenceRecordDTO createCaseReferenceRecord(Long caseId, CaseReferenceRecordCreateRequest request) {
        Case caseStudy = requireCase(caseId);
        CaseReferenceRecord referenceRecord = CaseReferenceRecord.builder()
                .caseId(caseId)
                .referencedBy(request.getReferencedBy())
                .referencedByName(resolveDisplayName(request.getReferencedBy(), request.getReferencedByName()))
                .referenceTarget(request.getReferenceTarget().trim())
                .referenceContext(trimToNull(request.getReferenceContext()))
                .build();
        CaseReferenceRecord saved = caseReferenceRecordRepository.save(referenceRecord);
        caseStudy.setUseCount(defaultLong(caseStudy.getUseCount()) + 1);
        caseRepository.save(caseStudy);
        return toReferenceRecordDTO(saved);
    }

    private Case requireCase(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", caseId.toString()));
    }

    private CaseDTO toDTO(Case caseStudy) {
        return CaseDTO.builder()
                .id(caseStudy.getId())
                .title(caseStudy.getTitle())
                .industry(caseStudy.getIndustry())
                .outcome(caseStudy.getOutcome())
                .amount(caseStudy.getAmount())
                .projectDate(caseStudy.getProjectDate())
                .description(caseStudy.getDescription())
                .customerName(caseStudy.getCustomerName())
                .locationName(caseStudy.getLocationName())
                .projectPeriod(caseStudy.getProjectPeriod())
                .tags(copyList(caseStudy.getTags()))
                .highlights(copyList(caseStudy.getHighlights()))
                .technologies(copyList(caseStudy.getTechnologies()))
                .viewCount(defaultLong(caseStudy.getViewCount()))
                .useCount(defaultLong(caseStudy.getUseCount()))
                .createdAt(caseStudy.getCreatedAt())
                .updatedAt(caseStudy.getUpdatedAt())
                .build();
    }

    private CaseShareRecordDTO toShareRecordDTO(CaseShareRecord shareRecord) {
        return CaseShareRecordDTO.builder()
                .id(shareRecord.getId())
                .caseId(shareRecord.getCaseId())
                .token(shareRecord.getToken())
                .url(shareRecord.getUrl())
                .createdBy(shareRecord.getCreatedBy())
                .createdByName(shareRecord.getCreatedByName())
                .expiresAt(shareRecord.getExpiresAt())
                .createdAt(shareRecord.getCreatedAt())
                .build();
    }

    private CaseReferenceRecordDTO toReferenceRecordDTO(CaseReferenceRecord referenceRecord) {
        return CaseReferenceRecordDTO.builder()
                .id(referenceRecord.getId())
                .caseId(referenceRecord.getCaseId())
                .referencedBy(referenceRecord.getReferencedBy())
                .referencedByName(referenceRecord.getReferencedByName())
                .referenceTarget(referenceRecord.getReferenceTarget())
                .referenceContext(referenceRecord.getReferenceContext())
                .referencedAt(referenceRecord.getReferencedAt())
                .build();
    }

    private String resolveDisplayName(Long userId, String fallback) {
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
                return user.getFullName();
            }
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return "未命名用户";
    }

    private List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
