package com.xiyu.bid.dto.ceb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * CEB 平台接口返回根对象
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CebResponse {
    private boolean success;
    private CebObject object;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CebObject {
        private List<CebTenderItem> returnlist;
        private CebPage page;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CebTenderItem {
        private String businessId;          // 唯一标识
        private String tenderProjectCode;   // 项目编号
        private String businessObjectName;  // 项目/公告名称
        private String transactionPlatfName;// 交易平台名称
        private String regionName;          // 地区
        private String industriesType;      // 行业类型
        private String bulletinEndTime;     // 截止日期/发布时间
        private String url;                 // 详情链接 (由于接口不直接返回，我们需要构造)
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CebPage {
        private int pageNo;
        private int totalPage;
        private int totalCount;
    }
}
