# MarketInsight 模块 (超前预测与市场洞察)

> 一旦我所属的文件夹有所变化，请更新我。

## 职责
对标讯历史进行行业趋势分析、采购方规律识别、客户商机研判与机会评分，向前端“市场洞察”弹窗与“客户商机中心”提供纯数据驱动的决策依据。

## 边界清单
| 文件 | 地位 | 功能 |
|------|------|------|
| `controller/MarketInsightController.java` | Controller | 行业趋势 / 采购方规律 / 预测提示聚合接口 |
| `controller/CustomerOpportunityController.java` | Controller | 客户洞察、采购历史、预测商机、刷新、流转、转项目接口 |
| `service/MarketInsightService.java` | Service | 市场洞察编排 |
| `service/CustomerOpportunityService.java` | Service | 客户商机 application shell，对 controller 暴露稳定入口 |
| `service/CustomerOpportunityQueryService.java` | Service | 客户洞察 / 采购记录 / 预测查询 |
| `service/CustomerOpportunityRefreshService.java` | Service | 客户商机刷新编排 |
| `service/CustomerOpportunityStatusCommandService.java` | Service | 状态流转与转项目命令 |
| `core/PurchaserExtractionPolicy.java` | 纯核心 | 从标题提取采购方名称并生成稳定哈希 |
| `core/IndustryClassificationPolicy.java` | 纯核心 | 关键词行业分类与 UI 颜色 |
| `core/TrendAnalysisPolicy.java` | 纯核心 | 趋势计算、热度等级、预测提示 |
| `core/OpportunityScoringPolicy.java` | 纯核心 | 机会评分、窗口预测、周期分类 |
| `core/PredictionTransitionPolicy.java` | 纯核心 | WATCH/RECOMMEND/CONVERTED/CANCELLED 流转校验 |
| `core/CustomerOpportunityRefreshPolicy.java` | 纯核心 | 基于显式参考时间刷新客户商机快照 |
| `core/CustomerOpportunityLifecyclePolicy.java` | 纯核心 | 刷新默认状态与生命周期流转决策 |
| `core/CustomerOpportunityTenderSnapshot.java` | 纯核心 | 刷新/查询共享的标讯快照 |
| `entity/CustomerPrediction.java` | Entity | 客户预测实体，映射 `customer_predictions` 表 |
| `repository/CustomerPredictionRepository.java` | Repository | 客户预测持久化 |
| `gateway/CustomerPredictionGateway.java` | Gateway | 客户预测持久化抽象 |
| `gateway/CustomerPredictionRepositoryGateway.java` | Gateway | Spring Data 持久化适配 |
| `mapper/CustomerOpportunitySnapshotMapper.java` | Mapper | Tender/Prediction 与纯快照/刷新结果互转 |
| `support/CustomerOpportunityTenderSupport.java` | Support | 查询与刷新共享的标讯快照支持 |
| `dto/*` | DTO | 行业趋势 / 采购方规律 / 预测提示 / 客户洞察 / 采购 / 预测 |
| `dto/MarketInsightAssembler.java` | Assembler | 市场洞察 DTO 组装 |
| `dto/CustomerOpportunityAssembler.java` | Assembler | 客户商机 DTO 组装 |

## 数据来源
- 标讯：`com.xiyu.bid.entity.Tender`
- 预测：`customer_predictions`（V58 迁移）

## 外部接口
- `GET /api/market-insight/insight`
- `GET /api/customer-opportunities/insights`
- `GET /api/customer-opportunities/{purchaserHash}/purchases`
- `GET /api/customer-opportunities/{purchaserHash}/predictions`
- `POST /api/customer-opportunities/refresh`
- `PUT /api/customer-opportunities/predictions/{id}/status`
- `PUT /api/customer-opportunities/predictions/{id}/convert`

## 架构边界
- `core/*` 满足 FP-Java Profile：`final class` + `private` 构造 + `static` 方法 + `record` 返回；涉及时间的规则必须显式输入参考时间。
- `service/*` 仅做编排，不承担规则计算；`CustomerOpportunityService` 仅保留 application shell 职责。
