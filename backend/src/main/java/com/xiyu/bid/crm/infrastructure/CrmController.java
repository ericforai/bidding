package com.xiyu.bid.crm.infrastructure;

import com.xiyu.bid.crm.application.CrmAuthService;
import com.xiyu.bid.crm.application.CrmCustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/xiyu/crm")
public class CrmController {

    private final CrmCustomerService customerService;
    private final CrmAuthService authService;

    public CrmController(CrmCustomerService customerService, CrmAuthService authService) {
        this.customerService = customerService;
        this.authService = authService;
    }

    @GetMapping("/customers")
    public ResponseEntity<?> searchCustomers(@RequestParam String keyword, @RequestParam(defaultValue = "20") int pageSize) {
        var response = customerService.searchCustomers(keyword, pageSize);
        return ResponseEntity.ok(Map.of("success", response.success(), "data", response.data()));
    }

    @GetMapping("/customers/{customerId}/contacts")
    public ResponseEntity<?> getContacts(@PathVariable String customerId) {
        var response = customerService.getCustomerContacts(List.of(customerId));
        return ResponseEntity.ok(Map.of("success", response.success(), "data", response.data()));
    }

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessages(@RequestBody Map<String, Object> body) {
        // placeholder: will route through CrmMessageService
        return ResponseEntity.accepted().body(Map.of("result", "accepted", "note", "message routing pending"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        authService.logout();
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}
