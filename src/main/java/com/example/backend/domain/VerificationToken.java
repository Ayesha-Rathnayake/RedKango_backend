package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "verification_token")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    /**
     * CHANGED from @OneToOne to @ManyToOne.
     *
     * Why: @OneToOne(fetch=EAGER) caused Hibernate to load the User into
     * its first-level (session) cache when the token was loaded.
     * Later, userRepo.findById() returned that SAME cached proxy — still
     * showing enabled=false — so setEnabled(true) + save() never produced
     * an UPDATE because Hibernate's dirty-check compared against the stale
     * cached snapshot.
     *
     * @ManyToOne still correctly models the relationship (one user can have
     * multiple tokens over time, e.g. after resend), and it avoids the
     * Hibernate first-level cache clash.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}