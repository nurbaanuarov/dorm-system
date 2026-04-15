package com.sdu.dorm_system.service;

import com.sdu.dorm_system.config.AppProperties;
import com.sdu.dorm_system.domain.BedUnit;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.domain.enums.Gender;
import com.sdu.dorm_system.domain.enums.Role;
import com.sdu.dorm_system.exception.BusinessException;
import com.sdu.dorm_system.repository.BedUnitRepository;
import com.sdu.dorm_system.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final UserAccountRepository userAccountRepository;
    private final BedUnitRepository bedUnitRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public Page<UserAccount> listGenderAdmins(Pageable pageable) {
        return PaginationUtils.pageList(
            userAccountRepository.findAllByRoleInOrderByRoleAsc(List.of(Role.BOYS_ADMIN, Role.GIRLS_ADMIN)),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> listStudents(UserAccount actor, Pageable pageable) {
        if (actor.getRole() == Role.LEAD_ADMIN) {
            return userAccountRepository.findByRole(Role.STUDENT, pageable);
        }

        if (actor.getRole().isGenderAdmin()) {
            return userAccountRepository.findByRoleAndGender(Role.STUDENT, actor.getRole().managedGender(), pageable);
        }

        throw BusinessException.forbidden("Only dorm admins can view the students list");
    }

    @Transactional
    public void ensureBootstrapAdmin(Role role, AppProperties.AdminSeed seed) {
        userAccountRepository.findByEmailIgnoreCase(seed.email())
            .orElseGet(() -> {
                UserAccount account = new UserAccount();
                account.setEmail(normalizeEmail(seed.email()));
                account.setRole(role);
                account.setName(seed.name().trim());
                account.setSurname(seed.surname().trim());
                account.setGender(role.managedGender());
                account.setPasswordHash(passwordEncoder.encode(seed.password()));
                account.setEnabled(true);
                return userAccountRepository.save(account);
            });
    }

    @Transactional
    public UserAccount createStudent(StudentUpsertCommand command, UserAccount actor) {
        Gender effectiveGender = resolveStudentGender(command.gender(), actor, false);
        validateStudentUniqueness(command.email(), command.studentIdentifier(), null);

        UserAccount student = new UserAccount();
        student.setRole(Role.STUDENT);
        student.setName(command.name().trim());
        student.setSurname(command.surname().trim());
        student.setEmail(normalizeEmail(command.email()));
        student.setStudentIdentifier(command.studentIdentifier().trim());
        student.setGender(effectiveGender);
        student.setEnabled(true);

        String rawPassword = generateTemporaryPassword();
        student.setPasswordHash(passwordEncoder.encode(rawPassword));

        UserAccount saved = userAccountRepository.save(student);
        notificationService.sendTemporaryPassword(saved, rawPassword, "Your student account has been created in the SDU Dorm System.");
        return saved;
    }

    @Transactional
    public UserAccount updateStudent(UUID studentId, StudentUpsertCommand command, UserAccount actor) {
        requireRole(actor, Role.LEAD_ADMIN);

        UserAccount student = userAccountRepository.findById(studentId)
            .orElseThrow(() -> BusinessException.notFound("Student was not found"));

        if (student.getRole() != Role.STUDENT) {
            throw BusinessException.badRequest("Only student accounts can be updated here");
        }

        Gender newGender = resolveStudentGender(command.gender(), actor, true);
        BedUnit assignedBed = bedUnitRepository.findByOccupantId(studentId).orElse(null);

        if (assignedBed != null && !assignedBed.getRoom().getFloor().getBlock().supportsGender(newGender)) {
            throw BusinessException.conflict("The student is already assigned to a room in a block that does not match the new gender");
        }

        validateStudentUniqueness(command.email(), command.studentIdentifier(), studentId);

        boolean emailChanged = !student.getEmail().equalsIgnoreCase(command.email().trim());

        student.setName(command.name().trim());
        student.setSurname(command.surname().trim());
        student.setEmail(normalizeEmail(command.email()));
        student.setStudentIdentifier(command.studentIdentifier().trim());
        student.setGender(newGender);

        if (emailChanged) {
            String rawPassword = generateTemporaryPassword();
            student.setPasswordHash(passwordEncoder.encode(rawPassword));
            notificationService.sendTemporaryPassword(student, rawPassword, "Your login email was updated in the SDU Dorm System.");
        }

        return userAccountRepository.save(student);
    }

    @Transactional
    public UserAccount updateGenderAdmin(UUID adminId, AdminUpdateCommand command, UserAccount actor) {
        requireRole(actor, Role.LEAD_ADMIN);

        UserAccount admin = userAccountRepository.findById(adminId)
            .orElseThrow(() -> BusinessException.notFound("Admin was not found"));

        if (!admin.getRole().isGenderAdmin()) {
            throw BusinessException.badRequest("Only boys and girls admins can be updated from this endpoint");
        }

        validateAdminUniqueness(command.email(), adminId);

        admin.setName(command.name().trim());
        admin.setSurname(command.surname().trim());
        admin.setEmail(normalizeEmail(command.email()));

        String rawPassword = generateTemporaryPassword();
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        UserAccount saved = userAccountRepository.save(admin);
        notificationService.sendTemporaryPassword(saved, rawPassword, "Your administrator credentials were updated in the SDU Dorm System.");
        return saved;
    }

    private void validateStudentUniqueness(String email, String studentIdentifier, UUID currentUserId) {
        validateAdminUniqueness(email, currentUserId);

        userAccountRepository.findByStudentIdentifier(studentIdentifier.trim())
            .filter(existing -> !existing.getId().equals(currentUserId))
            .ifPresent(existing -> {
                throw BusinessException.conflict("A student with this ID already exists");
            });
    }

    private void validateAdminUniqueness(String email, UUID currentUserId) {
        userAccountRepository.findByEmailIgnoreCase(email.trim())
            .filter(existing -> !existing.getId().equals(currentUserId))
            .ifPresent(existing -> {
                throw BusinessException.conflict("A user with this email already exists");
            });
    }

    private Gender resolveStudentGender(Gender requestedGender, UserAccount actor, boolean requireExplicitForLeadAdmin) {
        if (actor.getRole() == Role.LEAD_ADMIN) {
            if (requestedGender == null && requireExplicitForLeadAdmin) {
                throw BusinessException.badRequest("Gender is required");
            }

            if (requestedGender == null) {
                throw BusinessException.badRequest("Gender is required");
            }

            return requestedGender;
        }

        if (!actor.getRole().isGenderAdmin()) {
            throw BusinessException.forbidden("Only dorm admins can manage students");
        }

        Gender managedGender = actor.getRole().managedGender();
        if (requestedGender != null && requestedGender != managedGender) {
            throw BusinessException.badRequest("This admin can only manage " + managedGender.name().toLowerCase() + " students");
        }

        return managedGender;
    }

    private void requireRole(UserAccount actor, Role role) {
        if (actor.getRole() != role) {
            throw BusinessException.forbidden("This action requires the " + role.name() + " role");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < 12; index++) {
            builder.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }

        return builder.toString();
    }

    public record StudentUpsertCommand(
        String name,
        String surname,
        String email,
        String studentIdentifier,
        Gender gender
    ) {
    }

    public record AdminUpdateCommand(
        String name,
        String surname,
        String email
    ) {
    }
}
