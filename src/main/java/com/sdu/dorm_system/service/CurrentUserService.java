package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;

    @Transactional(readOnly = true)
    public UserAccount getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw BusinessException.unauthorized("Authentication is required");
        }

        return userAccountRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> BusinessException.unauthorized("Authenticated user was not found"));
    }
}
