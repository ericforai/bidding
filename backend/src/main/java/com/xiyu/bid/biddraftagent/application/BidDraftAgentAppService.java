package com.xiyu.bid.biddraftagent.application;

import com.xiyu.bid.biddraftagent.domain.BidDraftSnapshot;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentApplyResponseDTO;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentReviewDTO;
import com.xiyu.bid.biddraftagent.dto.BidDraftAgentRunDTO;
import com.xiyu.bid.biddraftagent.entity.BidAgentArtifact;
import com.xiyu.bid.biddraftagent.entity.BidAgentRun;
import com.xiyu.bid.biddraftagent.repository.BidAgentArtifactRepository;
import com.xiyu.bid.biddraftagent.repository.BidAgentRunRepository;
import com.xiyu.bid.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BidDraftAgentAppService {

    private final BidDraftSnapshotAssembler snapshotAssembler;
    private final BidDraftAgentEvaluator evaluator;
    private final BidDraftTextGenerator textGenerator;
    private final BidDraftAgentEntityFactory entityFactory;
    private final BidDraftAgentRunMapper runMapper;
    private final BidDraftAgentJsonCodec jsonCodec;
    private final BidAgentRunRepository runRepository;
    private final BidAgentArtifactRepository artifactRepository;

    public BidDraftAgentRunDTO createRun(Long projectId) {
        BidDraftSnapshot snapshot = snapshotAssembler.assemble(projectId);
        BidDraftAgentEvaluation evaluation = evaluator.evaluate(snapshot);
        BidDraftGenerationResult generation = generate(snapshot, evaluation);

        BidAgentRun savedRun = runRepository.save(entityFactory.buildRun(snapshot, evaluation, generation));
        List<BidAgentArtifact> savedArtifacts = artifactRepository.saveAll(entityFactory.buildArtifacts(savedRun, generation));
        return runMapper.toRunDTO(savedRun, savedArtifacts);
    }

    @Transactional(readOnly = true)
    public BidDraftAgentRunDTO getRun(Long projectId, Long runId) {
        BidAgentRun run = requireRun(projectId, runId);
        List<BidAgentArtifact> artifacts = artifactRepository.findByRunIdOrderByCreatedAtAsc(runId);
        return runMapper.toRunDTO(run, artifacts);
    }

    public BidDraftAgentReviewDTO reviewCurrentDraft(Long projectId) {
        BidAgentRun run = runRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("BidAgentRun", String.valueOf(projectId)));
        BidDraftSnapshot snapshot = jsonCodec.fromJson(run.getSnapshotJson(), BidDraftSnapshot.class);
        BidDraftAgentEvaluation evaluation = evaluator.evaluate(snapshot);
        BidDraftGenerationResult generation = generate(snapshot, evaluation);

        run.setReviewText(generation.reviewSummary());
        run.setReviewedAt(LocalDateTime.now());
        run.setStatus(BidAgentRun.Status.REVIEWED);
        entityFactory.updateEvaluationJson(run, evaluation);
        BidAgentRun savedRun = runRepository.save(run);

        updateReviewArtifact(savedRun.getId(), generation.reviewSummary());
        return runMapper.toReviewDTO(savedRun, evaluation, generation);
    }

    public BidDraftAgentApplyResponseDTO applyRun(Long projectId, Long runId) {
        BidAgentRun run = requireRun(projectId, runId);
        List<BidAgentArtifact> artifacts = artifactRepository.findByRunIdOrderByCreatedAtAsc(runId);
        if (artifacts.isEmpty()) {
            throw new IllegalStateException("当前运行没有可应用的产物");
        }

        BidAgentArtifact primaryArtifact = artifacts.stream()
                .filter(artifact -> "DRAFT_TEXT".equalsIgnoreCase(artifact.getArtifactType()))
                .findFirst()
                .orElse(artifacts.get(0));

        primaryArtifact.setStatus(BidAgentArtifact.Status.READY_FOR_WRITER);
        primaryArtifact.setAppliedAt(LocalDateTime.now());
        artifactRepository.save(primaryArtifact);

        run.setStatus(BidAgentRun.Status.READY_FOR_WRITER);
        run.setAppliedAt(LocalDateTime.now());
        runRepository.save(run);

        return BidDraftAgentApplyResponseDTO.builder()
                .runId(run.getId())
                .artifactId(primaryArtifact.getId())
                .artifactType(primaryArtifact.getArtifactType())
                .status(primaryArtifact.getStatus().name())
                .readyForWriter(true)
                .handoffTarget(primaryArtifact.getHandoffTarget())
                .message("草稿产物已标记为文档写手可用")
                .build();
    }

    private BidDraftGenerationResult generate(
            BidDraftSnapshot snapshot,
            BidDraftAgentEvaluation evaluation
    ) {
        return textGenerator.generate(
                snapshot,
                evaluation.requirementClassification(),
                evaluation.materialMatchScore(),
                evaluation.gapCheck(),
                evaluation.manualConfirmation(),
                evaluation.writeCoverage()
        );
    }

    private BidAgentRun requireRun(Long projectId, Long runId) {
        return runRepository.findByIdAndProjectId(runId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("BidAgentRun", String.valueOf(runId)));
    }

    private void updateReviewArtifact(Long runId, String reviewSummary) {
        artifactRepository.findByRunIdAndArtifactType(runId, "REVIEW_SUMMARY")
                .ifPresent(artifact -> {
                    artifact.setContent(reviewSummary);
                    artifactRepository.save(artifact);
                });
    }
}
