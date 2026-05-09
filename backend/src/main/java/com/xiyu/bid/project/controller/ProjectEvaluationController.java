// Input: HTTP 请求 (PATCH sub-stage / POST evidence / GET)
// Output: ApiResponse<EvaluationDTO>
// Pos: project/controller/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.project.dto.EvaluationDTO;
import com.xiyu.bid.project.dto.EvaluationEvidenceAttachRequest;
import com.xiyu.bid.project.dto.EvaluationSubStageUpdateRequest;
import com.xiyu.bid.project.service.ProjectEvaluationService;
import com.xiyu.bid.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/projects/{projectId}/evaluation")
@RequiredArgsConstructor
@Slf4j
public class ProjectEvaluationController {

    private final ProjectEvaluationService service;
    private final AuthService authService;

    /** 切换评标子状态：BID_LEAD（映射 MANAGER/ADMIN）。 */
    @PatchMapping("/sub-stage")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EvaluationDTO>> transitionSubStage(
            @PathVariable Long projectId,
            @Valid @RequestBody EvaluationSubStageUpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        EvaluationDTO dto = service.transitionSubStage(projectId, req, userId);
        return ResponseEntity.ok(ApiResponse.success("Evaluation sub-stage updated", dto));
    }

    /** 附加评标证据：BID_LEAD（映射 MANAGER/ADMIN）。 */
    @PostMapping("/evidence")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EvaluationDTO>> attachEvidence(
            @PathVariable Long projectId,
            @Valid @RequestBody EvaluationEvidenceAttachRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        EvaluationDTO dto = service.attachEvidence(projectId, req, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evidence attached", dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<EvaluationDTO>> get(@PathVariable Long projectId) {
        EvaluationDTO dto = service.getByProject(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评标尚未开始"));
        return ResponseEntity.ok(ApiResponse.success("ok", dto));
    }

    private Long currentUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法识别当前用户");
        }
        return authService.resolveUserIdByUsername(userDetails.getUsername().trim());
    }
}
