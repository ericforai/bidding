// Input: platform repositories, DTOs, and support services
// Output: Platform Account business service operations
// Pos: Service/业务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
package com.xiyu.bid.platform.service;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.platform.dto.BorrowAccountRequest;
import com.xiyu.bid.platform.dto.PlatformAccountCreateRequest;
import com.xiyu.bid.platform.dto.PlatformAccountDTO;
import com.xiyu.bid.platform.dto.PlatformAccountStatisticsDTO;
import com.xiyu.bid.platform.entity.PlatformAccount;
import com.xiyu.bid.platform.entity.PlatformAccount.AccountStatus;
import com.xiyu.bid.platform.entity.PlatformAccount.PlatformType;
import com.xiyu.bid.platform.repository.PlatformAccountRepository;
import com.xiyu.bid.platform.util.PasswordEncryptionUtil;
import com.xiyu.bid.audit.service.AuditLogService;
import com.xiyu.bid.audit.service.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Platform Accounts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAccountService {

    private final PlatformAccountRepository repository;
    private final PasswordEncryptionUtil passwordEncryptionUtil;
    private final IAuditLogService auditLogService;

    /**
     * Create a new platform account
     * @param request the account creation request
     * @param currentUser the user creating the account
     * @return the created account DTO
     */
    @Transactional
    public PlatformAccountDTO createAccount(PlatformAccountCreateRequest request, User currentUser) {
        validateRequest(request);

        // Check if username already exists
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Encrypt password
        String encryptedPassword = passwordEncryptionUtil.encrypt(request.getPassword());

        // Create account
        PlatformAccount account = PlatformAccount.builder()
            .username(request.getUsername())
            .password(encryptedPassword)
            .accountName(request.getAccountName())
            .platformType(request.getPlatformType())
            .status(AccountStatus.AVAILABLE)
            .returnCount(0)
            .build();

        PlatformAccount savedAccount = repository.save(account);

        // Log audit
        logAsync(AuditLogService.AuditLogEntry.builder()
            .userId(currentUser != null ? currentUser.getId().toString() : "SYSTEM")
            .username(currentUser != null ? currentUser.getUsername() : "SYSTEM")
            .action("CREATE")
            .entityType("PlatformAccount")
            .entityId(savedAccount.getId().toString())
            .description("Created platform account: " + savedAccount.getAccountName())
            .success(true)
            .build());

        return toDTO(savedAccount);
    }

    /**
     * Get account by ID
     * @param id the account ID
     * @return the account DTO
     */
    public PlatformAccountDTO getAccountById(Long id) {
        PlatformAccount account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));
        return toDTO(account);
    }

    /**
     * Get all accounts
     * @return list of all account DTOs
     */
    public List<PlatformAccountDTO> getAllAccounts() {
        return repository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Update an existing account
     * @param id the account ID
     * @param request the update request
     * @param currentUser the user updating the account
     * @return the updated account DTO
     */
    @Transactional
    public PlatformAccountDTO updateAccount(Long id, PlatformAccountCreateRequest request, User currentUser) {
        PlatformAccount account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));

        PlatformAccountDTO oldDTO = toDTO(account);
        if (request.getUsername() != null
                && !request.getUsername().trim().isEmpty()
                && !request.getUsername().equals(account.getUsername())
                && repository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        String encryptedPassword = request.getPassword() != null && !request.getPassword().trim().isEmpty()
                ? passwordEncryptionUtil.encrypt(request.getPassword())
                : null;
        account.updateProfile(
                request.getUsername(),
                encryptedPassword,
                request.getAccountName(),
                request.getPlatformType()
        );

        PlatformAccount savedAccount = repository.save(account);

        // Log audit
        logAsync(AuditLogService.AuditLogEntry.builder()
            .userId(currentUser != null ? currentUser.getId().toString() : "SYSTEM")
            .username(currentUser != null ? currentUser.getUsername() : "SYSTEM")
            .action("UPDATE")
            .entityType("PlatformAccount")
            .entityId(id.toString())
            .description("Updated platform account: " + savedAccount.getAccountName())
            .oldValue(oldDTO.toString())
            .newValue(toDTO(savedAccount).toString())
            .success(true)
            .build());

        return toDTO(savedAccount);
    }

    /**
     * Delete an account
     * @param id the account ID
     * @param currentUser the user deleting the account
     */
    @Transactional
    public void deleteAccount(Long id, User currentUser) {
        PlatformAccount account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));

        // Prevent deletion of accounts that are in use
        if (account.getStatus() == AccountStatus.IN_USE) {
            throw new IllegalStateException("Cannot delete account that is currently in use");
        }

        repository.delete(account);

        // Log audit
        logAsync(AuditLogService.AuditLogEntry.builder()
            .userId(currentUser != null ? currentUser.getId().toString() : "SYSTEM")
            .username(currentUser != null ? currentUser.getUsername() : "SYSTEM")
            .action("DELETE")
            .entityType("PlatformAccount")
            .entityId(id.toString())
            .description("Deleted platform account: " + account.getAccountName())
            .oldValue(toDTO(account).toString())
            .success(true)
            .build());
    }

    /**
     * Borrow an account
     * @param id the account ID
     * @param request the borrow request
     * @param currentUser the user borrowing the account
     * @return the updated account DTO
     */
    @Transactional
    public PlatformAccountDTO borrowAccount(Long id, BorrowAccountRequest request, User currentUser) {
        PlatformAccount account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));
        account.borrow(request.getBorrowedBy(), LocalDateTime.now(), LocalDateTime.now().plusHours(request.getDueHours()));

        PlatformAccount savedAccount = repository.save(account);

        // Log audit
        logAsync(AuditLogService.AuditLogEntry.builder()
            .userId(currentUser != null ? currentUser.getId().toString() : "SYSTEM")
            .username(currentUser != null ? currentUser.getUsername() : "SYSTEM")
            .action("BORROW")
            .entityType("PlatformAccount")
            .entityId(id.toString())
            .description("Borrowed platform account: " + savedAccount.getAccountName() +
                " by user ID: " + request.getBorrowedBy())
            .success(true)
            .build());

        return toDTO(savedAccount);
    }

    /**
     * Return a borrowed account
     * @param id the account ID
     * @param currentUser the user returning the account
     * @return the updated account DTO
     */
    @Transactional
    public PlatformAccountDTO returnAccount(Long id, User currentUser) {
        PlatformAccount account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));
        Long previousBorrower = account.getBorrowedBy();
        account.returnToPool();

        PlatformAccount savedAccount = repository.save(account);

        // Log audit
        logAsync(AuditLogService.AuditLogEntry.builder()
            .userId(currentUser != null ? currentUser.getId().toString() : "SYSTEM")
            .username(currentUser != null ? currentUser.getUsername() : "SYSTEM")
            .action("RETURN")
            .entityType("PlatformAccount")
            .entityId(id.toString())
            .description("Returned platform account: " + savedAccount.getAccountName() +
                " by user ID: " + previousBorrower)
            .success(true)
            .build());

        return toDTO(savedAccount);
    }

    /**
     * Get decrypted password for an account (ADMIN only)
     * This action is audit logged
     * @param id the account ID
     * @param currentUser the user requesting the password
     * @return the decrypted password
     */
    public String getPassword(Long id, User currentUser) {
        // Check admin permission
        if (currentUser == null || currentUser.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException("Only administrators can view account passwords");
        }

        PlatformAccount account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));

        String decryptedPassword = passwordEncryptionUtil.decrypt(account.getPassword());

        // Log audit for security
        logAsync(AuditLogService.AuditLogEntry.builder()
            .userId(currentUser.getId().toString())
            .username(currentUser.getUsername())
            .action("VIEW_PASSWORD")
            .entityType("PlatformAccount")
            .entityId(id.toString())
            .description("Viewed password for platform account: " + account.getAccountName())
            .success(true)
            .build());

        return decryptedPassword;
    }

    /**
     * Get account statistics
     * @return statistics DTO
     */
    public PlatformAccountStatisticsDTO getStatistics() {
        long totalAccounts = repository.count();
        long availableAccounts = repository.countByStatus(AccountStatus.AVAILABLE);
        long inUseAccounts = repository.countByStatus(AccountStatus.IN_USE);
        long maintenanceAccounts = repository.countByStatus(AccountStatus.MAINTENANCE);
        long disabledAccounts = repository.countByStatus(AccountStatus.DISABLED);

        return PlatformAccountStatisticsDTO.builder()
            .totalAccounts(totalAccounts)
            .availableAccounts(availableAccounts)
            .inUseAccounts(inUseAccounts)
            .maintenanceAccounts(maintenanceAccounts)
            .disabledAccounts(disabledAccounts)
            .build();
    }

    /**
     * Find overdue accounts
     * @return list of overdue account DTOs
     */
    public List<PlatformAccountDTO> findOverdueAccounts() {
        List<PlatformAccount> overdueAccounts =
            repository.findOverdueAccounts(AccountStatus.IN_USE, LocalDateTime.now());
        return overdueAccounts.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     * Note: Password is intentionally excluded
     */
    private PlatformAccountDTO toDTO(PlatformAccount account) {
        return PlatformAccountDTO.builder()
            .id(account.getId())
            .username(account.getUsername())
            // Password is excluded for security
            .accountName(account.getAccountName())
            .platformType(account.getPlatformType())
            .status(account.getStatus())
            .borrowedBy(account.getBorrowedBy())
            .borrowedAt(account.getBorrowedAt())
            .dueAt(account.getDueAt())
            .returnCount(account.getReturnCount())
            .createdAt(account.getCreatedAt())
            .updatedAt(account.getUpdatedAt())
            .build();
    }

    /**
     * Log audit entry asynchronously
     */
    @Async
    protected void logAsync(AuditLogService.AuditLogEntry entry) {
        try {
            auditLogService.log(entry);
        } catch (RuntimeException e) {
            log.error("Failed to log audit entry", e);
        }
    }

    private void validateRequest(PlatformAccountCreateRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (request.getAccountName() == null || request.getAccountName().trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }
        if (request.getPlatformType() == null) {
            throw new IllegalArgumentException("Platform type cannot be null");
        }
    }
}
