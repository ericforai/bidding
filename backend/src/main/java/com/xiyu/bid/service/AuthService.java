// Input: UserRepository、PasswordEncoder、JwtUtil、AuthenticationManager
// Output: 登录/注册结果、JWT 令牌和认证上下文
// Pos: Auth/认证支撑层
// 维护声明: 仅维护认证链路；权限规则调整请同步 controller 与 security 配置.
package com.xiyu.bid.service;

import com.xiyu.bid.dto.AuthResponse;
import com.xiyu.bid.dto.LoginRequest;
import com.xiyu.bid.dto.RegisterRequest;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.auth.JwtUtil;
import com.xiyu.bid.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ConcurrentMap<String, String> activeRefreshTokenIdsByUsername = new ConcurrentHashMap<>();

    public record RefreshSession(AuthResponse authResponse, String refreshToken) {}

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate password strength
        PasswordValidator.ValidationResult passwordValidation = PasswordValidator.validate(request.getPassword());
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException(passwordValidation.getMessage());
        }

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : User.Role.STAFF)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());
        return AuthResponse.from(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername());
        String refreshToken = issueRefreshToken(user.getUsername());
        log.info("User logged in: {}", user.getUsername());

        return AuthResponse.from(token, refreshToken, user);
    }

    public AuthResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return AuthResponse.builder()
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    public void logout(String username) {
        logout(username, null, null);
    }

    public void logout(String username, String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            jwtUtil.revokeToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            jwtUtil.revokeToken(refreshToken);
        }
        if (username != null && !username.isBlank()) {
            activeRefreshTokenIdsByUsername.remove(username);
        }
        log.info("User logged out: {}", username);
    }

    public String issueRefreshToken(String username) {
        String refreshToken = jwtUtil.generateRefreshToken(username);
        activeRefreshTokenIdsByUsername.put(username, jwtUtil.extractTokenId(refreshToken));
        return refreshToken;
    }

    public AuthResponse refreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername());
        log.info("Token refreshed for user: {}", user.getUsername());
        return AuthResponse.from(token, user);
    }

    public RefreshSession refreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        String activeRefreshTokenId = activeRefreshTokenIdsByUsername.get(username);
        String providedRefreshTokenId = jwtUtil.extractTokenId(refreshToken);
        if (activeRefreshTokenId == null || !activeRefreshTokenId.equals(providedRefreshTokenId)) {
            throw new IllegalArgumentException("Refresh token is no longer active");
        }

        jwtUtil.revokeToken(refreshToken);
        String accessToken = jwtUtil.generateToken(username);
        String rotatedRefreshToken = issueRefreshToken(username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        AuthResponse authResponse = AuthResponse.from(accessToken, rotatedRefreshToken, user);
        return new RefreshSession(authResponse, rotatedRefreshToken);
    }
}
