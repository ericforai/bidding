package com.xiyu.bid.crm.application;

import com.xiyu.bid.crm.infrastructure.CrmHttpClient;
import com.xiyu.bid.crm.infrastructure.CrmResponseHandler;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CrmEmployeeService {

    private final CrmHttpClient httpClient;
    private final CrmAuthService authService;

    public CrmEmployeeService(CrmHttpClient httpClient, CrmAuthService authService) {
        this.httpClient = httpClient;
        this.authService = authService;
    }

    public CrmResponseHandler.CrmApiResponse getEmployeeByToken(String employeeToken) {
        String token = authService.getValidToken();
        return httpClient.post("/employee/info", token,
                Map.of("token", employeeToken));
    }
}
