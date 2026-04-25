package com.xiyu.bid.tender.service;

import com.xiyu.bid.ai.service.AiService;
import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.tender.dto.TenderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenderCommandService {

    private final TenderRepository tenderRepository;
    private final AiService aiService;
    private final TenderMapper tenderMapper;
    private final TenderProjectAccessGuard accessGuard;

    public TenderDTO createTender(TenderDTO tenderDTO) {
        log.debug("Creating new tender: {}", tenderDTO.getTitle());
        Tender tender = tenderMapper.toEntity(withCommandDefaults(tenderDTO));
        Tender savedTender = tenderRepository.save(tender);
        log.info("Created tender with id: {}", savedTender.getId());
        return tenderMapper.toDTO(savedTender);
    }

    public TenderDTO updateTender(Long id, TenderDTO tenderDTO) {
        log.debug("Updating tender with id: {}", id);
        Tender existingTender = tenderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        accessGuard.assertCanAccessTender(existingTender);
        tenderMapper.updateEntity(existingTender, tenderDTO);
        ensurePurchaserHash(existingTender);
        Tender updatedTender = tenderRepository.save(existingTender);
        log.info("Updated tender with id: {}", id);
        return tenderMapper.toDTO(updatedTender);
    }

    public void deleteTender(Long id) {
        log.debug("Deleting tender with id: {}", id);
        Tender tender = tenderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        accessGuard.assertCanAccessTender(tender);
        tenderRepository.delete(tender);
        log.info("Deleted tender with id: {}", id);
    }

    @Auditable(action = "AI_ANALYZE", entityType = "Tender", description = "AI分析标讯")
    public TenderDTO analyzeTender(Long id) {
        log.debug("Analyzing tender with id: {}", id);
        Tender tender = tenderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        accessGuard.assertCanAccessTender(tender);
        CompletableFuture<Void> analysisFuture = aiService.analyzeTender(id, buildAiContext(tender));
        try {
            analysisFuture.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI analysis wait interrupted for tender id: {}", id, e);
            throw new RuntimeException("AI analysis wait interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Error waiting for AI analysis completion for tender id: {}", id, e);
            throw new RuntimeException("Failed to complete AI analysis", e);
        }
        Tender analyzedTender = tenderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        log.info("Analyzed tender with id: {}, AI Score: {}", id, analyzedTender.getAiScore());
        return tenderMapper.toDTO(analyzedTender);
    }

    private TenderDTO withCommandDefaults(TenderDTO tenderDTO) {
        if (tenderDTO.getStatus() == null) {
            tenderDTO.setStatus(Tender.Status.PENDING);
        }
        if (tenderDTO.getPublishDate() == null) {
            tenderDTO.setPublishDate(LocalDate.now());
        }
        if (!hasText(tenderDTO.getPurchaserHash()) && hasText(tenderDTO.getPurchaserName())) {
            tenderDTO.setPurchaserHash(generatePurchaserHash(tenderDTO.getPurchaserName()));
        }
        return tenderDTO;
    }

    private void ensurePurchaserHash(Tender tender) {
        if (!hasText(tender.getPurchaserHash()) && hasText(tender.getPurchaserName())) {
            tender.setPurchaserHash(generatePurchaserHash(tender.getPurchaserName()));
        }
    }

    private Map<String, Object> buildAiContext(Tender tender) {
        Map<String, Object> context = new LinkedHashMap<>();
        putIfPresent(context, "budget", tender.getBudget());
        putIfPresent(context, "deadline", tender.getDeadline());
        putIfPresent(context, "source", tender.getSource());
        putIfPresent(context, "region", tender.getRegion());
        putIfPresent(context, "industry", tender.getIndustry());
        putIfPresent(context, "purchaserName", tender.getPurchaserName());
        putIfPresent(context, "publishDate", tender.getPublishDate());
        putIfPresent(context, "description", tender.getDescription());
        putIfPresent(context, "tags", tender.getTags());
        return context;
    }

    private void putIfPresent(Map<String, Object> context, String key, Object value) {
        if (value != null) {
            context.put(key, value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String generatePurchaserHash(String purchaserName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(purchaserName.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
