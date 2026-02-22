package com.example.backend.service;

import com.example.backend.config.AppProperties;
import com.example.backend.domain.*;
import com.example.backend.dto.*;
import com.example.backend.repository.*;
import com.example.backend.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final VerificationTokenRepository verifyRepo;
    private final PasswordResetTokenRepository resetRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final EmailService email;
    private final AppProperties props;
    private final PasswordPolicyService policy;

    public AuthService(UserRepository userRepo,
                       VerificationTokenRepository verifyRepo,
                       PasswordResetTokenRepository resetRepo,
                       RefreshTokenRepository refreshRepo,
                       PasswordEncoder encoder,
                       AuthenticationManager authManager,
                       JwtService jwt,
                       EmailService email,
                       AppProperties props,
                       PasswordPolicyService policy) {
        this.userRepo = userRepo;
        this.verifyRepo = verifyRepo;
        this.resetRepo = resetRepo;
        this.refreshRepo = refreshRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.email = email;
        this.props = props;
        this.policy = policy;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        // StrongPassword annotation also validated, but double-safety:
        policy.validateOrThrow(req.getPassword());

        User user = new User();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRoles(Set.of(Role.ROLE_CUSTOMER)); // default: customer
        user.setEnabled(false);

        userRepo.save(user);

        VerificationToken vt = new VerificationToken();
        vt.setToken(UUID.randomUUID().toString());
        vt.setUser(user);
        vt.setExpiresAt(Instant.now().plusSeconds(props.getVerification().getTtlMinutes() * 60L));
        verifyRepo.save(vt);

        email.sendVerificationEmail(user.getEmail(), vt.getToken());
    }

    @Transactional
    public void verifyEmail(String token) {
        var vt = verifyRepo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (vt.isUsed() || vt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired or already used");
        }
        var user = vt.getUser();
        user.setEnabled(true);
        vt.setUsed(true);
        userRepo.save(user);
        verifyRepo.save(vt);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        var user = userRepo.findByEmail(req.getEmail()).orElseThrow();
        boolean remember = Boolean.TRUE.equals(req.getRememberMe());

        String accessToken = jwt.generateAccessToken(
                user.getEmail(), user.getRoles().stream().map(Enum::name).toList());

        String refreshTokenStr = jwt.generateRefreshToken(user.getEmail(), remember);

        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshTokenStr);
        rt.setUser(user);
        int days = remember ? props.getJwt().getRefreshTokenTtlDaysRememberMe()
                : props.getJwt().getRefreshTokenTtlDays();
        rt.setExpiresAt(Instant.now().plusSeconds(days * 24L * 3600L));
        rt.setRevoked(false);
        refreshRepo.save(rt);

        AuthResponse res = new AuthResponse();
        res.setAccessToken(accessToken);
        res.setRefreshToken(refreshTokenStr);
        res.setEmail(user.getEmail());
        res.setFullName(user.getFirstName() + " " + user.getLastName());
        return res;
    }

    @Transactional
    public void forgotPassword(String emailAddr) {
        var user = userRepo.findByEmail(emailAddr).orElseThrow(() -> new IllegalArgumentException("Email not found"));
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(UUID.randomUUID().toString());
        prt.setExpiresAt(Instant.now().plusSeconds(props.getReset().getTtlMinutes() * 60L));
        resetRepo.save(prt);
        email.sendPasswordResetEmail(user.getEmail(), prt.getToken());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        // enforce policy on reset as well
        policy.validateOrThrow(newPassword);

        var prt = resetRepo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));
        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        var user = prt.getUser();
        user.setPassword(encoder.encode(newPassword));
        prt.setUsed(true);
        userRepo.save(user);
        resetRepo.save(prt);
        // revoke existing refresh tokens after password change
        refreshRepo.deleteByUser_Id(user.getId());
    }

    // Optional: refresh with rotation
    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        // basic structural validation
        if (!jwt.isValid(refreshTokenStr)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        var dbToken = refreshRepo.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (dbToken.isRevoked() || dbToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        String email = jwt.extractSubject(refreshTokenStr);
        var user = userRepo.findByEmail(email).orElseThrow();

        // issue new pair (rotate)
        String newAccess = jwt.generateAccessToken(email, user.getRoles().stream().map(Enum::name).toList());
        String newRefresh = jwt.generateRefreshToken(email, false); // you can carry rememberMe flag if stored

        // revoke old and save new
        dbToken.setRevoked(true);
        refreshRepo.save(dbToken);

        RefreshToken rt = new RefreshToken();
        rt.setToken(newRefresh);
        rt.setUser(user);
        rt.setExpiresAt(jwt.extractExpiry(newRefresh).toInstant());
        rt.setRevoked(false);
        refreshRepo.save(rt);

        AuthResponse res = new AuthResponse();
        res.setAccessToken(newAccess);
        res.setRefreshToken(newRefresh);
        res.setEmail(user.getEmail());
        res.setFullName(user.getFirstName() + " " + user.getLastName());
        return res;
    }



    @Transactional
    public void logout(String refreshTokenStr) {
        refreshRepo.findByToken(refreshTokenStr).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshRepo.save(rt);
        });
    }

}