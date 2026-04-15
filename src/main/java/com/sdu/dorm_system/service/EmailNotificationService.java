package com.sdu.dorm_system.service;

import com.sdu.dorm_system.config.AppProperties;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties appProperties;

    @Override
    public void sendTemporaryPassword(UserAccount user, String rawPassword, String reason) {
        String messageText = """
            Hello %s %s,

            %s

            Email: %s
            Temporary password: %s

            Please log in and keep this password secure.
            """.formatted(user.getName(), user.getSurname(), reason, user.getEmail(), rawPassword);

        if (!appProperties.mail().enabled()) {
            log.info("Mail disabled. Temporary password for {} is {}", user.getEmail(), rawPassword);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail is enabled, but no JavaMailSender bean is configured. Temporary password for {} is {}", user.getEmail(), rawPassword);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.mail().from());
        message.setTo(user.getEmail());
        message.setSubject("SDU Dorm System account access");
        message.setText(messageText);

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Failed to send temporary password email to {}", user.getEmail(), exception);
            throw BusinessException.badRequest("Failed to send email. Check the mail configuration and recipient address.");
        }
    }
}
