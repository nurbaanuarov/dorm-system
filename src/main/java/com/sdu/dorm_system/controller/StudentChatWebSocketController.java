package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.service.CurrentUserService;
import com.sdu.dorm_system.service.StudentChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StudentChatWebSocketController {

    private final CurrentUserService currentUserService;
    private final StudentChatService studentChatService;

    @MessageMapping("/student.global-chat.send")
    public void sendGlobalChatMessage(
        @Valid @Payload ApiModels.ChatMessageRequest request,
        Principal principal
    ) {
        UserAccount actor = currentUserService.getCurrentUser(principal == null ? null : principal.getName());
        studentChatService.addMessage(
            actor,
            new StudentChatService.CreateChatMessageCommand(request.content(), request.parentMessageId())
        );
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ApiModels.MessageResponse handleBusinessException(BusinessException exception) {
        return new ApiModels.MessageResponse(exception.getMessage());
    }
}
