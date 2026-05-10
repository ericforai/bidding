package com.xiyu.bid.tender.service;

import com.xiyu.bid.ai.service.AiService;
import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.entity.Task;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.task.dto.TaskDTO;
import com.xiyu.bid.task.service.TaskService;
import com.xiyu.bid.tender.dto.TenderAbandonRequest;
import com.xiyu.bid.tender.dto.TenderBidResponse;
import com.xiyu.bid.tender.dto.TenderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final com.xiyu.bid.batch.core.TenderStatusTransitionPolicy statusTransitionPolicy;
    private final TaskService taskService;

    public TenderDTO createTender(TenderDTO tenderDTO) {
        log.debug("Creating new tender: {}", tenderDTO.getTitle());
        Tender tender = tenderMapper.toEntity(withCommandDefaults(tenderDTO));
        Tender savedTender = tenderRepository.save(tender);
        log.info("Created tender with id: {}", savedTender.getId());
        return tenderMapper.toDTO(savedTender);
    }

    public TenderDTO updateStatus(Long id, Tender.Status targetStatus) {
        log.debug("Updating tender status, id: {}, target: {}", id, targetStatus);
        Tender tender = tenderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        accessGuard.assertCanAccessTender(tender);

        statusTransitionPolicy.assertTransition(tender.getStatus(), targetStatus);

        tender.setStatus(targetStatus);
        Tender updatedTender = tenderRepository.save(tender);
        log.info("Updated tender status, id: {}, status: {}", id, targetStatus);
        return tenderMapper.toDTO(updatedTender);
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
            tenderDTO.setStatus(Tender.Status.PENDING_ASSIGNMENT);
        }
        if (tenderDTO.getSourceType() == null) {
            tenderDTO.setSourceType(Tender.SourceType.MANUAL);
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
        putIfPresent(context, "bidOpeningTime", tender.getBidOpeningTime());
        putIfPresent(context, "source", tender.getSource());
        putIfPresent(context, "region", tender.getRegion());
        putIfPresent(context, "industry", tender.getIndustry());
        putIfPresent(context, "tenderAgency", tender.getTenderAgency());
        putIfPresent(context, "purchaserName", tender.getPurchaserName());
        putIfPresent(context, "customerType", tender.getCustomerType());
        putIfPresent(context, "priority", tender.getPriority());
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

    @Transactional
    public TenderBidResponse participateBid(Long tenderId, Long userId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));
        if (tender.getStatus() == Tender.Status.BIDDED) {
            return TenderBidResponse.builder()
                    .accepted(false)
                    .message("该标讯已投标")
                    .build();
        }
        tender.setStatus(Tender.Status.BIDDED);
        tenderRepository.save(tender);

        TaskDTO todo = TaskDTO.builder()
                .projectId(tenderId)
                .title("【待立项】" + tender.getTitle())
                .description("标讯「" + tender.getTitle() + "」已投标，需进行项目立项。预算：" + tender.getBudget() + "万元。")
                .status(Task.Status.TODO)
                .priority(Task.Priority.HIGH)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();
        TaskDTO createdTodo = taskService.createTask(todo);

        log.info("Tender {} participated, created todo {} for user {}", tenderId, createdTodo.getId(), userId);
        return TenderBidResponse.builder()
                .accepted(true)
                .message("投标成功，已生成项目立项待办")
                .projectId(tenderId)
                .todoId(createdTodo.getId())
                .todoTitle(createdTodo.getTitle())
                .build();
    }

    @Transactional
    public TenderBidResponse abandonBid(Long tenderId, TenderAbandonRequest req) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", tenderId.toString()));
        if (tender.getStatus() == Tender.Status.ABANDONED) {
            return TenderBidResponse.builder()
                    .accepted(false)
                    .message("该标讯已放弃")
                    .build();
        }
        tender.setStatus(Tender.Status.ABANDONED);
        tenderRepository.save(tender);
        log.info("Tender {} abandoned, reason: {}", tenderId, req.getReason());
        return TenderBidResponse.builder()
                .accepted(true)
                .message("已放弃该标讯")
                .build();
    }
}
