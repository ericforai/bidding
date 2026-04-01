// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.tender.service;

import com.xiyu.bid.annotation.Auditable;
import com.xiyu.bid.ai.service.AiService;
import com.xiyu.bid.tender.dto.TenderDTO;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 标讯服务层
 * 提供标讯管理的业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TenderService {

    private final TenderRepository tenderRepository;
    private final AiService aiService;

    @Transactional(readOnly = true)
    public List<TenderDTO> getAllTenders() {
        log.debug("Fetching all tenders");
        return tenderRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenderDTO getTenderById(Long id) {
        log.debug("Fetching tender by id: {}", id);
        Tender tender = tenderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        return convertToDTO(tender);
    }

    public TenderDTO createTender(TenderDTO tenderDTO) {
        log.debug("Creating new tender: {}", tenderDTO.getTitle());
        Tender tender = convertToEntity(tenderDTO);
        Tender savedTender = tenderRepository.save(tender);
        log.info("Created tender with id: {}", savedTender.getId());
        return convertToDTO(savedTender);
    }

    public TenderDTO updateTender(Long id, TenderDTO tenderDTO) {
        log.debug("Updating tender with id: {}", id);
        Tender existingTender = tenderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        if (tenderDTO.getTitle() != null) existingTender.setTitle(tenderDTO.getTitle());
        if (tenderDTO.getSource() != null) existingTender.setSource(tenderDTO.getSource());
        if (tenderDTO.getBudget() != null) existingTender.setBudget(tenderDTO.getBudget());
        if (tenderDTO.getDeadline() != null) existingTender.setDeadline(tenderDTO.getDeadline());
        if (tenderDTO.getStatus() != null) existingTender.setStatus(tenderDTO.getStatus());
        if (tenderDTO.getAiScore() != null) existingTender.setAiScore(tenderDTO.getAiScore());
        if (tenderDTO.getRiskLevel() != null) existingTender.setRiskLevel(tenderDTO.getRiskLevel());
        Tender updatedTender = tenderRepository.save(existingTender);
        log.info("Updated tender with id: {}", id);
        return convertToDTO(updatedTender);
    }

    public void deleteTender(Long id) {
        log.debug("Deleting tender with id: {}", id);
        Tender tender = tenderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        tenderRepository.delete(tender);
        log.info("Deleted tender with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<TenderDTO> getTendersByStatus(Tender.Status status) {
        log.debug("Fetching tenders by status: {}", status);
        return tenderRepository.findByStatus(status).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TenderDTO> getTendersBySource(String source) {
        log.debug("Fetching tenders by source: {}", source);
        return tenderRepository.findBySource(source).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Auditable(action = "AI_ANALYZE", entityType = "Tender", description = "AI分析标讯")
    public TenderDTO analyzeTender(Long id) {
        log.debug("Analyzing tender with id: {}", id);
        Tender tender = tenderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        CompletableFuture<Void> analysisFuture = aiService.analyzeTender(id, Map.of("budget", tender.getBudget(), "deadline", tender.getDeadline(), "source", tender.getSource()));
        try { analysisFuture.get(30, TimeUnit.SECONDS); } catch (Exception e) { log.error("Error waiting for AI analysis completion for tender id: {}", id, e); throw new RuntimeException("Failed to complete AI analysis", e); }
        Tender analyzedTender = tenderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tender", id.toString()));
        log.info("Analyzed tender with id: {}, AI Score: {}", id, analyzedTender.getAiScore());
        return convertToDTO(analyzedTender);
    }

    @Transactional(readOnly = true)
    public Map<Tender.Status, Long> getTenderStatistics() {
        log.debug("Fetching tender statistics");
        return Map.of(Tender.Status.PENDING, tenderRepository.countByStatus(Tender.Status.PENDING), Tender.Status.TRACKING, tenderRepository.countByStatus(Tender.Status.TRACKING), Tender.Status.BIDDED, tenderRepository.countByStatus(Tender.Status.BIDDED), Tender.Status.ABANDONED, tenderRepository.countByStatus(Tender.Status.ABANDONED));
    }

    private TenderDTO convertToDTO(Tender tender) {
        return TenderDTO.builder().id(tender.getId()).title(tender.getTitle()).source(tender.getSource()).budget(tender.getBudget()).deadline(tender.getDeadline()).status(tender.getStatus()).aiScore(tender.getAiScore()).riskLevel(tender.getRiskLevel()).createdAt(tender.getCreatedAt()).updatedAt(tender.getUpdatedAt()).build();
    }

    private Tender convertToEntity(TenderDTO dto) {
        return Tender.builder().id(dto.getId()).title(dto.getTitle()).source(dto.getSource()).budget(dto.getBudget()).deadline(dto.getDeadline()).status(dto.getStatus() != null ? dto.getStatus() : Tender.Status.PENDING).aiScore(dto.getAiScore()).riskLevel(dto.getRiskLevel()).build();
    }
}
