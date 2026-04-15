package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.GlobalChatMessage;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.GlobalChatMessageRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentChatService {

    private final GlobalChatMessageRepository globalChatMessageRepository;

    @Transactional(readOnly = true)
    public Page<ChatMessageView> listMessages(UserAccount actor, Pageable pageable) {
        requireStudent(actor);
        return globalChatMessageRepository.findAllByParentMessageIsNull(pageable)
            .map(this::toView);
    }

    @Transactional
    public ChatMessageView addMessage(UserAccount actor, CreateChatMessageCommand command) {
        requireStudent(actor);

        GlobalChatMessage parentMessage = resolveParentMessage(command.parentMessageId());

        GlobalChatMessage message = new GlobalChatMessage();
        message.setAuthor(actor);
        message.setParentMessage(parentMessage);
        message.setContent(command.content().trim());
        globalChatMessageRepository.save(message);

        return toView(resolveRootMessage(message));
    }

    private void requireStudent(UserAccount actor) {
        if (actor.getRole() != Role.STUDENT) {
            throw BusinessException.forbidden("Only students can use the global chat");
        }
    }

    private GlobalChatMessage resolveParentMessage(UUID parentMessageId) {
        if (parentMessageId == null) {
            return null;
        }

        return globalChatMessageRepository.findById(parentMessageId)
            .orElseThrow(() -> BusinessException.notFound("Parent chat message was not found"));
    }

    private GlobalChatMessage resolveRootMessage(GlobalChatMessage message) {
        GlobalChatMessage current = message;
        while (current.getParentMessage() != null) {
            current = current.getParentMessage();
        }
        return current;
    }

    private ChatMessageView toView(GlobalChatMessage message) {
        return new ChatMessageView(
            message.getId(),
            message.getParentMessage() == null ? null : message.getParentMessage().getId(),
            message.getAuthor().getName() + " " + message.getAuthor().getSurname(),
            message.getContent(),
            message.getCreatedAt(),
            message.getReplies().stream()
                .sorted(Comparator.comparing(GlobalChatMessage::getCreatedAt))
                .map(this::toView)
                .toList()
        );
    }

    public record CreateChatMessageCommand(
        String content,
        UUID parentMessageId
    ) {
    }

    public record ChatMessageView(
        UUID id,
        UUID parentMessageId,
        String authorName,
        String content,
        java.time.OffsetDateTime createdAt,
        List<ChatMessageView> replies
    ) {
    }
}
