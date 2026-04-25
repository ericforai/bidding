package com.xiyu.bid.integration.application;

import com.xiyu.bid.integration.domain.ValidationResult;
import com.xiyu.bid.integration.domain.WeComConnectivityResult;
import com.xiyu.bid.integration.domain.WeComCredential;
import com.xiyu.bid.integration.domain.WeComCredentialValidation;
import com.xiyu.bid.integration.dto.WeComConnectivityResponse;
import com.xiyu.bid.integration.dto.WeComIntegrationRequest;
import com.xiyu.bid.integration.dto.WeComIntegrationResponse;
import com.xiyu.bid.integration.infrastructure.persistence.entity.WeComIntegrationEntity;
import com.xiyu.bid.integration.infrastructure.persistence.repository.WeComIntegrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestration service for WeCom integration.
 * Responsibilities: validate → cipher → persist → probe.
 * Does NOT contain rule logic or DTO conversion (controller owns that).
 */
@Service
@RequiredArgsConstructor
public class WeComIntegrationAppService {

    private static final long SINGLETON_ID = 1L;

    private final WeComIntegrationJpaRepository repository;
    private final WeComCredentialCipher cipher;
    private final WeComConnectivityProbe connectivityProbe;

    @Transactional(readOnly = true)
    public WeComIntegrationResponse getConfig() {
        Optional<WeComIntegrationEntity> entity = repository.findById(SINGLETON_ID);
        if (entity.isEmpty()) {
            return WeComIntegrationResponse.empty();
        }
        WeComIntegrationEntity e = entity.get();
        return WeComIntegrationResponse.configured(
                e.getCorpId(), e.getAgentId(), e.isSsoEnabled(), e.isMessageEnabled());
    }

    @Transactional
    public WeComIntegrationResponse saveConfig(WeComIntegrationRequest request, String operator) {
        WeComCredential credential = new WeComCredential(
                request.corpId(), request.agentId(), request.corpSecret(),
                request.ssoEnabled(), request.messageEnabled());

        ValidationResult validation = WeComCredentialValidation.validate(credential);
        if (!validation.valid()) {
            throw new IllegalArgumentException(String.join("; ", validation.errors()));
        }

        String encryptedSecret = cipher.encrypt(credential.corpSecret());

        WeComIntegrationEntity entity = repository.findById(SINGLETON_ID)
                .orElseGet(WeComIntegrationEntity::new);
        entity.setId(SINGLETON_ID);
        entity.setCorpId(credential.corpId());
        entity.setAgentId(credential.agentId());
        entity.setEncryptedSecret(encryptedSecret);
        entity.setSsoEnabled(credential.ssoEnabled());
        entity.setMessageEnabled(credential.messageEnabled());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(operator);

        WeComIntegrationEntity saved = repository.save(entity);
        return WeComIntegrationResponse.configured(
                saved.getCorpId(), saved.getAgentId(), saved.isSsoEnabled(), saved.isMessageEnabled());
    }

    @Transactional(readOnly = true)
    public WeComConnectivityResponse testConnectivity() {
        WeComIntegrationEntity entity = repository.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("企业微信集成尚未配置，请先保存配置后再测试连通性"));

        String plainSecret = cipher.decrypt(entity.getEncryptedSecret());
        WeComConnectivityResult result = connectivityProbe.probe(
                entity.getCorpId(), entity.getAgentId(), plainSecret);

        return new WeComConnectivityResponse(result.success(), result.message(), result.probedAt());
    }
}
