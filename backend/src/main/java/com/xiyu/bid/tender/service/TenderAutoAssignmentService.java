package com.xiyu.bid.tender.service;

import com.xiyu.bid.crm.application.CrmProjectClient;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.crm.domain.AssignmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 标讯自动分配服务
 *
 * <p>职责：在标讯创建后，根据业主单位名称自动匹配 CRM 项目负责人，
 * 实现自动分配。匹配失败时保持 PENDING_ASSIGNMENT 状态，等待手动分配。
 *
 * <p>集成点：
 * <ul>
 *   <li>委托 CrmProjectClient 查询（运行时为 CrmProjectClientStub），消除直接 Repository 访问</li>
 *   <li>记录分配日志（INFO/DEBUG 级别，不影响业务流程）</li>
 * </ul>
 */
@Service
public class TenderAutoAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TenderAutoAssignmentService.class);

    private final CrmProjectClient crmProjectClient;

    public TenderAutoAssignmentService(CrmProjectClient crmProjectClient) {
        this.crmProjectClient = crmProjectClient;
    }

    /**
     * 根据标讯的业主单位名称尝试自动分配。
     *
     * @param tender 标讯实体
     * @return 分配结果，包含是否成功匹配以及负责人信息
     */
    @Transactional(readOnly = true)
    public AssignmentResult tryAutoAssign(Tender tender) {
        if (tender == null || !hasText(tender.getPurchaserName())) {
            log.debug("Skip auto-assignment: tender or purchaserName is null/blank");
            return AssignmentResult.noMatch();
        }

        String purchaserName = tender.getPurchaserName().trim();
        log.debug("Attempting auto-assignment for purchaser: {}", purchaserName);

        // 委托给 CrmProjectClient，消除与 CrmProjectClientStub 的重复查询逻辑
        AssignmentResult result = crmProjectClient.findProjectByPurchaser(purchaserName);

        if (result.isMatched()) {
            log.info("Auto-assignment matched for tender {} (purchaser: {}): manager={}, dept={}",
                    tender.getId(), purchaserName, result.projectManagerName(), result.departmentName());
        } else {
            log.debug("No CRM mapping for purchaser: {}", purchaserName);
        }

        return result;
    }

    /**
     * 根据标讯创建后自动尝试分配。
     *
     * <p>此方法应在标讯保存后调用。
     * 分配成功后更新标讯状态为 TRACKING。
     *
     * @param tender 标讯实体（已保存，带 ID）
     * @return true 如果成功匹配并分配，false 如果保持 PENDING_ASSIGNMENT
     */
    @Transactional
    public boolean autoAssignIfPossible(Tender tender) {
        if (tender == null) {
            log.warn("Auto-assignment skipped: tender is null");
            return false;
        }

        AssignmentResult result = tryAutoAssign(tender);

        if (result.isMatched()) {
            // 状态转换检查由调用方 TenderCommandService 处理
            // 此处仅记录分配结果
            log.info("Tender {} auto-assigned to manager {} ({})",
                    tender.getId(), result.projectManagerName(), result.projectManagerId());
            return true;
        }

        log.debug("Tender {} remains PENDING_ASSIGNMENT (no CRM mapping)", tender.getId());
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
