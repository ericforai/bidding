package com.xiyu.bid.integration.application;

import com.xiyu.bid.dto.AuthSessionResult;
import com.xiyu.bid.entity.User;
import com.xiyu.bid.repository.UserRepository;
import com.xiyu.bid.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service to handle WeCom authentication and user mapping.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeComAuthAppService {

    private final WeComOAuthService weComOAuthService;
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * Attempts to login a user via WeCom OAuth2 code.
     *
     * @param code WeCom OAuth2 code
     * @return AuthSessionResult if successful, empty if user needs binding or error
     */
    @Transactional
    public Optional<AuthSessionResult> loginByWeCom(String code) {
        // 1. Get user info from WeCom
        return weComOAuthService.getAuthenticatedUserInfo(code).flatMap(userInfo -> {
            String wecomUserId = userInfo.UserId();
            if (wecomUserId == null) {
                log.warn("WeCom OAuth2 callback did not return a UserId (OpenId={})", userInfo.OpenId());
                return Optional.empty();
            }

            // 2. Try to find user by wecomUserId
            Optional<User> userOpt = userRepository.findByWecomUserId(wecomUserId);

            if (userOpt.isEmpty() && userInfo.user_ticket() != null) {
                // 3. Try to find by mobile if wecomUserId is not found
                userOpt = weComOAuthService.getUserDetail(userInfo.user_ticket())
                        .flatMap(detail -> {
                            String mobile = detail.mobile();
                            if (mobile != null && !mobile.isBlank()) {
                                return userRepository.findByPhone(mobile);
                            }
                            return Optional.empty();
                        })
                        .map(user -> {
                            // Link wecomUserId to the found user
                            user.setWecomUserId(wecomUserId);
                            return userRepository.save(user);
                        });
            }

            // 4. Return login result if user found
            return userOpt.map(authService::loginWithoutPassword);
        });
    }
}
