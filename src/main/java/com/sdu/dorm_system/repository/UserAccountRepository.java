package com.sdu.dorm_system.repository;

import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByStudentIdentifier(String studentIdentifier);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStudentIdentifier(String studentIdentifier);

    List<UserAccount> findAllByRoleInOrderByRoleAsc(List<Role> roles);

    Page<UserAccount> findByRole(Role role, Pageable pageable);

    Page<UserAccount> findByRoleAndGender(Role role, Gender gender, Pageable pageable);
}
