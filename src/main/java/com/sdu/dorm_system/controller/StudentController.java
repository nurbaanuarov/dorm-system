package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Block;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.service.CurrentUserService;
import com.sdu.dorm_system.service.DormRegistrationService;
import com.sdu.dorm_system.service.MealService;
import com.sdu.dorm_system.service.MealPlanService;
import com.sdu.dorm_system.service.PaginationUtils;
import com.sdu.dorm_system.service.PostService;
import com.sdu.dorm_system.service.RoomService;
import com.sdu.dorm_system.service.StudentChatService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final CurrentUserService currentUserService;
    private final RoomService roomService;
    private final DormRegistrationService dormRegistrationService;
    private final MealService mealService;
    private final MealPlanService mealPlanService;
    private final PostService postService;
    private final StudentChatService studentChatService;

    @GetMapping("/rooms")
    public ApiModels.PageResponse<RoomService.RoomAvailability> listRooms(
        @RequestParam Block block,
        @RequestParam Integer floor,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        Pageable pageable = PaginationUtils.pageable(page, size, Sort.by(Sort.Direction.ASC, "roomNumber"));
        return ApiModels.toPageResponse(roomService.listRoomsForStudent(actor, block, floor, pageable));
    }

    @PostMapping("/room-selection")
    public RoomService.RoomAssignment selectRoom(
        @Valid @RequestBody ApiModels.RoomSelectionRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return roomService.assignRoom(
            actor,
            request.roomId(),
            request.mealTypesIncludedInPrice() == null ? List.of() : request.mealTypesIncludedInPrice()
        );
    }

    @GetMapping("/room-selection")
    public ApiModels.RoomAssignmentResponse currentRoom(Authentication authentication) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        RoomService.RoomAssignment assignment = roomService.getAssignment(actor);
        return assignment == null
            ? null
            : new ApiModels.RoomAssignmentResponse(
                assignment.roomId(),
                assignment.block(),
                assignment.floorNumber(),
                assignment.roomNumber(),
                assignment.bedNumber(),
                assignment.mealTypesIncludedInPrice()
            );
    }

    @GetMapping("/dorm-registration-settings")
    public ApiModels.DormRegistrationSettingsResponse getDormRegistrationSettings() {
        DormRegistrationService.DormRegistrationSettingsView settings = dormRegistrationService.getSettings();
        return settings == null ? null : ApiModels.toDormRegistrationSettingsResponse(settings);
    }

    @GetMapping("/meal-registration-rules")
    public ApiModels.PageResponse<ApiModels.MealRegistrationRuleResponse> listMealRegistrationRules(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDate,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        Pageable pageable = PaginationUtils.pageable(page, size, Sort.by(Sort.Direction.ASC, "mealType"));
        return ApiModels.toPageResponse(mealPlanService.listRegistrationRulesForStudent(actor, registrationDate, pageable), rule ->
            new ApiModels.MealRegistrationRuleResponse(
                rule.mealType(),
                rule.genderScope(),
                rule.registrationDate(),
                rule.active()
            )
        );
    }

    @GetMapping("/meal-slots")
    public ApiModels.PageResponse<MealService.MealSlotView> listMealSlots(
        @RequestParam MealType mealType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        Pageable pageable = PaginationUtils.pageable(page, size, Sort.by(Sort.Direction.ASC, "startTime"));
        return ApiModels.toPageResponse(mealService.listSlotsForStudent(actor, mealType, slotDate, pageable));
    }

    @PostMapping("/meal-bookings/{slotId}")
    public MealService.MealSlotView bookMealSlot(
        @PathVariable UUID slotId,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return mealService.bookSlot(actor, slotId);
    }

    @GetMapping("/posts")
    public ApiModels.PageResponse<PostService.PostView> listPosts(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        Pageable pageable = PaginationUtils.pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiModels.toPageResponse(postService.listVisiblePosts(actor, pageable));
    }

    @PostMapping("/posts/{postId}/comments")
    public PostService.PostView addComment(
        @PathVariable UUID postId,
        @Valid @RequestBody ApiModels.CommentRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return postService.addComment(actor, postId, new PostService.AddCommentCommand(request.content(), request.parentCommentId()));
    }

    @GetMapping("/global-chat/messages")
    public ApiModels.PageResponse<StudentChatService.ChatMessageView> listGlobalChatMessages(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        Pageable pageable = PaginationUtils.pageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiModels.toPageResponse(studentChatService.listMessages(actor, pageable));
    }

    @PostMapping("/global-chat/messages")
    public StudentChatService.ChatMessageView addGlobalChatMessage(
        @Valid @RequestBody ApiModels.ChatMessageRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return studentChatService.addMessage(
            actor,
            new StudentChatService.CreateChatMessageCommand(request.content(), request.parentMessageId())
        );
    }
}
