package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Block;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.service.CurrentUserService;
import com.sdu.dorm_system.service.MealService;
import com.sdu.dorm_system.service.MealPlanService;
import com.sdu.dorm_system.service.PostService;
import com.sdu.dorm_system.service.RoomService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
    private final MealService mealService;
    private final MealPlanService mealPlanService;
    private final PostService postService;

    @GetMapping("/rooms")
    public List<RoomService.RoomAvailability> listRooms(
        @RequestParam Block block,
        @RequestParam Integer floor,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return roomService.listRoomsForStudent(actor, block, floor);
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

    @GetMapping("/meal-registration-rules")
    public List<ApiModels.MealRegistrationRuleResponse> listMealRegistrationRules(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDate,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return mealPlanService.listRegistrationRulesForStudent(actor, registrationDate)
            .stream()
            .map(rule -> new ApiModels.MealRegistrationRuleResponse(
                rule.mealType(),
                rule.genderScope(),
                rule.registrationDate(),
                rule.active()
            ))
            .toList();
    }

    @GetMapping("/meal-slots")
    public List<MealService.MealSlotView> listMealSlots(
        @RequestParam MealType mealType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return mealService.listSlotsForStudent(actor, mealType, slotDate);
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
    public List<PostService.PostView> listPosts(Authentication authentication) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return postService.listVisiblePosts(actor);
    }

    @PostMapping("/posts/{postId}/comments")
    public PostService.PostView addComment(
        @PathVariable UUID postId,
        @Valid @RequestBody ApiModels.CommentRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return postService.addComment(actor, postId, request.content());
    }
}
