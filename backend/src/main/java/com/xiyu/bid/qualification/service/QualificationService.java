// Input: DTO、Repository、其他 Service 依赖
// Output: 领域操作结果、事务内状态变更和查询结果
// Pos: Service/业务编排层
// 维护声明: 仅维护本服务职责内的业务规则；跨域变化请同步相关模块.

package com.xiyu.bid.qualification.service;

import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.entity.Qualification;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.repository.QualificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QualificationService {

    private final QualificationRepository qualificationRepository;

    @Transactional
    public QualificationDTO createQualification(QualificationDTO dto) {
        log.info("Creating qualification: {}", dto.getName());
        Qualification qualification = Qualification.builder()
                .name(dto.getName()).type(dto.getType()).level(dto.getLevel())
                .issueDate(dto.getIssueDate()).expiryDate(dto.getExpiryDate()).fileUrl(dto.getFileUrl()).build();
        return toDTO(qualificationRepository.save(qualification));
    }

    @Transactional(readOnly = true)
    public List<QualificationDTO> getAllQualifications() {
        return qualificationRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QualificationDTO getQualificationById(Long id) {
        return toDTO(qualificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Qualification", id.toString())));
    }

    @Transactional
    public QualificationDTO updateQualification(Long id, QualificationDTO dto) {
        log.info("Updating qualification: {}", id);
        Qualification existing = qualificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Qualification", id.toString()));
        Qualification updated = Qualification.builder()
                .id(existing.getId()).name(dto.getName() != null ? dto.getName() : existing.getName())
                .type(dto.getType() != null ? dto.getType() : existing.getType())
                .level(dto.getLevel() != null ? dto.getLevel() : existing.getLevel())
                .issueDate(dto.getIssueDate() != null ? dto.getIssueDate() : existing.getIssueDate())
                .expiryDate(dto.getExpiryDate() != null ? dto.getExpiryDate() : existing.getExpiryDate())
                .fileUrl(dto.getFileUrl() != null ? dto.getFileUrl() : existing.getFileUrl())
                .createdAt(existing.getCreatedAt()).updatedAt(existing.getUpdatedAt()).build();
        return toDTO(qualificationRepository.save(updated));
    }

    @Transactional
    public void deleteQualification(Long id) {
        log.info("Deleting qualification: {}", id);
        if (!qualificationRepository.existsById(id)) throw new ResourceNotFoundException("Qualification", id.toString());
        qualificationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<QualificationDTO> getQualificationsByType(Qualification.Type type) {
        return qualificationRepository.findByType(type, org.springframework.data.domain.PageRequest.of(0, 1000)).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QualificationDTO> getValidQualifications() {
        return qualificationRepository.findByExpiryDateAfter(LocalDate.now(), org.springframework.data.domain.PageRequest.of(0, 1000)).stream().map(this::toDTO).collect(Collectors.toList());
    }

    private QualificationDTO toDTO(Qualification q) {
        return QualificationDTO.builder().id(q.getId()).name(q.getName()).type(q.getType()).level(q.getLevel())
                .issueDate(q.getIssueDate()).expiryDate(q.getExpiryDate()).fileUrl(q.getFileUrl())
                .createdAt(q.getCreatedAt()).updatedAt(q.getUpdatedAt()).build();
    }
}
