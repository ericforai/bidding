package com.xiyu.bid.biddraftagent.infrastructure.openai;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementItemSnapshot;
import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenderRequirementProfileMergerTest {

    @Test
    void merge_shouldCombineChunkProfilesAndDeduplicateSignals() {
        TenderRequirementItemSnapshot sharedItem = new TenderRequirementItemSnapshot(
                "technical",
                "接口能力",
                "支持电子商城接口对接",
                true,
                "接口对接",
                92
        );
        TenderRequirementProfile first = profile(
                "中国兵器电子商城项目",
                List.of("具备独立法人资格"),
                List.of("支持电子商城接口对接"),
                List.of(sharedItem)
        );
        TenderRequirementProfile second = profile(
                null,
                List.of("具备独立法人资格", "提供售后服务承诺"),
                List.of("支持电子商城接口对接", "提供商品上架能力"),
                List.of(sharedItem, new TenderRequirementItemSnapshot(
                        "material",
                        "附件清单",
                        "提供营业执照扫描件",
                        true,
                        "营业执照",
                        88
                ))
        );

        TenderRequirementProfile merged = TenderRequirementProfileMerger.merge(List.of(first, second));

        assertThat(merged.projectName()).isEqualTo("中国兵器电子商城项目");
        assertThat(merged.qualificationRequirements())
                .containsExactly("具备独立法人资格", "提供售后服务承诺");
        assertThat(merged.technicalRequirements())
                .containsExactly("支持电子商城接口对接", "提供商品上架能力");
        assertThat(merged.items()).hasSize(2);
    }

    private TenderRequirementProfile profile(
            String projectName,
            List<String> qualificationRequirements,
            List<String> technicalRequirements,
            List<TenderRequirementItemSnapshot> items
    ) {
        return new TenderRequirementProfile(
                projectName,
                "谈判采购文件",
                "电商供应商引入",
                "中国兵器装备集团有限公司",
                qualificationRequirements,
                technicalRequirements,
                List.of(),
                List.of(),
                "2024-09-24",
                List.of(),
                List.of(),
                List.of(),
                items
        );
    }
}
