package com.linkvault.users.service;

import com.linkvault.auth.security.AuthenticatedUser;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.users.entity.User;
import com.linkvault.users.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserContextService {

    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Login required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            User user = userRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_LOGIN_REQUIRED, "User account no longer exists"));
            if (Boolean.FALSE.equals(user.getIsEnabled())) {
                throw new UnauthorizedException("Account is disabled");
            }
            return user;
        }

        throw new UnauthorizedException("Login required");
    }
}