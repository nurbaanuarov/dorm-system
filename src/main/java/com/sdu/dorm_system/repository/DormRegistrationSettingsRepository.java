package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.DormRegistrationSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DormRegistrationSettingsRepository extends JpaRepository<DormRegistrationSettings, UUID> {

    Optional<DormRegistrationSettings> findTopByOrderByUpdatedAtDesc();
}
