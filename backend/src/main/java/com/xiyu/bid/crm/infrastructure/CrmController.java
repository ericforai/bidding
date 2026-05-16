package com.xiyu.bid.crm.infrastructure;

import com.xiyu.bid.crm.application.CrmAuthService;
import com.xiyu.bid.crm.application.CrmCustomerService;
import com.xiyu.bid.crm.application.CrmEmployeeService;
import com.xiyu.bid.crm.application.CrmMenuService;
import com.xiyu.bid.crm.application.CrmMessageService;
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
    private final CrmMenuService menuService;
    private final CrmEmployeeService employeeService;
    private final CrmMessageService messageService;

    public CrmController(CrmCustomerService customerService, CrmAuthService authService,
                         CrmMenuService menuService, CrmEmployeeService employeeService,
                         CrmMessageService messageService) {
        this.customerService = customerService;
        this.authService = authService;
        this.menuService = menuService;
        this.employeeService = employeeService;
        this.messageService = messageService;
    }

    @GetMapping("/customers")
    public ResponseEntity<?> searchCustomers(@RequestParam String keyword,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        var response = customerService.searchCustomers(keyword, pageSize);
        return ResponseEntity.ok(Map.of("success", response.success(), "data", response.data()));
    }

    @GetMapping("/customers/{customerId}/contacts")
    public ResponseEntity<?> getContacts(@PathVariable String customerId) {
        var response = customerService.getCustomerContacts(List.of(customerId));
        return ResponseEntity.ok(Map.of("success", response.success(), "data", response.data()));
    }

    @GetMapping("/menus")
    public ResponseEntity<?> getMenuTree(@RequestParam String systemType) {
        var response = menuService.getMenuTree(systemType);
        return ResponseEntity.ok(Map.of("success", response.success(), "data", response.data()));
    }

    @GetMapping("/employees/{token}")
    public ResponseEntity<?> getEmployee(@PathVariable String token) {
        var response = employeeService.getEmployeeByToken(token);
        return ResponseEntity.ok(Map.of("success", response.success(), "data", response.data()));
    }

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessages(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        var messages = (List<Map<String, Object>>) body.get("messages");
        var response = messageService.sendMessages(messages);
        return ResponseEntity.ok(Map.of("success", response.success()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        authService.logout();
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}
