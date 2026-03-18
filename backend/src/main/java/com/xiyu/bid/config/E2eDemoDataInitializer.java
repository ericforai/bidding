// Input: Spring 配置属性、环境变量、外部 bean 依赖
// Output: 配置 Bean、过滤器、线程池和启动级常量
// Pos: Config/基础设施层
// 维护声明: 仅维护配置与启动约束；业务规则变更请同步到对应 service/controller.
package com.xiyu.bid.config;

import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("e2e")
@RequiredArgsConstructor
@Slf4j
public class E2eDemoDataInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedDemoUsers();
    }

    void seedDemoUsers() {
        List<DemoUser> demoUsers = List.of(
                new DemoUser("lizong", "李总", "lizong@example.com", User.Role.ADMIN),
                new DemoUser("zhangjingli", "张经理", "zhangjingli@example.com", User.Role.MANAGER),
                new DemoUser("xiaowang", "小王", "xiaowang@example.com", User.Role.STAFF)
        );

        demoUsers.forEach(this::createOrUpdateUser);
    }

    private void createOrUpdateUser(DemoUser demoUser) {
        User user = userRepository.findByUsername(demoUser.username())
                .orElseGet(User::new);

        user.setUsername(demoUser.username());
        user.setFullName(demoUser.fullName());
        user.setEmail(demoUser.email());
        user.setRole(demoUser.role());
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));

        userRepository.save(user);
        log.info("Seeded e2e demo user: {}", demoUser.username());
    }

    private record DemoUser(String username, String fullName, String email, User.Role role) {
    }
}
