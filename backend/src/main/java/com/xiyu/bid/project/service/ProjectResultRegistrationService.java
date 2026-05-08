// Input: 登记结果请求 + 当前用户
// Output: ResultDTO；通过策略校验+持久化+审计
// Pos: project/service/ - 编排层（不含纯规则）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.project.core.ResultRegistrationFieldPolicy;
import com.xiyu.bid.project.dto.ResultDTO;
import com.xiyu.bid.project.dto.ResultRegistrationRequest;
import com.xiyu.bid.project.entity.ProjectResult;
import com.xiyu.bid.project.repository.ProjectResultRepository;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PRD §3.4 结果确认编排：
 * <ul>
 *   <li>幂等：同 projectId 已登记 → 409 CONFLICT。</li>
 *   <li>策略：调 {@link ResultRegistrationFieldPolicy}（FAILED/ABANDONED 必须 summary，全部必须 evidence）。</li>
 *   <li>审计：@Auditable("REGISTER_PROJECT_RESULT")。</li>
 *   <li>FSM 推进：仅校验不直接写 stage，由 WS-G 编排统一处理。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectResultRegistrationService {

    private final ProjectResultRepository repository;
    private final ProjectRepository projectRepository;

    @Auditable(action = "REGISTER_PROJECT_RESULT", entityType = "ProjectResult", description = "登记项目结果")
    public ResultDTO register(Long projectId, ResultRegistrationRequest req, Long currentUserId) {
        Project project = mustGetProject(projectId);
        if (repository.findByProjectId(projectId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "项目结果已登记，不可重复提交");
        }
        var input = ResultRegistrationFieldPolicy.ResultInput.builder()
                .resultType(req.getResultType())
                .awardAmount(req.getAwardAmount())
                .contractStartDate(req.getContractStartDate())
                .contractEndDate(req.getContractEndDate())
                .evidenceFileIds(req.getEvidenceFileIds())
                .summary(req.getSummary())
                .build();
        var decision = ResultRegistrationFieldPolicy.validate(input);
        if (!decision.allowed()) {
            var deny = (ResultRegistrationFieldPolicy.Decision.Deny) decision;
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, deny.reason());
        }
        ProjectResult entity = ProjectResult.builder()
                .projectId(project.getId())
                .resultType(req.getResultType().name())
                .awardAmount(req.getAwardAmount())
                .contractStartDate(req.getContractStartDate())
                .contractEndDate(req.getContractEndDate())
                .evidenceAttachmentId(firstId(req.getEvidenceFileIds()))
                .evidenceDocIds(toCsv(req.getEvidenceFileIds()))
                .summary(req.getSummary())
                .registeredAt(LocalDateTime.now())
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();
        ProjectResult saved = repository.save(entity);
        log.info("ProjectResult registered project={} type={} user={}",
                projectId, req.getResultType(), currentUserId);
        return toDto(saved, currentUserId);
    }

    @Transactional(readOnly = true)
    public Optional<ResultDTO> getByProject(Long projectId) {
        return repository.findByProjectId(projectId).map(e -> toDto(e, e.getCreatedBy()));
    }

    private Project mustGetProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private static Long firstId(List<Long> ids) {
        return (ids == null || ids.isEmpty()) ? null : ids.get(0);
    }

    private static String toCsv(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().filter(java.util.Objects::nonNull)
                .map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).toList();
    }

    private ResultDTO toDto(ProjectResult e, Long registeredBy) {
        return ResultDTO.builder()
                .id(e.getId())
                .projectId(e.getProjectId())
                .resultType(e.getResultType())
                .awardAmount(e.getAwardAmount())
                .contractStartDate(e.getContractStartDate())
                .contractEndDate(e.getContractEndDate())
                .evidenceFileIds(fromCsv(e.getEvidenceDocIds()))
                .summary(e.getSummary())
                .registeredAt(e.getRegisteredAt())
                .registeredBy(registeredBy)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
