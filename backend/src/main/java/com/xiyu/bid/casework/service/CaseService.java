// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.casework.service;

import com.xiyu.bid.casework.application.service.CaseCrudAppService;
import com.xiyu.bid.casework.application.service.CasePromotionAppService;
import com.xiyu.bid.casework.application.service.CaseReferenceAppService;
import com.xiyu.bid.casework.application.service.CaseSearchAppService;
import com.xiyu.bid.casework.application.service.CaseShareAppService;
import com.xiyu.bid.casework.domain.model.CaseSearchCriteria;
import com.xiyu.bid.casework.domain.model.CaseSearchOptions;
import com.xiyu.bid.casework.dto.CaseDTO;
import com.xiyu.bid.casework.dto.CasePromoteFromProjectRequest;
import com.xiyu.bid.casework.dto.CaseRecommendationDTO;
import com.xiyu.bid.casework.dto.CaseReferenceRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseReferenceRecordDTO;
import com.xiyu.bid.casework.dto.CaseSearchOptionsDTO;
import com.xiyu.bid.casework.dto.CaseSearchResultDTO;
import com.xiyu.bid.casework.dto.CaseShareRecordCreateRequest;
import com.xiyu.bid.casework.dto.CaseShareRecordDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CaseService {

    private final CaseCrudAppService crudAppService;
    private final CaseShareAppService shareAppService;
    private final CaseReferenceAppService referenceAppService;
    private final CaseSearchAppService searchAppService;
    private final CasePromotionAppService promotionAppService;

    public CaseDTO createCase(CaseDTO dto) {
        return crudAppService.create(dto);
    }

    @Transactional(readOnly = true)
    public List<CaseDTO> getAllCases() {
        return crudAppService.findAll();
    }

    @Transactional(readOnly = true)
    public CaseDTO getCaseById(Long id) {
        return crudAppService.findById(id);
    }

    public CaseDTO updateCase(Long id, CaseDTO dto) {
        return crudAppService.update(id, dto);
    }

    public void deleteCase(Long id) {
        crudAppService.delete(id);
    }

    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByIndustry(CaseDTO.Industry industry) {
        return crudAppService.findByIndustry(industry);
    }

    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByOutcome(CaseDTO.Outcome outcome) {
        return crudAppService.findByOutcome(outcome);
    }

    @Transactional(readOnly = true)
    public List<CaseShareRecordDTO> getCaseShareRecords(Long caseId) {
        return shareAppService.getShareRecords(caseId);
    }

    public CaseShareRecordDTO createCaseShareRecord(Long caseId, CaseShareRecordCreateRequest request) {
        return shareAppService.createShareRecord(caseId, request);
    }

    @Transactional(readOnly = true)
    public List<CaseReferenceRecordDTO> getCaseReferenceRecords(Long caseId) {
        return referenceAppService.getReferenceRecords(caseId);
    }

    public CaseReferenceRecordDTO createCaseReferenceRecord(Long caseId, CaseReferenceRecordCreateRequest request) {
        return referenceAppService.createReferenceRecord(caseId, request);
    }

    @Transactional(readOnly = true)
    public CaseSearchResultDTO searchCases(
            String keyword,
            String industry,
            String productLine,
            String outcome,
            Integer year,
            BigDecimal amountMin,
            BigDecimal amountMax,
            List<String> tags,
            String status,
            String visibility,
            int page,
            int pageSize,
            String sort) {
        return searchAppService.search(new CaseSearchCriteria(
                keyword,
                industry,
                productLine,
                outcome,
                year,
                amountMin,
                amountMax,
                tags,
                status,
                visibility,
                page,
                pageSize,
                sort));
    }

    @Transactional(readOnly = true)
    public CaseSearchOptionsDTO getSearchOptions() {
        CaseSearchOptions options = searchAppService.getSearchOptions();
        return CaseSearchOptionsDTO.builder()
                .industries(options.industries())
                .outcomes(options.outcomes())
                .statuses(options.statuses())
                .visibilities(options.visibilities())
                .productLines(options.productLines())
                .tags(options.tags())
                .sortOptions(options.sortOptions())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CaseRecommendationDTO> getRelatedCases(Long caseId, int limit) {
        return searchAppService.getRelatedCases(caseId, limit);
    }

    public CaseDTO promoteFromProject(CasePromoteFromProjectRequest request) {
        return promotionAppService.promoteFromProject(request);
    }
}
