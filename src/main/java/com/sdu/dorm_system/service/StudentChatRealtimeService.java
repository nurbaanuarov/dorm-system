package com.sdu.dorm_system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class StudentChatRealtimeService {

    public static final String GLOBAL_CHAT_TOPIC = "/topic/student.global-chat";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessageCreated(StudentChatService.ChatMessageCreatedEvent event) {
        messagingTemplate.convertAndSend(GLOBAL_CHAT_TOPIC, event.event());
    }
}
