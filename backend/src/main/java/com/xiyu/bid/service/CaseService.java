// Input: Repository, 相关依赖
// Output: 业务服务、数据操作
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.service;

import com.xiyu.bid.dto.CaseDTO;
import com.xiyu.bid.entity.Case;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 案例管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;

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
                .build();

        Case saved = caseRepository.save(caseStudy);
        log.info("Case created successfully with id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * 获取所有案例（限制返回1000条）
     */
    public List<CaseDTO> getAllCases() {
        log.debug("Fetching all cases");
        return caseRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取案例
     */
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
    public List<CaseDTO> getCasesByIndustry(Case.Industry industry) {
        log.debug("Fetching cases by industry: {}", industry);
        return caseRepository.findByIndustry(industry, org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据结果查找案例（限制返回1000条）
     */
    public List<CaseDTO> getCasesByOutcome(Case.Outcome outcome) {
        log.debug("Fetching cases by outcome: {}", outcome);
        return caseRepository.findByOutcome(outcome, org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
                .createdAt(caseStudy.getCreatedAt())
                .updatedAt(caseStudy.getUpdatedAt())
                .build();
    }
}
