package com.xiyu.bid.tender.core;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 标讯去重策略（纯核心）。
 * 判断逻辑：项目名称 + 业主单位 + 报名截止时间 + 开标时间 四字段完全匹配。
 * 不依赖任何外部资源。
 */
public final class TenderDeduplicationPolicy {

    private TenderDeduplicationPolicy() { /* utility */ }

    /**
     * 判断两笔标讯是否重复。
     *
     * @param title1     标讯1的项目名称
     * @param purchaser1 标讯1的业主单位
     * @param regDeadline1 标讯1的报名截止时间
     * @param bidOpenTime1 标讯1的开标时间
     * @param title2     标讯2的项目名称
     * @param purchaser2 标讯2的业主单位
     * @param regDeadline2 标讯2的报名截止时间
     * @param bidOpenTime2 标讯2的开标时间
     * @return true 如果四字段完全匹配
     */
    public static boolean isDuplicate(
            String title1, String purchaser1, LocalDateTime regDeadline1, LocalDateTime bidOpenTime1,
            String title2, String purchaser2, LocalDateTime regDeadline2, LocalDateTime bidOpenTime2) {
        return Objects.equals(normalize(title1), normalize(title2))
                && Objects.equals(normalize(purchaser1), normalize(purchaser2))
                && Objects.equals(regDeadline1, regDeadline2)
                && Objects.equals(bidOpenTime1, bidOpenTime2);
    }

    /**
     * 生成去重提示文本。
     */
    public static String formatDuplicateMessage(String title, String purchaser, LocalDateTime regDeadline,
            LocalDateTime bidOpenTime) {
        return String.format("【%s】+【%s】+【%s】+【%s】已存在，请联系投标管理员确认是否覆盖原标讯",
                title, purchaser, regDeadline, bidOpenTime);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
