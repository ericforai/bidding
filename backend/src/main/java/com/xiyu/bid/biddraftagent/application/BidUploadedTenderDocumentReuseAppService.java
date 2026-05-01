package com.xiyu.bid.biddraftagent.application;

import com.xiyu.bid.biddraftagent.domain.TenderRequirementProfile;
import com.xiyu.bid.biddraftagent.dto.BidTenderDocumentDTO;
import com.xiyu.bid.biddraftagent.dto.BidTenderDocumentParseDTO;
import com.xiyu.bid.biddraftagent.entity.BidRequirementItem;
import com.xiyu.bid.biddraftagent.entity.BidTenderDocumentSnapshot;
import com.xiyu.bid.biddraftagent.repository.BidRequirementItemRepository;
import com.xiyu.bid.biddraftagent.repository.BidTenderDocumentSnapshotRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.projectworkflow.entity.ProjectDocument;
import com.xiyu.bid.projectworkflow.repository.ProjectDocumentRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.service.ProjectAccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BidUploadedTenderDocumentReuseAppService {

    private static final String DOCUMENT_CATEGORY = "TENDER_FILE";
    private static final String LINKED_ENTITY_TYPE = "TENDER";
    private static final String REUSE_UPLOADED_SUCCESS_MESSAGE = "已复用项目已上传的招标文件";

    private final ProjectAccessScopeService projectAccessScopeService;
    private final ProjectRepository projectRepository;
    private final TenderRepository tenderRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final BidRequirementItemRepository requirementItemRepository;
    private final BidTenderDocumentSnapshotRepository documentSnapshotRepository;
    private final TenderDocumentStorage documentStorage;
    private final TenderDocumentTextExtractor textExtractor;
    private final TenderDocumentAnalyzer documentAnalyzer;
    private final TenderRequirementSnapshotUpdater snapshotUpdater;
    private final TenderRequirementEntityFactory entityFactory;
    private final BidDraftAgentJsonCodec jsonCodec;
    private final TransactionTemplate transactionTemplate;

    public Optional<BidTenderDocumentParseDTO> parseLatestUploadedTenderDocument(Long projectId) {
        projectAccessScopeService.assertCurrentUserCanAccessProject(projectId);
        Project project = requireProject(projectId);
        Tender tender = requireTender(project.getTenderId());
        return projectDocumentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .filter(document -> isReusableTenderSource(document, tender.getId()))
                .map(document -> parseUploadedDocument(projectId, tender, document))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Optional<BidTenderDocumentParseDTO> parseUploadedDocument(
            Long projectId,
            Tender tender,
            ProjectDocument document
    ) {
        return documentStorage.loadByFileUrl(document.getFileUrl())
                .map(loadedDocument -> parseLoadedDocument(projectId, tender, document, loadedDocument));
    }

    private BidTenderDocumentParseDTO parseLoadedDocument(
            Long projectId,
            Tender tender,
            ProjectDocument document,
            LoadedTenderDocument loadedDocument
    ) {
        String fileName = firstNonBlank(document.getName(), "招标文件");
        String contentType = contentTypeOf(document);
        ExtractedTenderDocument extracted = textExtractor.extract(fileName, contentType, loadedDocument.content());
        TenderRequirementProfile profile = documentAnalyzer.analyze(new TenderDocumentAnalysisInput(
                projectId,
                tender.getId(),
                fileName,
                extracted.text(),
                extracted.structuredMetadata()
        ));
        BidTenderDocumentSnapshot snapshot = persistParsedSnapshot(
                projectId,
                tender,
                document,
                loadedDocument.storedDocument(),
                extracted,
                profile
        );
        return buildResult(document, tender.getId(), snapshot, extracted.textLength(), profile);
    }

    private BidTenderDocumentSnapshot persistParsedSnapshot(
            Long projectId,
            Tender tender,
            ProjectDocument document,
            StoredTenderDocument storedDocument,
            ExtractedTenderDocument extracted,
            TenderRequirementProfile profile
    ) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            String profileJson = jsonCodec.toJson(profile);
            BidTenderDocumentSnapshot snapshot = documentSnapshotRepository.save(entityFactory.buildSnapshot(
                    projectId,
                    tender.getId(),
                    document,
                    storedDocument,
                    extracted,
                    profile,
                    profileJson
            ));
            List<BidRequirementItem> items = entityFactory.buildItems(projectId, tender.getId(), document.getId(), profile);
            requirementItemRepository.saveAll(items);
            snapshotUpdater.apply(tender, profile);
            tenderRepository.save(tender);
            return snapshot;
        }));
    }

    private BidTenderDocumentParseDTO buildResult(
            ProjectDocument document,
            Long tenderId,
            BidTenderDocumentSnapshot snapshot,
            int textLength,
            TenderRequirementProfile profile
    ) {
        return BidTenderDocumentParseDTO.builder()
                .document(BidTenderDocumentDTO.builder()
                        .id(document.getId())
                        .projectId(document.getProjectId())
                        .tenderId(tenderId)
                        .name(firstNonBlank(document.getName(), snapshot.getFileName()))
                        .fileType(firstNonBlank(document.getFileType(), TenderDocumentFileType.toProjectDocumentType(
                                snapshot.getFileName(),
                                snapshot.getContentType()
                        )))
                        .size(document.getSize())
                        .fileUrl(firstNonBlank(document.getFileUrl(), snapshot.getFileUrl()))
                        .snapshotId(snapshot.getId())
                        .extractedTextLength(textLength)
                        .build())
                .requirementProfile(profile)
                .message(REUSE_UPLOADED_SUCCESS_MESSAGE)
                .build();
    }

    private boolean isReusableTenderSource(ProjectDocument document, Long tenderId) {
        if (document == null || document.getFileUrl() == null || document.getFileUrl().isBlank()) {
            return false;
        }
        boolean linkedTender = LINKED_ENTITY_TYPE.equals(trimToNull(document.getLinkedEntityType()))
                && Objects.equals(tenderId, document.getLinkedEntityId());
        boolean categoryTender = DOCUMENT_CATEGORY.equals(trimToNull(document.getDocumentCategory()));
        return linkedTender || categoryTender || nameLooksLikeTender(document.getName());
    }

    private boolean nameLooksLikeTender(String name) {
        String normalized = trimToNull(name);
        if (normalized == null) {
            return false;
        }
        String lowerName = normalized.toLowerCase(Locale.ROOT);
        return lowerName.contains("招标")
                || lowerName.contains("标书")
                || lowerName.contains("tender")
                || lowerName.contains("bid");
    }

    private String contentTypeOf(ProjectDocument document) {
        String fileType = trimToNull(document.getFileType());
        return fileType != null && fileType.contains("/") ? fileType : null;
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
    }

    private Tender requireTender(Long tenderId) {
        return tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender", String.valueOf(tenderId)));
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalized = trimToNull(preferred);
        return normalized != null ? normalized : fallback;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
