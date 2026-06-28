package com.example.backend.service;

import com.example.backend.config.AppProperties;
import com.example.backend.domain.*;
import com.example.backend.dto.*;
import com.example.backend.repository.*;
import com.example.backend.security.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

    private final UserRepository          userRepo;
    private final VerificationTokenRepository verifyRepo;
    private final PasswordResetTokenRepository resetRepo;
    private final RefreshTokenRepository  refreshRepo;
    private final PasswordEncoder         encoder;
    private final AuthenticationManager   authManager;
    private final JwtService              jwt;
    private final EmailService            email;
    private final AppProperties           props;
    private final PasswordPolicyService   policy;
    private final EntityManager           em;
    private final AdminNotificationService adminNotificationService;
    // ← needed to evict stale cache

    public AuthService(UserRepository userRepo,
                       VerificationTokenRepository verifyRepo,
                       PasswordResetTokenRepository resetRepo,
                       RefreshTokenRepository refreshRepo,
                       PasswordEncoder encoder,
                       AuthenticationManager authManager,
                       JwtService jwt,
                       EmailService email,
                       AppProperties props,
                       PasswordPolicyService policy,
                       EntityManager em,
                       AdminNotificationService adminNotificationService) {

        this.userRepo    = userRepo;
        this.verifyRepo  = verifyRepo;
        this.resetRepo   = resetRepo;
        this.refreshRepo = refreshRepo;
        this.encoder     = encoder;
        this.authManager = authManager;
        this.jwt         = jwt;
        this.email       = email;
        this.props       = props;
        this.policy      = policy;
        this.em          = em;
        this.adminNotificationService = adminNotificationService;
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        policy.validateOrThrow(req.getPassword());

        User user = new User();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRoles(Set.of(Role.ROLE_CUSTOMER));
        user.setEnabled(false);

        // saveAndFlush so the user row exists in DB before the FK token row is inserted
        userRepo.saveAndFlush(user);

        adminNotificationService.push(
                AdminNotification.NotificationType.NEW_USER,
                "New User: " + user.getFirstName() + " " + user.getLastName() + " | " + user.getEmail()
        );


        VerificationToken vt = new VerificationToken();
        vt.setToken(UUID.randomUUID().toString());
        vt.setUser(user);
        vt.setUsed(false);
        vt.setExpiresAt(Instant.now().plusSeconds(props.getVerification().getTtlMinutes() * 60L));
        verifyRepo.saveAndFlush(vt);

        email.sendVerificationEmail(user.getEmail(), vt.getToken());
    }

    // ─── VERIFY EMAIL ─────────────────────────────────────────────────────────

    @Transactional
    public void verifyEmail(String token) {

        VerificationToken vt = verifyRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        // Check expiry BEFORE used, so we can distinguish the two cases
        if (vt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        if (vt.isUsed()) {
            // Idempotent: already verified — treat as success
            throw new IllegalStateException("Token already used");
        }

        vt.setUsed(true);
        verifyRepo.saveAndFlush(vt);


        Long userId = vt.getUser().getId();
        em.detach(vt.getUser());


        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        user.setEnabled(true);

        userRepo.saveAndFlush(user);
    }

    // ─── RESEND VERIFICATION ─────────────────────────────────────────────────

    @Transactional
    public void resendVerification(String emailAddr) {

        User user = userRepo.findByEmail(emailAddr)
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email address"));

        if (user.isEnabled()) {
            throw new IllegalStateException("Account is already verified");
        }

        verifyRepo.deleteByUser_Id(user.getId());
        em.flush(); // ensure deletes are written before the insert

        VerificationToken vt = new VerificationToken();
        vt.setToken(UUID.randomUUID().toString());
        vt.setUser(user);
        vt.setUsed(false);
        vt.setExpiresAt(Instant.now().plusSeconds(
                props.getVerification().getTtlMinutes() * 60L));
        verifyRepo.saveAndFlush(vt);

        email.sendVerificationEmail(user.getEmail(), vt.getToken());
    }

    // ─── GET EMAIL FOR EXPIRED TOKEN (used by controller redirect) ────────────

    /**
     * Looks up the email even for an expired/used token so the controller
     */
    public String getEmailForToken(String token) {
        return verifyRepo.findByToken(token)
                .map(vt -> vt.getUser().getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepo.findByEmail(req.getEmail()).orElseThrow();
        if (user.isDeleted()) {
            throw new IllegalArgumentException("This account has been deleted");
        }
        boolean rememberMe = Boolean.TRUE.equals(req.getRememberMe());

        List<String> roleNames = user.getRoles().stream()
                .map(Enum::name)
                .toList();

        String accessToken  = jwt.generateAccessToken(user.getEmail(), roleNames);
        String refreshToken = jwt.generateRefreshToken(user.getEmail(), rememberMe);

        int days = rememberMe
                ? props.getJwt().getRefreshTokenTtlDaysRememberMe()
                : props.getJwt().getRefreshTokenTtlDays();

        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshToken);
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plusSeconds(days * 24L * 3600L));
        rt.setRevoked(false);
        refreshRepo.save(rt);

        AuthResponse res = new AuthResponse();
        res.setAccessToken(accessToken);
        res.setRefreshToken(refreshToken);
        res.setEmail(user.getEmail());
        res.setFullName(user.getFirstName() + " " + user.getLastName());
        res.setRoles(roleNames);
        return res;
    }

    // ─── FORGOT PASSWORD ─────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(String emailAddr) {
        User user = userRepo.findByEmail(emailAddr)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(UUID.randomUUID().toString());
        prt.setExpiresAt(Instant.now().plusSeconds(props.getReset().getTtlMinutes() * 60L));
        resetRepo.save(prt);

        email.sendPasswordResetEmail(user.getEmail(), prt.getToken());
    }

    // ─── RESET PASSWORD ──────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(String token, String newPassword) {
        policy.validateOrThrow(newPassword);

        PasswordResetToken prt = resetRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset token expired or already used");
        }

        User user = prt.getUser();
        user.setPassword(encoder.encode(newPassword));
        prt.setUsed(true);
        userRepo.saveAndFlush(user);
        resetRepo.saveAndFlush(prt);

        refreshRepo.deleteByUser_Id(user.getId());
    }

    // ─── TOKEN REFRESH ────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        if (!jwt.isValid(refreshTokenStr)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        RefreshToken dbToken = refreshRepo.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (dbToken.isRevoked() || dbToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        String userEmail = jwt.extractSubject(refreshTokenStr);
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        List<String> roleNames = user.getRoles().stream().map(Enum::name).toList();

        String newAccessToken  = jwt.generateAccessToken(userEmail, roleNames);
        String newRefreshToken = jwt.generateRefreshToken(userEmail, false);

        dbToken.setRevoked(true);
        refreshRepo.save(dbToken);

        RefreshToken newRt = new RefreshToken();
        newRt.setToken(newRefreshToken);
        newRt.setUser(user);
        newRt.setExpiresAt(jwt.extractExpiry(newRefreshToken).toInstant());
        newRt.setRevoked(false);
        refreshRepo.save(newRt);

        AuthResponse res = new AuthResponse();
        res.setAccessToken(newAccessToken);
        res.setRefreshToken(newRefreshToken);
        res.setEmail(user.getEmail());
        res.setFullName(user.getFirstName() + " " + user.getLastName());
        res.setRoles(roleNames);
        return res;
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshRepo.findByToken(refreshTokenStr).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshRepo.save(rt);
        });
    }
}