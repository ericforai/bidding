package com.xiyu.bid.resources.service;

import com.xiyu.bid.exception.ResourceNotFoundException;
import com.xiyu.bid.resources.dto.BarAssetCreateRequest;
import com.xiyu.bid.resources.dto.BarAssetUpdateRequest;
import com.xiyu.bid.resources.entity.BarAsset;
import com.xiyu.bid.resources.repository.BarAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarAssetService {

    private final BarAssetRepository barAssetRepository;

    @Transactional
    public BarAsset createBarAsset(BarAssetCreateRequest request) {
        // Validation
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Type is required");
        }
        if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Value must be positive");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (request.getAcquireDate() == null) {
            throw new IllegalArgumentException("Acquire date is required");
        }
        if (request.getAcquireDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Acquire date cannot be in the future");
        }

        BarAsset asset = BarAsset.builder()
                .name(request.getName())
                .type(request.getType())
                .value(request.getValue())
                .status(request.getStatus())
                .acquireDate(request.getAcquireDate())
                .remark(request.getRemark())
                .build();

        return barAssetRepository.save(asset);
    }

    public BarAsset getBarAssetById(Long id) {
        return barAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BarAsset", id.toString()));
    }

    public Page<BarAsset> getAllBarAssets(Pageable pageable) {
        return barAssetRepository.findAll(pageable);
    }

    public Page<BarAsset> getBarAssetsByType(BarAsset.AssetType type, Pageable pageable) {
        return barAssetRepository.findByType(type, pageable);
    }

    public Page<BarAsset> getBarAssetsByStatus(BarAsset.AssetStatus status, Pageable pageable) {
        return barAssetRepository.findByStatus(status, pageable);
    }

    public Page<BarAsset> getBarAssetsByValueRange(BigDecimal minValue, BigDecimal maxValue, Pageable pageable) {
        return barAssetRepository.findByValueBetween(minValue, maxValue, pageable);
    }

    public Page<BarAsset> getBarAssetsByAcquireDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return barAssetRepository.findByAcquireDateBetween(startDate, endDate, pageable);
    }

    public Page<BarAsset> searchBarAssets(String keyword, Pageable pageable) {
        return barAssetRepository.searchByNameContainingIgnoreCase(keyword, pageable);
    }

    @Transactional
    public BarAsset updateBarAsset(Long id, BarAssetUpdateRequest request) {
        BarAsset asset = getBarAssetById(id);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            asset.setName(request.getName());
        }
        if (request.getType() != null) {
            asset.setType(request.getType());
        }
        if (request.getValue() != null) {
            if (request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Value must be positive");
            }
            asset.setValue(request.getValue());
        }
        if (request.getStatus() != null) {
            asset.setStatus(request.getStatus());
        }
        if (request.getAcquireDate() != null) {
            if (request.getAcquireDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Acquire date cannot be in the future");
            }
            asset.setAcquireDate(request.getAcquireDate());
        }
        if (request.getRemark() != null) {
            asset.setRemark(request.getRemark());
        }

        return barAssetRepository.save(asset);
    }

    @Transactional
    public void deleteBarAsset(Long id) {
        if (!barAssetRepository.existsById(id)) {
            throw new ResourceNotFoundException("BarAsset", id.toString());
        }
        barAssetRepository.deleteById(id);
    }

    public BigDecimal getTotalAssetValue() {
        BigDecimal total = barAssetRepository.sumTotalValue();
        return total != null ? total : BigDecimal.ZERO;
    }

    public Map<String, Object> getAssetStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAssets", barAssetRepository.count());
        statistics.put("totalValue", getTotalAssetValue());

        for (BarAsset.AssetType type : BarAsset.AssetType.values()) {
            statistics.put(type.name().toLowerCase() + "Count", barAssetRepository.countByType(type));
        }

        return statistics;
    }
}
