package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.UserAccount;

public interface NotificationService {

    void sendTemporaryPassword(UserAccount user, String rawPassword, String reason);
}
