package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.service.AuthService;
import com.sdu.dorm_system.service.CurrentUserService;
import com.sdu.dorm_system.service.JwtTokenService;
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
    private final JwtTokenService jwtTokenService;
    private final CurrentUserService currentUserService;
    private final RoomService roomService;

    @PostMapping("/login")
    public ApiModels.LoginResponse login(@Valid @RequestBody ApiModels.LoginRequest request) {
        UserAccount currentUser = authService.authenticate(request.email(), request.password());
        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issueToken(currentUser);
        RoomService.RoomAssignment roomAssignment = currentUser.getRole() == Role.STUDENT
            ? roomService.getAssignment(currentUser)
            : null;

        return new ApiModels.LoginResponse(
            issuedToken.token(),
            "Bearer",
            issuedToken.expiresAt(),
            ApiModels.toAuthenticatedUserResponse(currentUser, roomAssignment)
        );
    }

    @PostMapping("/change-password")
    public ApiModels.MessageResponse changePassword(
        @Valid @RequestBody ApiModels.ChangePasswordRequest request,
        Authentication authentication
    ) {
        UserAccount currentUser = currentUserService.getCurrentUser(authentication);
        authService.changePassword(currentUser, new AuthService.ChangePasswordCommand(
            request.password(),
            request.newPassword(),
            request.repeatNewPassword()
        ));
        return new ApiModels.MessageResponse("Password was changed successfully");
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
