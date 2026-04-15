package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.FloorUnit;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Block;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.service.DormRegistrationService;
import com.sdu.dorm_system.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public final class ApiModels {

    private ApiModels() {
    }

    public static AuthenticatedUserResponse toAuthenticatedUserResponse(UserAccount user, RoomService.RoomAssignment roomAssignment) {
        return new AuthenticatedUserResponse(
            user.getId(),
            user.getRole(),
            user.getName(),
            user.getSurname(),
            user.getEmail(),
            user.getStudentIdentifier(),
            user.getGender(),
            roomAssignment == null
                ? null
                : new RoomAssignmentResponse(
                    roomAssignment.roomId(),
                    roomAssignment.block(),
                    roomAssignment.floorNumber(),
                    roomAssignment.roomNumber(),
                    roomAssignment.bedNumber(),
                    roomAssignment.mealTypesIncludedInPrice()
                )
        );
    }

    public static UserResponse toUserResponse(UserAccount user) {
        return new UserResponse(
            user.getId(),
            user.getRole(),
            user.getName(),
            user.getSurname(),
            user.getEmail(),
            user.getStudentIdentifier(),
            user.getGender()
        );
    }

    public static FloorResponse toFloorResponse(FloorUnit floorUnit) {
        return new FloorResponse(floorUnit.getId(), floorUnit.getBlock(), floorUnit.getFloorNumber(), floorUnit.isActive());
    }

    public static DormRegistrationSettingsResponse toDormRegistrationSettingsResponse(
        DormRegistrationService.DormRegistrationSettingsView settings
    ) {
        return new DormRegistrationSettingsResponse(
            settings.startDate(),
            settings.endDate(),
            settings.activeNow(),
            settings.mealOptions().stream()
                .map(option -> new DormRegistrationMealOptionResponse(
                    option.mealType(),
                    option.available(),
                    option.includedInPrice()
                ))
                .toList()
        );
    }

    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    public static <T, R> PageResponse<R> toPageResponse(Page<T> page, Function<T, R> mapper) {
        return new PageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    public record AuthenticatedUserResponse(
        UUID id,
        Role role,
        String name,
        String surname,
        String email,
        String studentIdentifier,
        Gender gender,
        RoomAssignmentResponse roomAssignment
    ) {
    }

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {
    }

    public record LoginResponse(
        String accessToken,
        String tokenType,
        java.time.OffsetDateTime expiresAt,
        AuthenticatedUserResponse user
    ) {
    }

    public record UserResponse(
        UUID id,
        Role role,
        String name,
        String surname,
        String email,
        String studentIdentifier,
        Gender gender
    ) {
    }

    public record RoomAssignmentResponse(
        UUID roomId,
        Block block,
        Integer floorNumber,
        Integer roomNumber,
        Integer bedNumber,
        List<MealType> mealTypesIncludedInPrice
    ) {
    }

    public record AdminUpdateRequest(
        @NotBlank String name,
        @NotBlank String surname,
        @NotBlank @Email String email
    ) {
    }

    public record StudentUpsertRequest(
        @NotBlank String name,
        @NotBlank String surname,
        @NotBlank @Email String email,
        @NotBlank String studentIdentifier,
        Gender gender
    ) {
    }

    public record ImportStudentsResponse(
        int importedCount,
        List<UserResponse> students
    ) {
    }

    public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
    ) {
    }

    public record UploadedFileResponse(
        String fileName,
        String url,
        String contentType,
        long size
    ) {
    }

    public record FloorUpdateRequest(
        @NotNull Boolean active
    ) {
    }

    public record FloorResponse(
        UUID id,
        Block block,
        Integer floorNumber,
        boolean active
    ) {
    }

    public record CreatePostRequest(
        @NotBlank String title,
        @NotBlank @Size(max = 4000) String description,
        List<@NotBlank String> photoUrls
    ) {
    }

    public record CommentRequest(
        @NotBlank @Size(max = 2000) String content,
        UUID parentCommentId
    ) {
    }

    public record ChatMessageRequest(
        @NotBlank @Size(max = 2000) String content,
        UUID parentMessageId
    ) {
    }

    public record MealSlotBatchRequest(
        @NotNull LocalDate slotDate,
        @NotNull MealType mealType,
        @NotNull LocalTime startTime,
        @Min(1) @Max(24) int slotCount,
        @Min(1) @Max(180) int durationMinutes,
        @Positive int capacity
    ) {
    }

    public record MealSlotUpdateRequest(
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        Boolean active
    ) {
    }

    public record MealRegistrationRuleRequest(
        @NotNull LocalDate registrationDate,
        @NotNull MealType mealType,
        @NotNull Boolean active
    ) {
    }

    public record MealRegistrationRuleResponse(
        MealType mealType,
        Gender genderScope,
        LocalDate registrationDate,
        boolean active
    ) {
    }

    public record DormRegistrationMealOptionRequest(
        @NotNull MealType mealType,
        @NotNull Boolean available,
        @NotNull Boolean includedInPrice
    ) {
    }

    public record DormRegistrationSettingsRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        List<@Valid DormRegistrationMealOptionRequest> mealOptions
    ) {
    }

    public record DormRegistrationMealOptionResponse(
        MealType mealType,
        boolean available,
        boolean includedInPrice
    ) {
    }

    public record DormRegistrationSettingsResponse(
        LocalDate startDate,
        LocalDate endDate,
        boolean activeNow,
        List<DormRegistrationMealOptionResponse> mealOptions
    ) {
    }

    public record RoomSelectionRequest(
        @NotNull UUID roomId,
        List<@NotNull MealType> mealTypesIncludedInPrice
    ) {
    }
}
