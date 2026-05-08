// Input: HTTP 请求 (assignLeads / advance / get) + 当前用户
// Output: ApiResponse<ProjectDraftingViewDto>
// Pos: project/controller/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.project.dto.ProjectDraftingViewDto;
import com.xiyu.bid.project.dto.ProjectLeadAssignmentRequest;
import com.xiyu.bid.project.service.ProjectDraftingService;
import com.xiyu.bid.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/projects/{projectId}/drafting")
@RequiredArgsConstructor
@Slf4j
public class ProjectDraftingController {

    private final ProjectDraftingService service;
    private final AuthService authService;

    /** PRD §3.2 分配主/副投标负责人：BID_ADMIN（映射到 ADMIN）。 */
    @PatchMapping("/leads")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectDraftingViewDto>> assignLeads(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectLeadAssignmentRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("leads assigned",
                service.assignLeads(projectId, req, userId)));
    }

    /** PRD §3.2.3 闸门：DRAFTING → EVALUATING 推进检查。 */
    @PostMapping("/advance")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ProjectDraftingViewDto>> advance(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("advance ok",
                service.gateAdvanceToEvaluation(projectId, userId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ProjectDraftingViewDto>> get(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success("ok", service.get(projectId)));
    }

    private Long currentUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法识别当前用户");
        }
        return authService.resolveUserIdByUsername(userDetails.getUsername().trim());
    }
}
