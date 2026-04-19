// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.casework.service;

import com.xiyu.bid.casework.dto.CaseDTO;
import com.xiyu.bid.casework.dto.CaseReferenceRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseReferenceRecordDTO;
import com.xiyu.bid.casework.dto.CaseShareRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseShareRecordDTO;
import com.xiyu.bid.casework.entity.CaseReferenceRecord;
import com.xiyu.bid.casework.entity.CaseShareRecord;
import com.xiyu.bid.casework.repository.CaseReferenceRecordRepository;
import com.xiyu.bid.casework.repository.CaseShareRecordRepository;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.CaseRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseShareRecordRepository caseShareRecordRepository;
    private final CaseReferenceRecordRepository caseReferenceRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public CaseDTO createCase(CaseDTO dto) {
        log.info("Creating case: {}", dto.getTitle());
        Case caseStudy = Case.builder()
                .title(dto.getTitle()).industry(toEntityIndustry(dto.getIndustry())).outcome(toEntityOutcome(dto.getOutcome())).amount(dto.getAmount())
                .projectDate(dto.getProjectDate()).description(dto.getDescription())
                .customerName(trimToNull(dto.getCustomerName())).locationName(trimToNull(dto.getLocationName()))
                .projectPeriod(trimToNull(dto.getProjectPeriod())).tags(copyList(dto.getTags()))
                .highlights(copyList(dto.getHighlights())).technologies(copyList(dto.getTechnologies()))
                .viewCount(defaultLong(dto.getViewCount())).useCount(defaultLong(dto.getUseCount())).build();
        Case saved = caseRepository.save(caseStudy);
        log.info("Case created successfully with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CaseDTO> getAllCases() {
        return caseRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CaseDTO getCaseById(Long id) {
        Case caseStudy = caseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Case", id.toString()));
        return toDTO(caseStudy);
    }

    @Transactional
    public CaseDTO updateCase(Long id, CaseDTO dto) {
        log.info("Updating case: {}", id);
        Case existing = caseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Case", id.toString()));
        Case updated = Case.builder()
                .id(existing.getId()).title(dto.getTitle() != null ? dto.getTitle() : existing.getTitle())
                .industry(dto.getIndustry() != null ? toEntityIndustry(dto.getIndustry()) : existing.getIndustry())
                .outcome(dto.getOutcome() != null ? toEntityOutcome(dto.getOutcome()) : existing.getOutcome())
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
                .createdAt(existing.getCreatedAt()).updatedAt(existing.getUpdatedAt()).build();
        return toDTO(caseRepository.save(updated));
    }

    @Transactional
    public void deleteCase(Long id) {
        log.info("Deleting case: {}", id);
        if (!caseRepository.existsById(id)) throw new ResourceNotFoundException("Case", id.toString());
        caseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByIndustry(CaseDTO.Industry industry) {
        return caseRepository.findByIndustry(toEntityIndustry(industry), org.springframework.data.domain.PageRequest.of(0, 1000)).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByOutcome(CaseDTO.Outcome outcome) {
        return caseRepository.findByOutcome(toEntityOutcome(outcome), org.springframework.data.domain.PageRequest.of(0, 1000)).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<CaseShareRecordDTO> getCaseShareRecords(Long caseId) {
        requireCase(caseId);
        return caseShareRecordRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toShareRecordDTO).toList();
    }

    @Transactional
    public CaseShareRecordDTO createCaseShareRecord(Long caseId, CaseShareRecordCreateRequest request) {
        requireCase(caseId);
        String token = UUID.randomUUID().toString().replace("-", "");
        String baseUrl = request.getBaseUrl().trim().replaceAll("/+$", "");
        CaseShareRecord shareRecord = CaseShareRecord.builder()
                .caseId(caseId).token(token).url(baseUrl + "/knowledge/case/detail?id=" + caseId + "&share=" + token)
                .createdBy(request.getCreatedBy()).createdByName(resolveDisplayName(request.getCreatedBy(), request.getCreatedByName()))
                .expiresAt(request.getExpiresAt()).build();
        return toShareRecordDTO(caseShareRecordRepository.save(shareRecord));
    }

    @Transactional(readOnly = true)
    public List<CaseReferenceRecordDTO> getCaseReferenceRecords(Long caseId) {
        requireCase(caseId);
        return caseReferenceRecordRepository.findByCaseIdOrderByReferencedAtDesc(caseId).stream().map(this::toReferenceRecordDTO).toList();
    }

    @Transactional
    public CaseReferenceRecordDTO createCaseReferenceRecord(Long caseId, CaseReferenceRecordCreateRequest request) {
        Case caseStudy = requireCase(caseId);
        CaseReferenceRecord referenceRecord = CaseReferenceRecord.builder()
                .caseId(caseId).referencedBy(request.getReferencedBy())
                .referencedByName(resolveDisplayName(request.getReferencedBy(), request.getReferencedByName()))
                .referenceTarget(request.getReferenceTarget().trim()).referenceContext(trimToNull(request.getReferenceContext())).build();
        CaseReferenceRecord saved = caseReferenceRecordRepository.save(referenceRecord);
        caseStudy.setUseCount(defaultLong(caseStudy.getUseCount()) + 1);
        caseRepository.save(caseStudy);
        return toReferenceRecordDTO(saved);
    }

    private Case requireCase(Long caseId) {
        return caseRepository.findById(caseId).orElseThrow(() -> new ResourceNotFoundException("Case", caseId.toString()));
    }

    private CaseDTO toDTO(Case c) {
        return CaseDTO.builder().id(c.getId()).title(c.getTitle()).industry(CaseDTO.Industry.valueOf(c.getIndustry().name())).outcome(CaseDTO.Outcome.valueOf(c.getOutcome().name()))
                .amount(c.getAmount()).projectDate(c.getProjectDate()).description(c.getDescription())
                .customerName(c.getCustomerName()).locationName(c.getLocationName()).projectPeriod(c.getProjectPeriod())
                .tags(copyList(c.getTags())).highlights(copyList(c.getHighlights())).technologies(copyList(c.getTechnologies()))
                .viewCount(defaultLong(c.getViewCount())).useCount(defaultLong(c.getUseCount()))
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }

    private CaseShareRecordDTO toShareRecordDTO(CaseShareRecord s) {
        return CaseShareRecordDTO.builder().id(s.getId()).caseId(s.getCaseId()).token(s.getToken()).url(s.getUrl())
                .createdBy(s.getCreatedBy()).createdByName(s.getCreatedByName()).expiresAt(s.getExpiresAt()).createdAt(s.getCreatedAt()).build();
    }

    private CaseReferenceRecordDTO toReferenceRecordDTO(CaseReferenceRecord r) {
        return CaseReferenceRecordDTO.builder().id(r.getId()).caseId(r.getCaseId()).referencedBy(r.getReferencedBy())
                .referencedByName(r.getReferencedByName()).referenceTarget(r.getReferenceTarget())
                .referenceContext(r.getReferenceContext()).referencedAt(r.getReferencedAt()).build();
    }

    private String resolveDisplayName(Long userId, String fallback) {
        if (userId != null) { User user = userRepository.findById(userId).orElse(null); if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName(); }
        return (fallback != null && !fallback.isBlank()) ? fallback.trim() : "未命名用户";
    }

    private Case.Industry toEntityIndustry(CaseDTO.Industry industry) { return industry == null ? null : Case.Industry.valueOf(industry.name()); }
    private Case.Outcome toEntityOutcome(CaseDTO.Outcome outcome) { return outcome == null ? null : Case.Outcome.valueOf(outcome.name()); }
    private List<String> copyList(List<String> source) { return source == null ? new ArrayList<>() : new ArrayList<>(source); }
    private String trimToNull(String value) { if (value == null) return null; String t = value.trim(); return t.isEmpty() ? null : t; }
    private Long defaultLong(Long value) { return value == null ? 0L : value; }
}
