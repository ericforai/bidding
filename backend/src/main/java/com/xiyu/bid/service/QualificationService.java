// Input: Repository, 相关依赖
// Output: 业务服务、数据操作
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.xiyu.bid.service;

import com.xiyu.bid.dto.QualificationDTO;
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

/**
 * 资质管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualificationService {

    private final QualificationRepository qualificationRepository;

    /**
     * 创建资质
     */
    @Transactional
    public QualificationDTO createQualification(QualificationDTO dto) {
        log.info("Creating qualification: {}", dto.getName());

        Qualification qualification = Qualification.builder()
                .name(dto.getName())
                .type(dto.getType())
                .level(dto.getLevel())
                .issueDate(dto.getIssueDate())
                .expiryDate(dto.getExpiryDate())
                .fileUrl(dto.getFileUrl())
                .build();

        Qualification saved = qualificationRepository.save(qualification);
        log.info("Qualification created successfully with id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * 获取所有资质（限制返回1000条）
     */
    public List<QualificationDTO> getAllQualifications() {
        log.debug("Fetching all qualifications");
        return qualificationRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取资质
     */
    public QualificationDTO getQualificationById(Long id) {
        log.debug("Fetching qualification by id: {}", id);
        Qualification qualification = qualificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Qualification", id.toString()));
        return toDTO(qualification);
    }

    /**
     * 更新资质
     */
    @Transactional
    public QualificationDTO updateQualification(Long id, QualificationDTO dto) {
        log.info("Updating qualification: {}", id);

        Qualification existing = qualificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Qualification", id.toString()));

        // 使用Builder模式创建新对象而非直接修改
        Qualification updated = Qualification.builder()
                .id(existing.getId())
                .name(dto.getName() != null ? dto.getName() : existing.getName())
                .type(dto.getType() != null ? dto.getType() : existing.getType())
                .level(dto.getLevel() != null ? dto.getLevel() : existing.getLevel())
                .issueDate(dto.getIssueDate() != null ? dto.getIssueDate() : existing.getIssueDate())
                .expiryDate(dto.getExpiryDate() != null ? dto.getExpiryDate() : existing.getExpiryDate())
                .fileUrl(dto.getFileUrl() != null ? dto.getFileUrl() : existing.getFileUrl())
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();

        updated = qualificationRepository.save(updated);
        log.info("Qualification updated successfully: {}", id);

        return toDTO(updated);
    }

    /**
     * 删除资质
     */
    @Transactional
    public void deleteQualification(Long id) {
        log.info("Deleting qualification: {}", id);

        if (!qualificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Qualification", id.toString());
        }

        qualificationRepository.deleteById(id);
        log.info("Qualification deleted successfully: {}", id);
    }

    /**
     * 根据类型查找资质（限制返回1000条）
     */
    public List<QualificationDTO> getQualificationsByType(Qualification.Type type) {
        log.debug("Fetching qualifications by type: {}", type);
        return qualificationRepository.findByType(type, org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查找未过期的资质（限制返回1000条）
     */
    public List<QualificationDTO> getValidQualifications() {
        log.debug("Fetching valid qualifications");
        return qualificationRepository.findByExpiryDateAfter(LocalDate.now(), org.springframework.data.domain.PageRequest.of(0, 1000)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private QualificationDTO toDTO(Qualification qualification) {
        return QualificationDTO.builder()
                .id(qualification.getId())
                .name(qualification.getName())
                .type(qualification.getType())
                .level(qualification.getLevel())
                .issueDate(qualification.getIssueDate())
                .expiryDate(qualification.getExpiryDate())
                .fileUrl(qualification.getFileUrl())
                .createdAt(qualification.getCreatedAt())
                .updatedAt(qualification.getUpdatedAt())
                .build();
    }
}
