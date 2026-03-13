package com.xiyu.bid.config;

import com.xiyu.bid.entity.Tender;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.TenderRepository;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile({"e2e", "local-pg"})
@RequiredArgsConstructor
public class E2eDemoUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final TenderRepository tenderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedUser("xiaowang", "123456", "xiaowang@example.com", "小王", User.Role.STAFF);
        seedUser("zhangjingli", "123456", "zhangjingli@example.com", "张经理", User.Role.MANAGER);
        seedUser("lizong", "123456", "lizong@example.com", "李总", User.Role.ADMIN);
        seedTender("某央企智慧办公平台采购项目", "央企采购平台", BigDecimal.valueOf(500), LocalDateTime.now().plusDays(15), 92, Tender.RiskLevel.LOW);
        seedTender("华南电力集团集采项目", "南方电网招采平台", BigDecimal.valueOf(1200), LocalDateTime.now().plusDays(20), 78, Tender.RiskLevel.MEDIUM);
        seedTender("深圳地铁自动化系统建设", "深圳地铁电子采购平台", BigDecimal.valueOf(800), LocalDateTime.now().plusDays(10), 85, Tender.RiskLevel.LOW);
    }

    private void seedUser(String username, String password, String email, String fullName, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build());
    }

    private void seedTender(String title, String source, BigDecimal budget, LocalDateTime deadline, Integer aiScore, Tender.RiskLevel riskLevel) {
        boolean exists = tenderRepository.findAll().stream()
                .anyMatch(tender -> title.equals(tender.getTitle()));
        if (exists) {
            return;
        }
        tenderRepository.save(Tender.builder()
                .title(title)
                .source(source)
                .budget(budget)
                .deadline(deadline)
                .status(Tender.Status.TRACKING)
                .aiScore(aiScore)
                .riskLevel(riskLevel)
                .build());
    }
}
