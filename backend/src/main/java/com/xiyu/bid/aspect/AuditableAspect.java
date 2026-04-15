// Input: @Auditable 切点、审计上下文、IAuditLogService
// Output: 审计日志记录与环绕增强
// Pos: Aspect/审计切面层
// 维护声明: 仅维护横切审计逻辑；不要在此层承载业务规则.
package com.xiyu.bid.aspect;

import com.xiyu.bid.audit.service.AuditLogService;
import com.xiyu.bid.audit.service.IAuditLogService;
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
import java.lang.reflect.Modifier;

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
        } catch (RuntimeException e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } catch (Error e) {
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
                    .entityId(extractEntityId(joinPoint.getArgs(), result))
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
    private String extractEntityId(Object[] args, Object result) {
        String resultId = extractIdFromObject(result);
        if (resultId != null) {
            return resultId;
        }

        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            String argId = extractIdFromObject(arg);
            if (argId != null) {
                return argId;
            }
            if (arg instanceof Number || arg instanceof CharSequence) {
                String str = arg.toString();
                if (str.length() <= 100) {
                    return str;
                }
            }
        }
        return null;
    }

    private String extractIdFromObject(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number || value instanceof CharSequence) {
            String str = value.toString();
            return str.length() <= 100 ? str : null;
        }

        try {
            Method getId = value.getClass().getMethod("getId");
            if (Modifier.isPublic(getId.getModifiers()) && getId.getParameterCount() == 0) {
                Object id = getId.invoke(value);
                if (id != null) {
                    String str = id.toString();
                    return str.length() <= 100 ? str : null;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to no ID extracted.
        }

        return null;
    }
}
