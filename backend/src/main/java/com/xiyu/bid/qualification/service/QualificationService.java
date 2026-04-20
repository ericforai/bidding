// Input: compatibility DTOs, businessqualification application services, and DTO mapper
// Output: legacy qualification API orchestration backed by the real businessqualification domain
// Pos: Service/业务编排层
// 维护声明: 仅维护兼容入口编排；业务规则下沉到 businessqualification 子域。
package com.xiyu.bid.qualification.service;

import com.xiyu.bid.businessqualification.application.service.BorrowQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.CreateQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.DeleteQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.GetQualificationBorrowRecordsAppService;
import com.xiyu.bid.businessqualification.application.service.ListQualificationsAppService;
import com.xiyu.bid.businessqualification.application.service.ReturnQualificationAppService;
import com.xiyu.bid.businessqualification.application.service.ScanExpiringQualificationsAppService;
import com.xiyu.bid.businessqualification.application.service.UpdateQualificationAppService;
import com.xiyu.bid.businessqualification.domain.model.BusinessQualification;
import com.xiyu.bid.businessqualification.domain.model.QualificationLoan;
import com.xiyu.bid.qualification.dto.QualificationBorrowRecordDTO;
import com.xiyu.bid.qualification.dto.QualificationBorrowRequest;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.qualification.dto.QualificationOverviewDTO;
import com.xiyu.bid.qualification.dto.QualificationReturnRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QualificationService {

    private final CreateQualificationAppService createQualificationAppService;
    private final UpdateQualificationAppService updateQualificationAppService;
    private final BorrowQualificationAppService borrowQualificationAppService;
    private final ReturnQualificationAppService returnQualificationAppService;
    private final ListQualificationsAppService listQualificationsAppService;
    private final GetQualificationBorrowRecordsAppService getQualificationBorrowRecordsAppService;
    private final ScanExpiringQualificationsAppService scanExpiringQualificationsAppService;
    private final DeleteQualificationAppService deleteQualificationAppService;
    private final QualificationDtoMapper mapper;

    public QualificationDTO createQualification(QualificationDTO dto) {
        return mapper.toDto(createQualificationAppService.create(mapper.toUpsertCommand(dto)));
    }

    @Transactional(readOnly = true)
    public List<QualificationDTO> getAllQualifications(
            String subjectType,
            String subjectName,
            String category,
            String status,
            String borrowStatus,
            Integer expiringWithinDays,
            String keyword
    ) {
        return listQualificationsAppService.list(
                        mapper.toCriteria(subjectType, subjectName, category, status, borrowStatus, expiringWithinDays, keyword))
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public QualificationDTO getQualificationById(Long id) {
        return mapper.toDto(findQualification(id));
    }

    public QualificationDTO updateQualification(Long id, QualificationDTO dto) {
        return mapper.toDto(updateQualificationAppService.update(id, mapper.toUpsertCommand(dto)));
    }

    public void deleteQualification(Long id) {
        deleteQualificationAppService.delete(id);
    }

    @Transactional(readOnly = true)
    public List<QualificationDTO> getQualificationsByType(com.xiyu.bid.entity.Qualification.Type type) {
        return getAllQualifications(null, null, type == null ? null : mapper.toUpsertCommand(QualificationDTO.builder().type(type).build()).getCategory().name(), null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<QualificationDTO> getValidQualifications() {
        return getAllQualifications(null, null, null, null, null, null, null).stream()
                .filter(item -> !"expired".equals(item.getStatus()))
                .toList();
    }

    public QualificationBorrowRecordDTO borrowQualification(Long id, QualificationBorrowRequest request) {
        QualificationLoan loan = borrowQualificationAppService.borrow(id, mapper.toBorrowCommand(request));
        return mapper.toBorrowRecordDto(loan, findQualification(id));
    }

    public QualificationBorrowRecordDTO returnQualification(Long id, QualificationReturnRequest request) {
        QualificationLoan loan = returnQualificationAppService.returnLoan(id, mapper.toReturnCommand(request));
        return mapper.toBorrowRecordDto(loan, findQualification(id));
    }

    public QualificationBorrowRecordDTO returnQualificationByRecordId(Long recordId, QualificationReturnRequest request) {
        QualificationLoan loan = returnQualificationAppService.returnLoanByRecordId(recordId, mapper.toReturnCommand(request));
        return mapper.toBorrowRecordDto(loan, findQualification(loan.getQualificationId()));
    }

    @Transactional(readOnly = true)
    public List<QualificationBorrowRecordDTO> getBorrowRecords(Long id) {
        BusinessQualification qualification = findQualification(id);
        return getQualificationBorrowRecordsAppService.getBorrowRecords(id).stream()
                .map(item -> mapper.toBorrowRecordDto(item, qualification))
                .toList();
    }

    @Transactional(readOnly = true)
    public QualificationOverviewDTO getOverview() {
        return mapper.toOverview(getAllQualifications(null, null, null, null, null, null, null));
    }

    public int scanExpiringQualifications(int thresholdDays) {
        return scanExpiringQualificationsAppService.scan(thresholdDays).size();
    }

    private BusinessQualification findQualification(Long id) {
        return listQualificationsAppService.get(id);
    }
}
