// Input: HTTP 请求 (preview/submit closure)
// Output: ApiResponse<ClosurePreviewDTO | ClosureDTO>
// Pos: project/controller/ - WS-F 结项 + 保证金强校验
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.controller;

import com.xiyu.bid.dto.ApiResponse;
import com.xiyu.bid.project.dto.ClosureDTO;
import com.xiyu.bid.project.dto.ClosurePreviewDTO;
import com.xiyu.bid.project.dto.ClosureSubmitRequest;
import com.xiyu.bid.project.service.ProjectClosureService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/projects/{projectId}/closure")
@RequiredArgsConstructor
@Slf4j
public class ProjectClosureController {

    private final ProjectClosureService service;
    private final AuthService authService;

    /** 结项预览：返回保证金快照 + can-close + 阻断原因。 */
    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ClosurePreviewDTO>> preview(@PathVariable Long projectId) {
        ClosurePreviewDTO dto = service.preview(projectId);
        return ResponseEntity.ok(ApiResponse.success("ok", dto));
    }

    /** 提交结项：保证金未退回 → 409；已结项 → 423；缺退回日期/凭证 → 422。 */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClosureDTO>> submit(
            @PathVariable Long projectId,
            @Valid @RequestBody ClosureSubmitRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        // 422：请求自身不合法（声明 depositReturned=true 但日期/凭证缺失）— 早期校验
        if (Boolean.TRUE.equals(req.getDepositReturned())) {
            if (req.getDepositReturnDate() == null || req.getDepositReturnEvidenceId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "声明保证金已退回时，必须同时提供退回日期与退回凭证");
            }
        }
        ClosureDTO dto = service.submitClosure(projectId, req, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project closed", dto));
    }

    private Long currentUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法识别当前用户");
        }
        return authService.resolveUserIdByUsername(userDetails.getUsername().trim());
    }
}
