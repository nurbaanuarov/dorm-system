package com.sdu.dorm_system.service;

import com.sdu.dorm_system.domain.PostComment;
import com.sdu.dorm_system.domain.PostItem;
import com.sdu.dorm_system.domain.PostPhoto;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.PostAudience;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.PostCommentRepository;
import com.sdu.dorm_system.repository.PostItemRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostItemRepository postItemRepository;
    private final PostCommentRepository postCommentRepository;

    @Transactional
    public PostView createLeadPost(UserAccount actor, CreatePostCommand command) {
        if (actor.getRole() != Role.LEAD_ADMIN) {
            throw BusinessException.forbidden("Only the lead admin can create posts for all students");
        }

        return createPost(actor, command, PostAudience.ALL);
    }

    @Transactional
    public PostView createGenderPost(UserAccount actor, CreatePostCommand command) {
        PostAudience audience = switch (actor.getRole()) {
            case BOYS_ADMIN -> PostAudience.BOYS;
            case GIRLS_ADMIN -> PostAudience.GIRLS;
            default -> throw BusinessException.forbidden("Only boys and girls admins can create gender-specific posts");
        };

        return createPost(actor, command, audience);
    }

    @Transactional(readOnly = true)
    public Page<PostView> listVisiblePosts(UserAccount actor, Pageable pageable) {
        return postItemRepository.findAllByAudienceIn(resolveAudiences(actor), pageable)
            .map(this::toView);
    }

    @Transactional
    public PostView addComment(UserAccount actor, UUID postId, AddCommentCommand command) {
        PostItem postItem = getVisiblePost(actor, postId);
        PostComment parentComment = resolveParentComment(postItem, command.parentCommentId());

        PostComment comment = new PostComment();
        comment.setPost(postItem);
        comment.setAuthor(actor);
        comment.setParentComment(parentComment);
        comment.setContent(command.content().trim());
        postCommentRepository.save(comment);

        return toView(postItemRepository.findById(postId)
            .orElseThrow(() -> BusinessException.notFound("Post was not found")));
    }

    private PostView createPost(UserAccount actor, CreatePostCommand command, PostAudience audience) {
        PostItem postItem = new PostItem();
        postItem.setTitle(normalizeRequiredText(command.title(), "Post title is required"));
        postItem.setDescription(normalizeRequiredText(command.description(), "Post description is required"));
        postItem.setAudience(audience);
        postItem.setCreatedBy(actor);

        int order = 0;
        for (String photoUrl : normalizePhotoUrls(command.photoUrls())) {
            PostPhoto photo = new PostPhoto();
            photo.setPost(postItem);
            photo.setPhotoUrl(photoUrl);
            photo.setSortOrder(order++);
            postItem.getPhotos().add(photo);
        }

        return toView(postItemRepository.save(postItem));
    }

    private PostItem getVisiblePost(UserAccount actor, UUID postId) {
        PostItem postItem = postItemRepository.findById(postId)
            .orElseThrow(() -> BusinessException.notFound("Post was not found"));

        if (!resolveAudiences(actor).contains(postItem.getAudience())) {
            throw BusinessException.forbidden("This post is not visible to the current user");
        }

        return postItem;
    }

    private PostComment resolveParentComment(PostItem postItem, UUID parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        PostComment parentComment = postCommentRepository.findById(parentCommentId)
            .orElseThrow(() -> BusinessException.notFound("Parent comment was not found"));

        if (!parentComment.getPost().getId().equals(postItem.getId())) {
            throw BusinessException.badRequest("Parent comment must belong to the same post");
        }

        return parentComment;
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        return value.trim();
    }

    private List<String> normalizePhotoUrls(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return List.of();
        }

        return photoUrls.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }

    private List<PostAudience> resolveAudiences(UserAccount actor) {
        return switch (actor.getRole()) {
            case LEAD_ADMIN -> List.of(PostAudience.ALL, PostAudience.BOYS, PostAudience.GIRLS);
            case BOYS_ADMIN -> List.of(PostAudience.ALL, PostAudience.BOYS);
            case GIRLS_ADMIN -> List.of(PostAudience.ALL, PostAudience.GIRLS);
            case STUDENT -> actor.getGender() == null || actor.getGender() == Gender.MALE
                ? List.of(PostAudience.ALL, PostAudience.BOYS)
                : List.of(PostAudience.ALL, PostAudience.GIRLS);
        };
    }

    private PostView toView(PostItem postItem) {
        return new PostView(
            postItem.getId(),
            postItem.getTitle(),
            postItem.getDescription(),
            postItem.getAudience(),
            postItem.getPhotos().stream().map(PostPhoto::getPhotoUrl).toList(),
            postItem.getCreatedAt(),
            postItem.getCreatedBy().getName() + " " + postItem.getCreatedBy().getSurname(),
            postItem.getComments().stream()
                .filter(comment -> comment.getParentComment() == null)
                .sorted(Comparator.comparing(PostComment::getCreatedAt).reversed())
                .map(this::toCommentView)
                .toList()
        );
    }

    private PostCommentView toCommentView(PostComment comment) {
        return new PostCommentView(
            comment.getId(),
            comment.getParentComment() == null ? null : comment.getParentComment().getId(),
            comment.getAuthor().getName() + " " + comment.getAuthor().getSurname(),
            comment.getAuthor().getRole(),
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getReplies().stream()
                .sorted(Comparator.comparing(PostComment::getCreatedAt))
                .map(this::toCommentView)
                .toList()
        );
    }

    public record CreatePostCommand(
        String title,
        String description,
        List<String> photoUrls
    ) {
    }

    public record AddCommentCommand(
        String content,
        UUID parentCommentId
    ) {
    }

    public record PostView(
        UUID id,
        String title,
        String description,
        PostAudience audience,
        List<String> photoUrls,
        java.time.OffsetDateTime createdAt,
        String createdBy,
        List<PostCommentView> comments
    ) {
    }

    public record PostCommentView(
        UUID id,
        UUID parentCommentId,
        String authorName,
        Role authorRole,
        String content,
        java.time.OffsetDateTime createdAt,
        List<PostCommentView> replies
    ) {
    }
}
