package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.PostItem;
import com.sdu.dorm_system.domain.enums.PostAudience;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostItemRepository extends JpaRepository<PostItem, UUID> {

    Page<PostItem> findAllByAudienceIn(Collection<PostAudience> audiences, Pageable pageable);
}
