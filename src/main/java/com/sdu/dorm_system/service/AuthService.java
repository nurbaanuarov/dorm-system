package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserAccount authenticate(String email, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw BusinessException.unauthorized("Invalid credentials");
        }

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> BusinessException.unauthorized("Invalid credentials"));

        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BusinessException.unauthorized("Invalid credentials");
        }

        return user;
    }

    @Transactional
    public void changePassword(UserAccount user, ChangePasswordCommand command) {
        if (user == null || !user.isEnabled()) {
            throw BusinessException.unauthorized("Authenticated user was not found");
        }

        if (!StringUtils.hasText(command.password())
            || !StringUtils.hasText(command.newPassword())
            || !StringUtils.hasText(command.repeatNewPassword())) {
            throw BusinessException.badRequest("All password fields are required");
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Current password is incorrect");
        }

        if (!command.newPassword().equals(command.repeatNewPassword())) {
            throw BusinessException.badRequest("New password fields do not match");
        }

        if (command.password().equals(command.newPassword())) {
            throw BusinessException.badRequest("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(command.newPassword()));
        userAccountRepository.save(user);
    }

    public record ChangePasswordCommand(
        String password,
        String newPassword,
        String repeatNewPassword
    ) {
    }
}
