package com.xiyu.bid.aspect;

import com.xiyu.bid.service.AuditLogService;
import com.xiyu.bid.service.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 审计日志切面
 * 自动记录带有 @Auditable 注解的方法调用
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditableAspect {

    private final IAuditLogService auditLogService;

    @Around("@annotation(com.xiyu.bid.annotation.Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解
        com.xiyu.bid.annotation.Auditable auditable =
            method.getAnnotation(com.xiyu.bid.annotation.Auditable.class);

        // 获取当前用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : "system";
        String username = auth != null && auth.getPrincipal() != null ?
            auth.getPrincipal().toString() : "system";

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        Object result = null;
        boolean success = false;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            // 计算执行时间
            long duration = System.currentTimeMillis() - startTime;

            // 记录审计日志
            if (auditable != null) {
                AuditLogService.AuditLogEntry entry = AuditLogService.AuditLogEntry.builder()
                    .userId(userId)
                    .username(username)
                    .action(auditable.action())
                    .entityType(auditable.entityType())
                    .entityId(extractEntityId(joinPoint.getArgs()))
                    .description(auditable.description().isEmpty() ?
                        method.getName() : auditable.description())
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();

                auditLogService.log(entry);

                log.debug("Audited method call: {} - {} ({}ms)",
                    method.getName(), success ? "SUCCESS" : "FAILED", duration);
            }
        }
    }

    /**
     * 从方法参数中提取实体ID
     */
    private String extractEntityId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        // 简单实现：取第一个参数的toString()
        // 实际项目中可能需要更复杂的逻辑（如从实体对象中获取ID字段）
        for (Object arg : args) {
            if (arg != null) {
                String str = arg.toString();
                if (str.length() <= 100) {
                    return str;
                }
            }
        }
        return null;
    }
}
