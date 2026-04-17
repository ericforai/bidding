package com.xiyu.bid.bootstrap;

import com.xiyu.bid.entity.RoleProfile;
import com.xiyu.bid.entity.RoleProfileCatalog;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.RoleProfileRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminInitializer implements ApplicationRunner {

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password:XiyuAdmin2026!}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.email:admin@xiyu-local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.full-name:系统管理员}")
    private String adminFullName;

    private final UserRepository userRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            log.warn("ZERO users detected in database — seeding default admin account.");
            seedDefaultAdmin();
        } else if (!userRepository.existsByUsername(adminUsername)) {
            log.info("Default admin '{}' not found — creating.", adminUsername);
            seedDefaultAdmin();
        }
    }

    private void seedDefaultAdmin() {
        String roleCode = RoleProfileCatalog.definitionForLegacyRole(User.Role.ADMIN).code();
        RoleProfile profile = roleProfileRepository.findByCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new IllegalStateException("Required RoleProfile not found: " + roleCode));

        User user = userRepository.findByUsername(adminUsername)
                .orElseGet(User::new);

        user.setUsername(adminUsername);
        user.setFullName(adminFullName);
        user.setEmail(adminEmail);
        user.setRole(User.Role.ADMIN);
        user.setRoleProfile(profile);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(adminPassword));

        userRepository.save(user);

        log.warn("=== DEFAULT ADMIN CREDENTIALS ===");
        log.warn("  Username: {}", adminUsername);
        log.warn("  Password: {} (CHANGE THIS IN PRODUCTION!)", adminPassword);
        log.warn("==================================");
    }
}
