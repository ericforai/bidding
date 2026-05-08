// Input: ProjectInitiationService 行为
// Output: Mockito 单元测试覆盖 submit / update locked / 422 / 423 / 404
// Pos: backend test source
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.project.service;

import com.xiyu.bid.entity.Project;
import com.xiyu.bid.entity.ProjectInitiationDetails;
import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.project.core.InitiationFieldPolicy;
import com.xiyu.bid.project.dto.InitiationDto;
import com.xiyu.bid.project.repository.ProjectInitiationDetailsRepository;
import com.xiyu.bid.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectInitiationServiceTest {

    @Mock ProjectInitiationDetailsRepository repo;
    @Mock ProjectRepository projectRepository;

    ProjectInitiationService service;

    @BeforeEach
    void setUp() {
        service = new ProjectInitiationService(repo, projectRepository);
        lenient().when(projectRepository.findById(1L)).thenReturn(Optional.of(Project.builder().id(1L).build()));
        lenient().when(repo.save(any(ProjectInitiationDetails.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private InitiationDto fullDto() {
        return InitiationDto.builder()
                .ownerUnit("国网")
                .expectedBidders(3).contractPeriodMonths(12)
                .projectType(InitiationFieldPolicy.ProjectType.PUBLIC_BIDDING)
                .customerType(InitiationFieldPolicy.CustomerType.CENTRAL_SOE)
                .annualRevenue(new BigDecimal("100000"))
                .bidOpenTime(LocalDateTime.of(2026, 6, 1, 9, 30))
                .ownerUserId(42L).departmentSnapshot("投标部")
                .depositAmount(new BigDecimal("50000")).depositPaymentMethod("银行汇票")
                .build();
    }

    @Test
    void submit_ok_locksAndDerivesMonth() {
        when(repo.findByProjectId(1L)).thenReturn(Optional.empty());
        var view = service.submit(1L, fullDto(), 99L);
        assertThat(view.getLocked()).isTrue();
        assertThat(view.getBidMonth()).isEqualTo("2026-06");
        var captor = ArgumentCaptor.forClass(ProjectInitiationDetails.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getLocked()).isTrue();
        assertThat(captor.getValue().getProjectType()).isEqualTo("PUBLIC_BIDDING");
    }

    @Test
    void submit_missingFields_throws422() {
        var bad = fullDto();
        bad.setOwnerUnit(null);
        lenient().when(repo.findByProjectId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(1L, bad, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void submit_unknownProject_throws404() {
        when(projectRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(2L, fullDto(), 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_changingLockedField_throws423() {
        ProjectInitiationDetails existing = ProjectInitiationDetails.builder()
                .id(10L).projectId(1L)
                .ownerUnit("国网").expectedBidders(3).contractPeriodMonths(12)
                .projectType("PUBLIC_BIDDING").customerType("CENTRAL_SOE")
                .annualRevenue(new BigDecimal("100000"))
                .bidOpenTime(LocalDateTime.of(2026, 6, 1, 9, 30))
                .ownerUserId(42L).departmentSnapshot("投标部")
                .depositAmount(new BigDecimal("50000")).depositPaymentMethod("银行汇票")
                .locked(Boolean.TRUE).build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var patch = InitiationDto.builder()
                .bidOpenTime(LocalDateTime.of(2027, 1, 1, 9, 0)).build();
        assertThatThrownBy(() -> service.update(1L, patch, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void update_nonLockedField_ok() {
        ProjectInitiationDetails existing = ProjectInitiationDetails.builder()
                .id(10L).projectId(1L)
                .ownerUnit("国网").expectedBidders(3).contractPeriodMonths(12)
                .projectType("PUBLIC_BIDDING").customerType("CENTRAL_SOE")
                .annualRevenue(new BigDecimal("100000"))
                .bidOpenTime(LocalDateTime.of(2026, 6, 1, 9, 30))
                .ownerUserId(42L).departmentSnapshot("投标部")
                .depositAmount(new BigDecimal("50000")).depositPaymentMethod("银行汇票")
                .locked(Boolean.TRUE).build();
        when(repo.findByProjectId(1L)).thenReturn(Optional.of(existing));
        var patch = InitiationDto.builder().depositAmount(new BigDecimal("99999")).build();
        var view = service.update(1L, patch, 99L);
        assertThat(view.getDepositAmount()).isEqualByComparingTo("99999");
        assertThat(view.getOwnerUnit()).isEqualTo("国网");
    }

    @Test
    void update_unknownInitiation_throws404() {
        when(repo.findByProjectId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, fullDto(), 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
