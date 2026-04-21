package com.xiyu.bid.tender.service;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.tender.dto.TenderDTO;
import com.xiyu.bid.tender.dto.TenderRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TenderMapper {

    public TenderDTO toDTO(Tender tender) {
        if (tender == null) {
            return null;
        }
        return TenderDTO.builder()
                .id(tender.getId())
                .title(tender.getTitle())
                .source(tender.getSource())
                .budget(tender.getBudget())
                .region(tender.getRegion())
                .industry(tender.getIndustry())
                .purchaserName(tender.getPurchaserName())
                .purchaserHash(tender.getPurchaserHash())
                .publishDate(tender.getPublishDate())
                .deadline(tender.getDeadline())
                .contactName(tender.getContactName())
                .contactPhone(tender.getContactPhone())
                .description(tender.getDescription())
                .tags(decodeTags(tender.getTags()))
                .status(tender.getStatus())
                .aiScore(tender.getAiScore())
                .riskLevel(tender.getRiskLevel())
                .originalUrl(tender.getOriginalUrl())
                .externalId(tender.getExternalId())
                .createdAt(tender.getCreatedAt())
                .updatedAt(tender.getUpdatedAt())
                .build();
    }

    public TenderDTO toDTO(TenderRequest request) {
        if (request == null) {
            return null;
        }
        return TenderDTO.builder()
                .title(request.getTitle())
                .source(request.getSource())
                .budget(request.getBudget())
                .region(request.getRegion())
                .industry(request.getIndustry())
                .purchaserName(request.getPurchaserName())
                .purchaserHash(request.getPurchaserHash())
                .publishDate(request.getPublishDate())
                .deadline(request.getDeadline())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .description(request.getDescription())
                .tags(request.getTags())
                .status(request.getStatus())
                .aiScore(request.getAiScore())
                .riskLevel(request.getRiskLevel())
                .originalUrl(request.getOriginalUrl())
                .externalId(request.getExternalId())
                .build();
    }

    public Tender toEntity(TenderDTO dto) {
        if (dto == null) {
            return null;
        }
        Tender.TenderBuilder builder = Tender.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .source(dto.getSource())
                .budget(dto.getBudget())
                .region(dto.getRegion())
                .industry(dto.getIndustry())
                .purchaserName(dto.getPurchaserName())
                .purchaserHash(dto.getPurchaserHash())
                .publishDate(dto.getPublishDate())
                .deadline(dto.getDeadline())
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .description(dto.getDescription())
                .tags(encodeTags(dto.getTags()))
                .aiScore(dto.getAiScore())
                .riskLevel(dto.getRiskLevel())
                .originalUrl(dto.getOriginalUrl())
                .externalId(dto.getExternalId());
        if (dto.getStatus() != null) {
            builder.status(dto.getStatus());
        }
        return builder.build();
    }

    public void updateEntity(Tender target, TenderDTO dto) {
        if (target == null || dto == null) {
            return;
        }
        if (dto.getTitle() != null) target.setTitle(dto.getTitle());
        if (dto.getSource() != null) target.setSource(dto.getSource());
        if (dto.getBudget() != null) target.setBudget(dto.getBudget());
        if (dto.getRegion() != null) target.setRegion(dto.getRegion());
        if (dto.getIndustry() != null) target.setIndustry(dto.getIndustry());
        if (dto.getPurchaserName() != null) target.setPurchaserName(dto.getPurchaserName());
        if (dto.getPurchaserHash() != null) target.setPurchaserHash(dto.getPurchaserHash());
        if (dto.getPublishDate() != null) target.setPublishDate(dto.getPublishDate());
        if (dto.getDeadline() != null) target.setDeadline(dto.getDeadline());
        if (dto.getContactName() != null) target.setContactName(dto.getContactName());
        if (dto.getContactPhone() != null) target.setContactPhone(dto.getContactPhone());
        if (dto.getDescription() != null) target.setDescription(dto.getDescription());
        if (dto.getTags() != null) target.setTags(encodeTags(dto.getTags()));
        if (dto.getStatus() != null) target.setStatus(dto.getStatus());
        if (dto.getAiScore() != null) target.setAiScore(dto.getAiScore());
        if (dto.getRiskLevel() != null) target.setRiskLevel(dto.getRiskLevel());
        if (dto.getOriginalUrl() != null) target.setOriginalUrl(dto.getOriginalUrl());
        if (dto.getExternalId() != null) target.setExternalId(dto.getExternalId());
    }

    private List<String> decodeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    private String encodeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }
}
