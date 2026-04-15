package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.GlobalChatMessage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalChatMessageRepository extends JpaRepository<GlobalChatMessage, UUID> {

    Page<GlobalChatMessage> findAllByParentMessageIsNull(Pageable pageable);
}
