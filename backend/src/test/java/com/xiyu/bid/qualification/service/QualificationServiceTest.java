package com.xiyu.bid.qualification.service;

import com.xiyu.bid.entity.Qualification;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.qualification.dto.QualificationDTO;
import com.xiyu.bid.repository.QualificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualificationServiceTest {

    @Mock
    private QualificationRepository qualificationRepository;

    @InjectMocks
    private QualificationService qualificationService;

    private Qualification qualification;
    private QualificationDTO qualificationDTO;

    @BeforeEach
    void setUp() {
        qualification = Qualification.builder()
                .id(1L)
                .name("建筑工程施工总承包一级")
                .type(Qualification.Type.CONSTRUCTION)
                .level(Qualification.Level.FIRST)
                .issueDate(LocalDate.now().minusYears(1))
                .expiryDate(LocalDate.now().plusYears(4))
                .build();

        qualificationDTO = QualificationDTO.builder()
                .id(1L)
                .name("建筑工程施工总承包一级")
                .type(Qualification.Type.CONSTRUCTION)
                .level(Qualification.Level.FIRST)
                .issueDate(LocalDate.now().minusYears(1))
                .expiryDate(LocalDate.now().plusYears(4))
                .build();
    }

    @Test
    @DisplayName("创建资质 - 成功")
    void createQualification_ShouldReturnSavedDto() {
        when(qualificationRepository.save(any(Qualification.class))).thenReturn(qualification);

        QualificationDTO result = qualificationService.createQualification(qualificationDTO);

        assertThat(result.getName()).isEqualTo(qualificationDTO.getName());
        verify(qualificationRepository).save(any(Qualification.class));
    }

    @Test
    @DisplayName("获取有效资质 - 成功过滤过期资质")
    void getValidQualifications_ShouldReturnOnlyNonExpired() {
        Qualification expired = Qualification.builder()
                .id(2L)
                .name("已过期的证书")
                .expiryDate(LocalDate.now().minusDays(1))
                .build();

        when(qualificationRepository.findByExpiryDateAfter(any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(qualification)));

        List<QualificationDTO> result = qualificationService.getValidQualifications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("建筑工程施工总承包一级");
        verify(qualificationRepository).findByExpiryDateAfter(any(LocalDate.class), any(Pageable.class));
    }

    @Test
    @DisplayName("根据类型获取资质 - 成功")
    void getQualificationsByType_ShouldReturnFilteredList() {
        when(qualificationRepository.findByType(eq(Qualification.Type.CONSTRUCTION), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(qualification)));

        List<QualificationDTO> result = qualificationService.getQualificationsByType(Qualification.Type.CONSTRUCTION);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(Qualification.Type.CONSTRUCTION);
    }

    @Test
    @DisplayName("更新资质 - 成功合并字段")
    void updateQualification_ShouldUpdateExistingFields() {
        when(qualificationRepository.findById(1L)).thenReturn(Optional.of(qualification));
        when(qualificationRepository.save(any(Qualification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QualificationDTO updateDto = QualificationDTO.builder()
                .name("更新后的名称")
                .build();

        QualificationDTO result = qualificationService.updateQualification(1L, updateDto);

        assertThat(result.getName()).isEqualTo("更新后的名称");
        assertThat(result.getType()).isEqualTo(Qualification.Type.CONSTRUCTION); // 保持原样
    }

    @Test
    @DisplayName("删除资质 - 成功")
    void deleteQualification_ShouldCallRepository() {
        when(qualificationRepository.existsById(1L)).thenReturn(true);

        qualificationService.deleteQualification(1L);

        verify(qualificationRepository).deleteById(1L);
    }

    @Test
    @DisplayName("删除资质 - 不存在抛出异常")
    void deleteQualification_NotFound_ShouldThrowException() {
        when(qualificationRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> qualificationService.deleteQualification(99L));
    }
}
