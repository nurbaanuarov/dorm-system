package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.service.AuthService;
import com.sdu.dorm_system.service.CurrentUserService;
import com.sdu.dorm_system.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final RoomService roomService;

    @PostMapping("/login")
    public ApiModels.AuthenticatedUserResponse login(@Valid @RequestBody ApiModels.LoginRequest request) {
        UserAccount currentUser = authService.authenticate(request.email(), request.password());
        RoomService.RoomAssignment roomAssignment = currentUser.getRole() == Role.STUDENT
            ? roomService.getAssignment(currentUser)
            : null;

        return ApiModels.toAuthenticatedUserResponse(currentUser, roomAssignment);
    }

    @GetMapping("/me")
    public ApiModels.AuthenticatedUserResponse me(Authentication authentication) {
        UserAccount currentUser = currentUserService.getCurrentUser(authentication);
        RoomService.RoomAssignment roomAssignment = currentUser.getRole() == Role.STUDENT
            ? roomService.getAssignment(currentUser)
            : null;

        return ApiModels.toAuthenticatedUserResponse(currentUser, roomAssignment);
    }
}
