package com.xiyu.bid.integration.organization.application;

import com.xiyu.bid.integration.organization.domain.OrganizationDepartmentSnapshot;
import com.xiyu.bid.integration.organization.infrastructure.persistence.entity.OrganizationDepartmentEntity;
import com.xiyu.bid.integration.organization.infrastructure.persistence.repository.OrganizationDepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationDepartmentSyncWriter - department persistence")
class OrganizationDepartmentSyncWriterTest {
    @Mock
    private OrganizationDepartmentRepository repository;

    @Test
    @DisplayName("upsert keeps external department id and internal department code separately")
    void upsert_mapsExternalDepartmentIdSeparately() {
        when(repository.findBySourceAppAndExternalDeptId("customer-org", "D001")).thenReturn(Optional.empty());
        when(repository.findById("sales")).thenReturn(Optional.empty());
        when(repository.save(any(OrganizationDepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrganizationDepartmentSyncWriter writer = new OrganizationDepartmentSyncWriter(repository);

        writer.upsert("customer-org", "event-key", new OrganizationDepartmentSnapshot("D001", "sales", "销售部", "", "", true));

        ArgumentCaptor<OrganizationDepartmentEntity> saved = ArgumentCaptor.forClass(OrganizationDepartmentEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getDepartmentCode()).isEqualTo("sales");
        assertThat(saved.getValue().getExternalDeptId()).isEqualTo("D001");
        assertThat(saved.getValue().getLastEventKey()).isEqualTo("event-key");
    }
}
