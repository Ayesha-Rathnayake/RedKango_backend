package com.example.backend.web;

import com.example.backend.domain.Role;
import com.example.backend.domain.User;
import com.example.backend.dto.AdminProfileResponse;
import com.example.backend.dto.AdminUserResponse;
import com.example.backend.dto.ApiMessage;
import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.dto.UpdateAdminProfileRequest;
import com.example.backend.dto.UpdateUserStatusRequest;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.TermsConditionRepository;
import com.example.backend.domain.TermsCondition;
import com.example.backend.dto.TermsConditionRequest;
import com.example.backend.service.EmailService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TermsConditionRepository termsRepository;
    private final EmailService emailService;


    public AdminController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            TermsConditionRepository termsRepository,
            EmailService emailService


    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.termsRepository = termsRepository;
        this.emailService = emailService;

    }

    // ================= ADMIN PROFILE =================

    @GetMapping("/profile")
    public AdminProfileResponse getAdminProfile(Authentication authentication) {
        User admin = getLoggedAdmin(authentication);
        return toAdminProfileResponse(admin);
    }

    @PutMapping("/profile")
    public AdminProfileResponse updateAdminProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateAdminProfileRequest request
    ) {
        User admin = getLoggedAdmin(authentication);

        admin.setFirstName(request.getFirstName().trim());
        admin.setLastName(request.getLastName().trim());
        admin.setPhone(request.getPhone().trim());

        admin.setProfileImageUrl(request.getProfileImageUrl());

        User savedAdmin = userRepository.saveAndFlush(admin);
        return toAdminProfileResponse(savedAdmin);
    }

    @PutMapping("/change-password")
    @Transactional
    public ApiMessage changeAdminPassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User admin = getLoggedAdmin(authentication);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.saveAndFlush(admin);

        refreshTokenRepository.deleteByUser_Id(admin.getId());

        return new ApiMessage("Password changed successfully");
    }

    // ================= USERS MANAGEMENT =================

    @GetMapping("/users")
    public List<AdminUserResponse> getUsers() {
        return userRepository.findByRolesContaining(Role.ROLE_CUSTOMER)
                .stream()
                .map(this::toAdminUserResponse)
                .toList();
    }

    @GetMapping("/users/stats")
    public Map<String, Object> userStats() {

        List<User> users =
                userRepository.findByRolesContaining(Role.ROLE_CUSTOMER);

        long totalUsers = users.size();

        long activeUsers = users.stream()
                .filter(u ->
                        !u.isDeleted()
                                && u.isEnabled()
                                && !u.isLocked())
                .count();

        long inactiveUsers = users.stream()
                .filter(u ->
                        !u.isDeleted()
                                && (!u.isEnabled()
                                || u.isLocked()))
                .count();

        long deactivatedUsers = users.stream()
                .filter(User::isDeleted)
                .count();

        return Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "inactiveUsers", inactiveUsers,
                "deactivatedUsers", deactivatedUsers
        );
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserResponse updateUserStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        User user = getCustomerUser(id);

        if (user.isDeleted()) {
            throw new IllegalArgumentException("Deleted users cannot be updated");
        }

        user.setEnabled(request.isEnabled());
        user.setLocked(!request.isEnabled());

        User savedUser = userRepository.saveAndFlush(user);

        // Send email notification if being deactivated (made inactive)
        if (!request.isEnabled()) {
            String reason = request.getReason() != null ? request.getReason() : "Account suspended";
            String notes = request.getNotes();
            emailService.sendAccountSuspendedEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName() + " " + savedUser.getLastName(),
                    reason,
                    notes
            );
        }

        return toAdminUserResponse(savedUser);
    }


    @DeleteMapping("/users/{id}")
    public Map<String, String> deleteUser(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateUserStatusRequest request
    ) {
        User user = getCustomerUser(id);

        user.setDeleted(true);
        user.setEnabled(false);
        user.setLocked(true);

        userRepository.saveAndFlush(user);

        String reason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "Policy violation";
        String notes = (request != null) ? request.getNotes() : null;

        emailService.sendAccountDeactivatedEmail(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                reason,
                notes
        );

        return Map.of("message", "User deleted successfully");
    }

    @PatchMapping("/users/{id}/restore")
    public AdminUserResponse restoreUser(@PathVariable Long id) {
        User user = getCustomerUser(id);

        if (!user.isDeleted()) {
            throw new IllegalArgumentException(
                    "User is not deactivated"
            );
        }

        user.setDeleted(false);
        user.setLocked(false);
        user.setEnabled(true);

        User savedUser = userRepository.saveAndFlush(user);

        return toAdminUserResponse(savedUser);
    }

    // ================= HELPERS =================

    private User getLoggedAdmin(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (!user.getRoles().contains(Role.ROLE_ADMIN)) {
            throw new IllegalArgumentException("Only admin can access this profile");
        }

        return user;
    }

    private User getCustomerUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getRoles().contains(Role.ROLE_CUSTOMER)) {
            throw new IllegalArgumentException("Only customer users can be managed here");
        }

        return user;
    }

    private AdminProfileResponse toAdminProfileResponse(User admin) {
        return new AdminProfileResponse(
                admin.getId(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getFirstName() + " " + admin.getLastName(),
                admin.getEmail(),
                admin.getPhone(),
                "Admin",
                admin.getProfileImageUrl()
        );
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        String status;

        if (user.isDeleted()) {
            status = "Deactivated";
        } else if (!user.isEnabled() || user.isLocked()) {
            status = "Inactive";
        } else {
            status = "Active";
        }
        return new AdminUserResponse(
                user.getId(),
                "U" + String.format("%03d", user.getId()),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt(),
                user.isEnabled(),
                user.isLocked(),
                status
        );
    }
    // ================= TERMS & CONDITIONS =================

    @GetMapping("/terms")
    public List<TermsCondition> getTerms() {

        return termsRepository.findAll()
                .stream()
                .sorted((a, b) ->
                        b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @PostMapping("/terms")
    public TermsCondition createTerms(
            @RequestBody TermsConditionRequest request
    ) {

        if (request.isActive()) {

            termsRepository.findAll().forEach(t -> {
                t.setActive(false);
                termsRepository.save(t);
            });
        }

        TermsCondition terms = new TermsCondition();

        terms.setTitle(request.getTitle());
        terms.setVersion(request.getVersion());
        terms.setContent(request.getContent());
        terms.setActive(request.isActive());

        return termsRepository.save(terms);
    }

    @PutMapping("/terms/{id}")
    public ResponseEntity<?> updateTerms(
            @PathVariable Long id,
            @RequestBody TermsConditionRequest request
    ) {

        return termsRepository.findById(id)
                .map(terms -> {

                    if (request.isActive()) {

                        termsRepository.findAll().forEach(t -> {
                            t.setActive(false);
                            termsRepository.save(t);
                        });
                    }

                    terms.setTitle(request.getTitle());
                    terms.setVersion(request.getVersion());
                    terms.setContent(request.getContent());
                    terms.setActive(request.isActive());

                    return ResponseEntity.ok(
                            termsRepository.save(terms)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/terms/{id}")
    public ResponseEntity<?> deleteTerms(
            @PathVariable Long id
    ) {

        if (!termsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        termsRepository.deleteById(id);

        return ResponseEntity.ok().build();
    }
}