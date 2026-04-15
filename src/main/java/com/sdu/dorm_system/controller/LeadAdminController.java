package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.service.CurrentUserService;
import com.sdu.dorm_system.service.DormRegistrationService;
import com.sdu.dorm_system.service.PostService;
import com.sdu.dorm_system.service.RoomService;
import com.sdu.dorm_system.service.StudentImportService;
import com.sdu.dorm_system.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lead-admin")
@RequiredArgsConstructor
public class LeadAdminController {

    private final CurrentUserService currentUserService;
    private final UserManagementService userManagementService;
    private final StudentImportService studentImportService;
    private final DormRegistrationService dormRegistrationService;
    private final PostService postService;
    private final RoomService roomService;

    @GetMapping("/admins")
    public List<ApiModels.UserResponse> listGenderAdmins() {
        return userManagementService.listGenderAdmins().stream()
            .map(ApiModels::toUserResponse)
            .toList();
    }

    @PutMapping("/admins/{adminId}")
    public ApiModels.UserResponse updateGenderAdmin(
        @PathVariable UUID adminId,
        @Valid @RequestBody ApiModels.AdminUpdateRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return ApiModels.toUserResponse(userManagementService.updateGenderAdmin(
            adminId,
            new UserManagementService.AdminUpdateCommand(request.name(), request.surname(), request.email()),
            actor
        ));
    }

    @PostMapping("/students")
    public ApiModels.UserResponse createStudent(
        @Valid @RequestBody ApiModels.StudentUpsertRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return ApiModels.toUserResponse(userManagementService.createStudent(
            new UserManagementService.StudentUpsertCommand(
                request.name(),
                request.surname(),
                request.email(),
                request.studentIdentifier(),
                request.gender()
            ),
            actor
        ));
    }

    @PutMapping("/students/{studentId}")
    public ApiModels.UserResponse updateStudent(
        @PathVariable UUID studentId,
        @Valid @RequestBody ApiModels.StudentUpsertRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return ApiModels.toUserResponse(userManagementService.updateStudent(
            studentId,
            new UserManagementService.StudentUpsertCommand(
                request.name(),
                request.surname(),
                request.email(),
                request.studentIdentifier(),
                request.gender()
            ),
            actor
        ));
    }

    @PostMapping("/students/import")
    public ApiModels.ImportStudentsResponse importStudents(
        @RequestPart("file") MultipartFile file,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        List<ApiModels.UserResponse> students = studentImportService.importStudents(file, actor)
            .stream()
            .map(ApiModels::toUserResponse)
            .toList();

        return new ApiModels.ImportStudentsResponse(students.size(), students);
    }

    @GetMapping("/floors")
    public List<ApiModels.FloorResponse> listFloors() {
        return roomService.listFloors().stream()
            .map(ApiModels::toFloorResponse)
            .toList();
    }

    @PatchMapping("/floors/{floorId}")
    public ApiModels.FloorResponse updateFloor(
        @PathVariable UUID floorId,
        @Valid @RequestBody ApiModels.FloorUpdateRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return ApiModels.toFloorResponse(roomService.updateFloorActiveStatus(actor, floorId, request.active()));
    }

    @GetMapping("/dorm-registration-settings")
    public ApiModels.DormRegistrationSettingsResponse getDormRegistrationSettings() {
        DormRegistrationService.DormRegistrationSettingsView settings = dormRegistrationService.getSettings();
        return settings == null ? null : ApiModels.toDormRegistrationSettingsResponse(settings);
    }

    @PutMapping("/dorm-registration-settings")
    public ApiModels.DormRegistrationSettingsResponse upsertDormRegistrationSettings(
        @Valid @RequestBody ApiModels.DormRegistrationSettingsRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        DormRegistrationService.DormRegistrationSettingsView settings = dormRegistrationService.upsertSettings(
            actor,
            new DormRegistrationService.UpsertDormRegistrationSettingsCommand(
                request.startDate(),
                request.endDate(),
                request.mealOptions() == null
                    ? List.of()
                    : request.mealOptions().stream()
                        .map(option -> new DormRegistrationService.MealAvailabilityCommand(
                            option.mealType(),
                            option.available(),
                            option.includedInPrice()
                        ))
                        .toList()
            )
        );

        return ApiModels.toDormRegistrationSettingsResponse(settings);
    }

    @PostMapping("/posts")
    public PostService.PostView createPost(
        @Valid @RequestBody ApiModels.CreatePostRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return postService.createLeadPost(
            actor,
            new PostService.CreatePostCommand(
                request.title(),
                request.description(),
                request.photoUrls() == null ? List.of() : request.photoUrls()
            )
        );
    }
}
