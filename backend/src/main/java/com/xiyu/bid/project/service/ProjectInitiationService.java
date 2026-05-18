// Input: 提交/更新立项请求 + 当前用户
// Output: InitiationViewDto；通过 InitiationFieldPolicy 校验 + 持久化 + 审计
// Pos: project/service/ - 编排层（不含纯规则）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.project.entity.ProjectInitiationDetails;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.project.core.InitiationFieldPolicy;
import com.xiyu.bid.project.core.ProjectFieldLockPolicy;
import com.xiyu.bid.project.core.ProjectStage;
import com.xiyu.bid.project.dto.InitiationDto;
import com.xiyu.bid.project.dto.InitiationViewDto;
import com.xiyu.bid.project.repository.ProjectInitiationDetailsRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectInitiationService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ProjectInitiationDetailsRepository repository;
    private final ProjectRepository projectRepository;
    private final ProjectStageService projectStageService;
    private final ProjectAccessScopeService projectAccessScopeService;

    @Auditable(action = "SUBMIT_INITIATION", entityType = "ProjectInitiationDetails", description = "提交项目立项审核")
    public InitiationViewDto submit(Long projectId, InitiationDto req, Long currentUserId) {
        projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
        mustGetProject(projectId);
        var input = toInput(req);
        var decision = InitiationFieldPolicy.validate(input);
        if (!decision.allowed()) {
            var deny = (InitiationFieldPolicy.Decision.Deny) decision;
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, deny.reasonText());
        }
        ProjectInitiationDetails entity = repository.findByProjectId(projectId)
                .orElseGet(() -> ProjectInitiationDetails.builder()
                        .projectId(projectId)
                        .createdBy(currentUserId)
                        .locked(Boolean.FALSE)
                        .reviewStatus("DRAFT")
                        .build());
        // 蓝图 V1.1 §4.3: 不能重复提交已审核或待审核的项目
        if ("PENDING_REVIEW".equals(entity.getReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "项目已提交审核，请勿重复提交");
        }
        if ("APPROVED".equals(entity.getReviewStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "项目已通过审核，不可重新提交");
        }
        applyInput(entity, req);
        // 蓝图 V1.1 §4.3: 提交审核 → PENDING_REVIEW（不再直接推进到 DRAFTING）
        entity.setReviewStatus("PENDING_REVIEW");
        entity.setRejectionReason(null); // 清空驳回原因
        entity.setUpdatedBy(currentUserId);
        ProjectInitiationDetails saved = repository.save(entity);
        log.info("Initiation submitted for review project={} user={}", projectId, currentUserId);
        return toView(saved);
    }

    @Auditable(action = "UPDATE_INITIATION", entityType = "ProjectInitiationDetails", description = "更新项目立项")
    public InitiationViewDto update(Long projectId, InitiationDto req, Long currentUserId) {
        projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
        mustGetProject(projectId);
        // §3.6 全字段锁定 — CLOSED 阶段拒绝写入。
        ProjectStage stage = projectStageService.currentStage(projectId);
        var lockDecision0 = ProjectFieldLockPolicy.assertWritable(stage, "initiation");
        if (!lockDecision0.allowed()) {
            var deny = (ProjectFieldLockPolicy.Decision.Deny) lockDecision0;
            throw new ResponseStatusException(HttpStatus.LOCKED, deny.reason());
        }
        ProjectInitiationDetails existing = repository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectInitiationDetails", String.valueOf(projectId)));
        boolean lockedAlready = Boolean.TRUE.equals(existing.getLocked());
        var existingInput = toInput(existing);
        var requestedInput = mergeForUpdate(existingInput, req);
        var lockDecision = InitiationFieldPolicy.validateUpdate(existingInput, requestedInput, lockedAlready);
        if (!lockDecision.allowed()) {
            var deny = (InitiationFieldPolicy.Decision.Deny) lockDecision;
            throw new ResponseStatusException(HttpStatus.LOCKED, deny.reasonText());
        }
        var fullDecision = InitiationFieldPolicy.validate(requestedInput);
        if (!fullDecision.allowed()) {
            var deny = (InitiationFieldPolicy.Decision.Deny) fullDecision;
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, deny.reasonText());
        }
        applyInput(existing, toDto(requestedInput));
        existing.setUpdatedBy(currentUserId);
        ProjectInitiationDetails saved = repository.save(existing);
        log.info("Initiation updated project={} user={}", projectId, currentUserId);
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public Optional<InitiationViewDto> getByProject(Long projectId) {
        return repository.findByProjectId(projectId).map(this::toView);
    }

    private Project mustGetProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private InitiationFieldPolicy.InitiationInput toInput(InitiationDto d) {
        return new InitiationFieldPolicy.InitiationInput(
                d.getOwnerUnit(), d.getExpectedBidders(), d.getContractPeriodMonths(),
                d.getProjectType(), d.getCustomerType(), d.getAnnualRevenue(),
                d.getBidOpenTime(), d.getOwnerUserId(), d.getDepartmentSnapshot(),
                d.getDepositAmount(), d.getDepositPaymentMethod(), d.getCompetitors());
    }

    private InitiationFieldPolicy.InitiationInput toInput(ProjectInitiationDetails e) {
        return new InitiationFieldPolicy.InitiationInput(
                e.getOwnerUnit(), e.getExpectedBidders(), e.getContractPeriodMonths(),
                parseProjectType(e.getProjectType()), parseCustomerType(e.getCustomerType()),
                e.getAnnualRevenue(), e.getBidOpenTime(), e.getOwnerUserId(),
                e.getDepartmentSnapshot(), e.getDepositAmount(), e.getDepositPaymentMethod(),
                e.getCompetitors());
    }

    private InitiationDto toDto(InitiationFieldPolicy.InitiationInput in) {
        return InitiationDto.builder()
                .ownerUnit(in.ownerUnit()).expectedBidders(in.expectedBidders())
                .contractPeriodMonths(in.contractPeriodMonths())
                .projectType(in.projectType()).customerType(in.customerType())
                .annualRevenue(in.annualRevenue()).bidOpenTime(in.bidOpenTime())
                .ownerUserId(in.ownerUserId()).departmentSnapshot(in.departmentSnapshot())
                .depositAmount(in.depositAmount()).depositPaymentMethod(in.depositPaymentMethod())
                .competitors(in.competitors()).build();
    }

    /** 把请求体覆盖到 existing 之上：null 字段表示不修改。 */
    private InitiationFieldPolicy.InitiationInput mergeForUpdate(
            InitiationFieldPolicy.InitiationInput base, InitiationDto patch) {
        return new InitiationFieldPolicy.InitiationInput(
                patch.getOwnerUnit() != null ? patch.getOwnerUnit() : base.ownerUnit(),
                patch.getExpectedBidders() != null ? patch.getExpectedBidders() : base.expectedBidders(),
                patch.getContractPeriodMonths() != null ? patch.getContractPeriodMonths() : base.contractPeriodMonths(),
                patch.getProjectType() != null ? patch.getProjectType() : base.projectType(),
                patch.getCustomerType() != null ? patch.getCustomerType() : base.customerType(),
                patch.getAnnualRevenue() != null ? patch.getAnnualRevenue() : base.annualRevenue(),
                patch.getBidOpenTime() != null ? patch.getBidOpenTime() : base.bidOpenTime(),
                patch.getOwnerUserId() != null ? patch.getOwnerUserId() : base.ownerUserId(),
                patch.getDepartmentSnapshot() != null ? patch.getDepartmentSnapshot() : base.departmentSnapshot(),
                patch.getDepositAmount() != null ? patch.getDepositAmount() : base.depositAmount(),
                patch.getDepositPaymentMethod() != null ? patch.getDepositPaymentMethod() : base.depositPaymentMethod(),
                patch.getCompetitors() != null ? patch.getCompetitors() : base.competitors());
    }

    private void applyInput(ProjectInitiationDetails e, InitiationDto d) {
        if (d.getOwnerUnit() != null) e.setOwnerUnit(d.getOwnerUnit());
        if (d.getExpectedBidders() != null) e.setExpectedBidders(d.getExpectedBidders());
        if (d.getContractPeriodMonths() != null) e.setContractPeriodMonths(d.getContractPeriodMonths());
        if (d.getProjectType() != null) e.setProjectType(d.getProjectType().name());
        if (d.getCustomerType() != null) e.setCustomerType(d.getCustomerType().name());
        if (d.getAnnualRevenue() != null) e.setAnnualRevenue(d.getAnnualRevenue());
        if (d.getBidOpenTime() != null) {
            e.setBidOpenTime(d.getBidOpenTime());
            e.setBidMonth(d.getBidOpenTime().format(MONTH_FMT));
        }
        if (d.getOwnerUserId() != null) e.setOwnerUserId(d.getOwnerUserId());
        if (d.getDepartmentSnapshot() != null) e.setDepartmentSnapshot(d.getDepartmentSnapshot());
        if (d.getDepositAmount() != null) e.setDepositAmount(d.getDepositAmount());
        if (d.getDepositPaymentMethod() != null) e.setDepositPaymentMethod(d.getDepositPaymentMethod());
        if (d.getCompetitors() != null) e.setCompetitors(d.getCompetitors());
    }

    private InitiationFieldPolicy.ProjectType parseProjectType(String v) {
        if (v == null) return null;
        try { return InitiationFieldPolicy.ProjectType.valueOf(v); } catch (IllegalArgumentException ex) { return null; }
    }

    private InitiationFieldPolicy.CustomerType parseCustomerType(String v) {
        if (v == null) return null;
        try { return InitiationFieldPolicy.CustomerType.valueOf(v); } catch (IllegalArgumentException ex) { return null; }
    }

    private InitiationViewDto toView(ProjectInitiationDetails e) {
        return InitiationViewDto.builder()
                .id(e.getId()).projectId(e.getProjectId())
                .ownerUnit(e.getOwnerUnit()).expectedBidders(e.getExpectedBidders())
                .contractPeriodMonths(e.getContractPeriodMonths())
                .projectType(e.getProjectType()).customerType(e.getCustomerType())
                .annualRevenue(e.getAnnualRevenue()).bidOpenTime(e.getBidOpenTime())
                .bidMonth(e.getBidMonth()).ownerUserId(e.getOwnerUserId())
                .departmentSnapshot(e.getDepartmentSnapshot())
                .depositAmount(e.getDepositAmount()).depositPaymentMethod(e.getDepositPaymentMethod())
                .competitors(e.getCompetitors()).locked(e.getLocked())
                .reviewStatus(e.getReviewStatus())
                .rejectionReason(e.getRejectionReason())
                .reviewedBy(e.getReviewedBy()).reviewedAt(e.getReviewedAt())
                .aiRiskLevel(e.getAiRiskLevel()).tenderDocumentId(e.getTenderDocumentId())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }
}
