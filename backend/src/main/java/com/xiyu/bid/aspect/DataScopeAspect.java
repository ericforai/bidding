package com.xiyu.bid.aspect;

import com.xiyu.bid.admin.service.DataScopeConfigService;
import com.xiyu.bid.annotation.DataScope;
import com.xiyu.bid.entity.CrmCustomerPermission;
import com.xiyu.bid.entity.ProjectMember;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.enums.DataScopeType;
import com.xiyu.bid.repository.CrmCustomerPermissionRepository;
import com.xiyu.bid.repository.ProjectMemberRepository;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.security.DataScopeContext;
import com.xiyu.bid.security.DataScopeContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DataScopeAspect {

    private final UserRepository userRepository;
    private final DataScopeConfigService dataScopeConfigService;
    private final ProjectMemberRepository projectMemberRepository;
    private final CrmCustomerPermissionRepository crmCustomerPermissionRepository;

    @Before("@annotation(com.xiyu.bid.annotation.DataScope)")
    public void doBefore(JoinPoint point) {
        DataScopeContextHolder.clearContext();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DataScope dataScope = method.getAnnotation(DataScope.class);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            DataScopeContextHolder.setContext(DataScopeContext.builder()
                    .scopeType(DataScopeType.ALL)
                    .build());
            return;
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return;
        }

        DataScopeConfigService.AccessProfile profile = dataScopeConfigService.getAccessProfile(user);
        DataScopeType scopeType = parseScopeType(profile.getDataScope());

        List<Long> collaboratedProjectIds = projectMemberRepository.findByUserId(user.getId()).stream()
                .map(ProjectMember::getProjectId)
                .collect(Collectors.toList());

        List<String> crmAuthorizedCustomerIds = crmCustomerPermissionRepository.findByUserId(user.getId()).stream()
                .map(CrmCustomerPermission::getCustomerId)
                .collect(Collectors.toList());

        DataScopeContext context = DataScopeContext.builder()
                .scopeType(scopeType)
                .userAlias(dataScope.userAlias())
                .deptAlias(dataScope.deptAlias())
                .currentUserId(user.getId())
                .allowedDeptCodes(profile.getAllowedDepartmentCodes())
                .explicitProjectIds(profile.getExplicitProjectIds() != null ?
                        new ArrayList<>(profile.getExplicitProjectIds()) : List.of())
                .collaboratedProjectIds(collaboratedProjectIds)
                .crmAuthorizedCustomerIds(crmAuthorizedCustomerIds)
                .build();

        DataScopeContextHolder.setContext(context);
    }

    @After("@annotation(com.xiyu.bid.annotation.DataScope)")
    public void doAfter(JoinPoint point) {
        DataScopeContextHolder.clearContext();
    }

    private DataScopeType parseScopeType(String scope) {
        for (DataScopeType type : DataScopeType.values()) {
            if (type.getCode().equals(scope)) {
                return type;
            }
        }
        return DataScopeType.SELF;
    }
}
