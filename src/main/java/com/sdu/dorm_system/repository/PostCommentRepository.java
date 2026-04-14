package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.PostComment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
}
