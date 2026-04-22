package com.xiyu.bid.biddraftagent.application;

import com.xiyu.bid.biddraftagent.domain.BidDraftSnapshot;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.CaseRepository;
import com.xiyu.bid.repository.ProjectRepository;
import com.xiyu.bid.repository.QualificationRepository;
import com.xiyu.bid.repository.TemplateRepository;
import com.xiyu.bid.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BidDraftSnapshotAssembler {

    private final ProjectRepository projectRepository;
    private final TenderRepository tenderRepository;
    private final QualificationRepository qualificationRepository;
    private final TemplateRepository templateRepository;
    private final CaseRepository caseRepository;

    public BidDraftSnapshot assemble(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", String.valueOf(projectId)));
        Tender tender = tenderRepository.findById(project.getTenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Tender", String.valueOf(project.getTenderId())));

        return new BidDraftSnapshot(
                project.getId(),
                tender.getId(),
                project.getName(),
                project.getDescription(),
                project.getSourceReasoningSummary(),
                project.getCustomer(),
                project.getCustomerType(),
                project.getRegion(),
                project.getIndustry(),
                project.getBudget(),
                project.getDeadline(),
                tender.getTitle(),
                tender.getDescription(),
                tender.getPurchaserName(),
                tender.getSource(),
                tender.getTags() == null ? List.of() : splitValues(tender.getTags()),
                collectQualificationSignals(),
                collectTemplateSignals(),
                collectCaseSignals()
        );
    }

    private List<String> collectQualificationSignals() {
        return qualificationRepository.findAll().stream()
                .limit(8)
                .map(qualification -> qualification.getName() + " / " + qualification.getType() + " / " + qualification.getLevel())
                .toList();
    }

    private List<String> collectTemplateSignals() {
        return templateRepository.findAll().stream()
                .limit(8)
                .map(template -> template.getName() + " / " + template.getCategory() + (template.getDescription() == null ? "" : " / " + template.getDescription()))
                .toList();
    }

    private List<String> collectCaseSignals() {
        return caseRepository.findAll().stream()
                .filter(caseEntity -> caseEntity.getTitle() != null && !caseEntity.getTitle().isBlank())
                .filter(caseEntity -> caseEntity.getPublishedAt() != null || (caseEntity.getStatus() != null && caseEntity.getStatus().equalsIgnoreCase("PUBLISHED")))
                .limit(8)
                .map(caseEntity -> {
                    String highlights = caseEntity.getHighlights() == null || caseEntity.getHighlights().isEmpty()
                            ? ""
                            : " / " + String.join("、", caseEntity.getHighlights());
                    String summary = caseEntity.getArchiveSummary() == null ? "" : " / " + caseEntity.getArchiveSummary();
                    return caseEntity.getTitle() + summary + highlights;
                })
                .toList();
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(","));
    }
}
