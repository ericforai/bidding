// Input: resources repositories, DTOs, and support services
// Output: Account business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.resources.service;

import com.xiyu.bid.resources.dto.AccountCreateRequest;
import com.xiyu.bid.resources.dto.AccountUpdateRequest;
import com.xiyu.bid.resources.entity.Account;
import com.xiyu.bid.resources.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(AccountCreateRequest request) {
        // Validation
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Type is required");
        }
        if (request.getCreditLevel() == null) {
            throw new IllegalArgumentException("Credit level is required");
        }

        // Check for duplicate name
        accountRepository.findByName(request.getName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Account with this name already exists");
                });

        Account account = Account.builder()
                .name(request.getName())
                .type(request.getType())
                .contactInfo(request.getContactInfo())
                .industry(request.getIndustry())
                .region(request.getRegion())
                .creditLevel(request.getCreditLevel())
                .build();

        return accountRepository.save(account);
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
    }

    public Page<Account> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    public Page<Account> getAccountsByType(Account.AccountType type, Pageable pageable) {
        return accountRepository.findByType(type, pageable);
    }

    public Page<Account> getAccountsByIndustry(String industry, Pageable pageable) {
        return accountRepository.findByIndustry(industry, pageable);
    }

    public Page<Account> getAccountsByRegion(String region, Pageable pageable) {
        return accountRepository.findByRegion(region, pageable);
    }

    public Page<Account> getAccountsByCreditLevel(Account.CreditLevel creditLevel, Pageable pageable) {
        return accountRepository.findByCreditLevel(creditLevel, pageable);
    }

    public Page<Account> searchAccounts(String keyword, Pageable pageable) {
        return accountRepository.searchByNameContainingIgnoreCase(keyword, pageable);
    }

    @Transactional
    public Account updateAccount(Long id, AccountUpdateRequest request) {
        Account account = getAccountById(id);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            // Check if name is being changed and if new name already exists
            if (!account.getName().equals(request.getName())) {
                accountRepository.findByName(request.getName())
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Account with this name already exists");
                        });
                account.setName(request.getName());
            }
        }
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        if (request.getContactInfo() != null) {
            account.setContactInfo(request.getContactInfo());
        }
        if (request.getIndustry() != null) {
            account.setIndustry(request.getIndustry());
        }
        if (request.getRegion() != null) {
            account.setRegion(request.getRegion());
        }
        if (request.getCreditLevel() != null) {
            account.setCreditLevel(request.getCreditLevel());
        }

        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
    }

    public Map<String, Object> getAccountStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAccounts", accountRepository.count());

        for (Account.AccountType type : Account.AccountType.values()) {
            statistics.put(type.name().toLowerCase() + "Count", accountRepository.countByType(type));
        }

        return statistics;
    }
}
