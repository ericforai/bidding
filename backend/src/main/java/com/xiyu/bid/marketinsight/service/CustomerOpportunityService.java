// Input: Dedicated query/refresh/command services
// Output: Stable customer opportunity application shell for controllers
// Pos: Service/Application Shell
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.marketinsight.service;

import com.xiyu.bid.marketinsight.dto.CustomerInsightDTO;
import com.xiyu.bid.marketinsight.dto.CustomerPredictionDTO;
import com.xiyu.bid.marketinsight.dto.CustomerPurchaseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户商机应用壳
 * 仅对外暴露稳定入口，并将职责分派给查询、刷新、命令服务
 */
@Service
@RequiredArgsConstructor
public class CustomerOpportunityService {

    private final CustomerOpportunityQueryService queryService;
    private final CustomerOpportunityRefreshService refreshService;
    private final CustomerOpportunityStatusCommandService statusCommandService;

    /**
     * 获取所有客户洞察列表。
     *
     * @return 客户洞察 DTO 列表
     */
    public List<CustomerInsightDTO> getCustomerInsights() {
        return queryService.getCustomerInsights();
    }

    /**
     * 获取指定采购人的采购记录。
     *
     * @param purchaserHash 采购人哈希值
     * @return 客户采购记录 DTO 列表
     */
    public List<CustomerPurchaseDTO> getCustomerPurchases(String purchaserHash) {
        return queryService.getCustomerPurchases(purchaserHash);
    }

    /**
     * 获取指定采购人的商机预测。
     *
     * @param purchaserHash 采购人哈希值
     * @return 客户商机预测 DTO 列表
     */
    public List<CustomerPredictionDTO> getCustomerPredictions(String purchaserHash) {
        return queryService.getCustomerPredictions(purchaserHash);
    }

    /**
     * 重新计算所有客户商机预测。
     * 加载全部标讯 → 提取采购人/行业 → 按采购人分组 → 计算评分/窗口/周期 → 更新或新建预测记录
     */
    public void refreshInsights() {
        refreshService.refreshInsights();
    }

    /**
     * 转换预测状态。
     * 委托 PredictionTransitionPolicy 校验合法性后更新状态。
     *
     * @param id           预测记录 ID
     * @param targetStatus 目标状态名称
     * @return 更新后的预测 DTO
     */
    public CustomerPredictionDTO transitionPrediction(Long id, String targetStatus) {
        return statusCommandService.transitionPrediction(id, targetStatus);
    }

    /**
     * 将预测转化为项目。
     *
     * @param id        预测记录 ID
     * @param projectId 关联项目 ID
     * @return 更新后的预测 DTO
     */
    public CustomerPredictionDTO convertPrediction(Long id, Long projectId) {
        return statusCommandService.convertPrediction(id, projectId);
    }
}
