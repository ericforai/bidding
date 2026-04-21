package com.xiyu.bid.contractborrow.controller;

import com.xiyu.bid.contractborrow.application.command.ContractBorrowActionCommand;
import com.xiyu.bid.contractborrow.application.command.ContractBorrowQueryCriteria;
import com.xiyu.bid.contractborrow.application.command.CreateContractBorrowCommand;
import com.xiyu.bid.contractborrow.application.service.ContractBorrowCommandAppService;
import com.xiyu.bid.contractborrow.application.service.ContractBorrowQueryAppService;
import com.xiyu.bid.contractborrow.application.view.ContractBorrowEventView;
import com.xiyu.bid.contractborrow.application.view.ContractBorrowOverviewView;
import com.xiyu.bid.contractborrow.application.view.ContractBorrowView;
import com.xiyu.bid.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contract-borrows")
@RequiredArgsConstructor
public class ContractBorrowController {

    private final ContractBorrowQueryAppService queryService;
    private final ContractBorrowCommandAppService commandService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ContractBorrowOverviewView>> overview() {
        return ResponseEntity.ok(ApiResponse.success(queryService.overview()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ContractBorrowView>>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String borrowerName
    ) {
        ContractBorrowQueryCriteria criteria = new ContractBorrowQueryCriteria(keyword, status, borrowerName);
        return ResponseEntity.ok(ApiResponse.success(queryService.list(criteria)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ContractBorrowView>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(queryService.detail(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ContractBorrowView>> create(@RequestBody CreateContractBorrowRequest request) {
        ContractBorrowView view = commandService.create(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(view));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContractBorrowView>> approve(
        @PathVariable Long id,
        @RequestBody ContractBorrowActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(commandService.approve(id, toCommand(request))));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContractBorrowView>> reject(
        @PathVariable Long id,
        @RequestBody ContractBorrowActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(commandService.reject(id, toCommand(request))));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ContractBorrowView>> returnBack(
        @PathVariable Long id,
        @RequestBody ContractBorrowActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(commandService.returnBack(id, toCommand(request))));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ContractBorrowView>> cancel(
        @PathVariable Long id,
        @RequestBody ContractBorrowActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(commandService.cancel(id, toCommand(request))));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ContractBorrowEventView>>> events(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(queryService.events(id)));
    }

    private CreateContractBorrowCommand toCommand(CreateContractBorrowRequest request) {
        return new CreateContractBorrowCommand(
            request.contractNo(),
            request.contractName(),
            request.sourceName(),
            request.borrowerName(),
            request.borrowerDept(),
            request.customerName(),
            request.purpose(),
            request.borrowType(),
            request.expectedReturnDate()
        );
    }

    private ContractBorrowActionCommand toCommand(ContractBorrowActionRequest request) {
        return new ContractBorrowActionCommand(request.actorName(), request.comment(), request.reason());
    }
}
