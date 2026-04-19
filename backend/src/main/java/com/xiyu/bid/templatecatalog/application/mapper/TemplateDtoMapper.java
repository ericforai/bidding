package com.xiyu.bid.templatecatalog.application.mapper;

import com.xiyu.bid.entity.Template;
import com.xiyu.bid.templatecatalog.application.view.TemplateCatalogView;
import com.xiyu.bid.templatecatalog.domain.valueobject.DocumentType;
import com.xiyu.bid.templatecatalog.domain.valueobject.IndustryType;
import com.xiyu.bid.templatecatalog.domain.valueobject.ProductType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TemplateDtoMapper {

    public TemplateCatalogView toDto(Template template, long downloads, long useCount) {
        return TemplateCatalogView.builder()
                .id(template.getId())
                .name(template.getName())
                .category(template.getCategory())
                .productType(ProductType.fromValue(template.getProductType()))
                .industry(IndustryType.fromValue(template.getIndustry()))
                .documentType(DocumentType.fromValue(template.getDocumentType()))
                .fileUrl(template.getFileUrl())
                .description(template.getDescription())
                .currentVersion(template.getCurrentVersion())
                .fileSize(template.getFileSize())
                .downloads(downloads)
                .useCount(useCount)
                .tags(copyTags(template.getTags()))
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private List<String> copyTags(List<String> tags) {
        return tags == null ? List.of() : List.copyOf(tags);
    }
}
