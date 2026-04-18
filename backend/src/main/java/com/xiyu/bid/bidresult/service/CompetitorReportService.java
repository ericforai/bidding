package com.xiyu.bid.bidresult.service;

import com.xiyu.bid.bidresult.core.CompetitorReportComputation;
import com.xiyu.bid.bidresult.core.CompetitorReportRow;
import com.xiyu.bid.bidresult.core.CompetitorWinRow;
import com.xiyu.bid.bidresult.dto.BidResultCompetitorReportRowDTO;
import com.xiyu.bid.bidresult.dto.CompetitorReportAssembler;
import com.xiyu.bid.bidresult.dto.CompetitorWinDTO;
import com.xiyu.bid.bidresult.dto.CompetitorWinRequest;
import com.xiyu.bid.bidresult.entity.CompetitorWinRecord;
import com.xiyu.bid.bidresult.repository.CompetitorWinRecordRepository;
import com.xiyu.bid.competitionintel.entity.Competitor;
import com.xiyu.bid.competitionintel.repository.CompetitorRepository;
import com.xiyu.bid.entity.Project;
import com.xiyu.bid.exception.BusinessException;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompetitorReportService {

    private final CompetitorWinRecordRepository competitorWinRecordRepository;
    private final CompetitorRepository competitorRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public CompetitorWinDTO registerCompetitorWin(CompetitorWinRequest request, Long operatorId, String operatorName) {
        if (request == null || request.getCompetitorId() == null) {
            throw new BusinessException("竞争对手 ID 必填");
        }
        Competitor competitor = competitorRepository.findById(request.getCompetitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Competitor not found: " + request.getCompetitorId()));

        Optional<String> projectName = Optional.ofNullable(request.getProjectId())
                .flatMap(projectRepository::findById)
                .map(Project::getName);

        CompetitorWinRecord entity = CompetitorWinRecord.builder()
                .competitorId(competitor.getId())
                .competitorName(competitor.getName())
                .projectId(request.getProjectId())
                .projectName(projectName.orElse(null))
                .skuCount(Optional.ofNullable(request.getSkuCount()).orElse(0))
                .category(request.getCategory())
                .discount(request.getDiscount())
                .paymentTerms(request.getPaymentTerms())
                .wonAt(Optional.ofNullable(request.getWonAt()).orElse(LocalDate.now()))
                .amount(request.getAmount())
                .notes(request.getNotes())
                .recordedBy(operatorId)
                .recordedByName(operatorName)
                .build();

        return CompetitorReportAssembler.toDTO(competitorWinRecordRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<BidResultCompetitorReportRowDTO> getCompetitorReport() {
        List<CompetitorWinRow> rows = competitorWinRecordRepository.findAllByOrderByWonAtDesc().stream()
                .map(CompetitorReportAssembler::toRow)
                .toList();
        List<CompetitorReportRow> aggregated = CompetitorReportComputation.aggregate(rows);
        return aggregated.stream()
                .map(CompetitorReportAssembler::toReportDTO)
                .toList();
    }
}
