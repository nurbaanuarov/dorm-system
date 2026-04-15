package com.sdu.dorm_system.controller;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.MealType;
import com.sdu.dorm_system.service.CurrentUserService;
import com.sdu.dorm_system.service.MealService;
import com.sdu.dorm_system.service.MealPlanService;
import com.sdu.dorm_system.service.PaginationUtils;
import com.sdu.dorm_system.service.PostService;
import com.sdu.dorm_system.service.PostImageStorageService;
import com.sdu.dorm_system.service.StudentImportService;
import com.sdu.dorm_system.service.UserManagementService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CurrentUserService currentUserService;
    private final UserManagementService userManagementService;
    private final StudentImportService studentImportService;
    private final MealService mealService;
    private final MealPlanService mealPlanService;
    private final PostService postService;
    private final PostImageStorageService postImageStorageService;

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

    @PostMapping("/post-images")
    public ApiModels.UploadedFileResponse uploadPostImage(
        @RequestPart("file") MultipartFile file,
        Authentication authentication
    ) {
        currentUserService.getCurrentUser(authentication);
        PostImageStorageService.StoredImage storedImage = postImageStorageService.storePostImage(file);

        return new ApiModels.UploadedFileResponse(
            storedImage.fileName(),
            storedImage.publicUrl(),
            storedImage.contentType(),
            storedImage.size()
        );
    }

    @PostMapping("/meal-slots/batches")
    public List<MealService.MealSlotView> createMealSlots(
        @Valid @RequestBody ApiModels.MealSlotBatchRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return mealService.createSlots(actor, new MealService.CreateSlotsCommand(
            request.slotDate(),
            request.mealType(),
            request.startTime(),
            request.slotCount(),
            request.durationMinutes(),
            request.capacity()
        ));
    }

    @PatchMapping("/meal-slots/{slotId}")
    public MealService.MealSlotView updateMealSlot(
        @PathVariable UUID slotId,
        @Valid @RequestBody ApiModels.MealSlotUpdateRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return mealService.updateSlot(actor, slotId, new MealService.UpdateSlotCommand(
            request.startTime(),
            request.endTime(),
            request.capacity(),
            request.active()
        ));
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
        return ApiModels.toPageResponse(mealService.listSlotsForAdmin(actor, mealType, slotDate, pageable));
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
        return ApiModels.toPageResponse(mealPlanService.listRegistrationRulesForAdmin(actor, registrationDate, pageable), rule ->
            new ApiModels.MealRegistrationRuleResponse(
                rule.mealType(),
                rule.genderScope(),
                rule.registrationDate(),
                rule.active()
            )
        );
    }

    @PutMapping("/meal-registration-rules")
    public ApiModels.MealRegistrationRuleResponse upsertMealRegistrationRule(
        @Valid @RequestBody ApiModels.MealRegistrationRuleRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        MealPlanService.MealRegistrationRuleView rule = mealPlanService.upsertRegistrationRule(
            actor,
            new MealPlanService.UpsertMealRegistrationRuleCommand(
                request.mealType(),
                request.registrationDate(),
                request.active()
            )
        );

        return new ApiModels.MealRegistrationRuleResponse(
            rule.mealType(),
            rule.genderScope(),
            rule.registrationDate(),
            rule.active()
        );
    }

    @PostMapping("/posts")
    public PostService.PostView createPost(
        @Valid @RequestBody ApiModels.CreatePostRequest request,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return createGenderPost(actor, request.title(), request.description(), request.photoUrls(), List.of());
    }

    @PostMapping(path = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostService.PostView createPostWithImages(
        @RequestPart("title") String title,
        @RequestPart("description") String description,
        @RequestPart(value = "photoUrls", required = false) List<String> photoUrls,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) {
        UserAccount actor = currentUserService.getCurrentUser(authentication);
        return createGenderPost(actor, title, description, photoUrls, files);
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

    private PostService.PostView createGenderPost(
        UserAccount actor,
        String title,
        String description,
        List<String> photoUrls,
        List<MultipartFile> files
    ) {
        List<String> uploadedPhotoUrls = postImageStorageService.storePostImages(files);
        List<String> mergedPhotoUrls = mergePhotoUrls(photoUrls, uploadedPhotoUrls);
        return postService.createGenderPost(
            actor,
            new PostService.CreatePostCommand(title, description, mergedPhotoUrls)
        );
    }

    private List<String> mergePhotoUrls(List<String> photoUrls, List<String> uploadedPhotoUrls) {
        List<String> existingUrls = photoUrls == null ? List.of() : photoUrls.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
        if (uploadedPhotoUrls == null || uploadedPhotoUrls.isEmpty()) {
            return existingUrls;
        }
        return java.util.stream.Stream.concat(existingUrls.stream(), uploadedPhotoUrls.stream()).toList();
    }
}
