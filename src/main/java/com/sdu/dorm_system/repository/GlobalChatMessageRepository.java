package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.GlobalChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalChatMessageRepository extends JpaRepository<GlobalChatMessage, UUID> {

    List<GlobalChatMessage> findAllByParentMessageIsNullOrderByCreatedAtDesc();
}
