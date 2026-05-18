// Input: Tender entity and TenderRepository
// Output: duplicate detection with BusinessException or silent skip
// Pos: Service/标讯去重校验外壳
// 维护声明: 去重规则统一在 TenderDeduplicationPolicy；本类只做查询与异常抛出的编排.

package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.BusinessException;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.tender.core.TenderDeduplicationPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenderDeduplicationService {

    private final TenderRepository tenderRepository;

    /**
     * 去重检查：按项目名称和业主单位查找已有标讯。
     * 如果四字段（项目名称、业主单位、报名截止时间、开标时间）完全匹配视为重复。
     *
     * @throws BusinessException 如果检测到重复标讯
     */
    public void checkDuplicate(Tender tender) {
        var existing = tenderRepository.findByTitleAndPurchaserNameAllIgnoreCase(
                tender.getTitle(), tender.getPurchaserName());
        for (var t : existing) {
            if (TenderDeduplicationPolicy.isDuplicate(
                    tender.getTitle(), tender.getPurchaserName(),
                    tender.getRegistrationDeadline(), tender.getBidOpeningTime(),
                    t.getTitle(), t.getPurchaserName(),
                    t.getRegistrationDeadline(), t.getBidOpeningTime())) {
                throw new BusinessException(TenderDeduplicationPolicy.formatDuplicateMessage(
                        t.getTitle(), t.getPurchaserName(),
                        t.getRegistrationDeadline(), t.getBidOpeningTime()));
            }
        }
    }
}
