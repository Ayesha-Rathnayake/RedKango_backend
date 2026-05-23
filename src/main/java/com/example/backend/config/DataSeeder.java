package com.example.backend.config;

import com.example.backend.domain.Role;
import com.example.backend.domain.User;
import com.example.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Runs once on every application startup.
 *
 * - Creates the default admin account if it doesn't already exist.
 * - Safe to run repeatedly — it checks before inserting.
 * - Credentials are read from application.properties (or environment variables),
 *   never hardcoded here.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DataSeeder(UserRepository userRepo,
                      PasswordEncoder passwordEncoder,
                      AppProperties props) {
        this.userRepo        = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.props           = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
    }

    private void seedAdmin() {
        String adminEmail = props.getAdmin().getEmail();

        if (userRepo.existsByEmail(adminEmail)) {
            log.info("Admin account already exists — skipping seed.");
            return;
        }

        User admin = new User();
        admin.setFirstName(props.getAdmin().getFirstName());
        admin.setLastName(props.getAdmin().getLastName());
        admin.setEmail(adminEmail);
        admin.setPhone(props.getAdmin().getPhone());
        admin.setPassword(passwordEncoder.encode(props.getAdmin().getPassword()));
        admin.setRoles(Set.of(Role.ROLE_ADMIN));
        admin.setEnabled(true);   // admin is pre-verified — no email confirmation needed
        admin.setLocked(false);

        userRepo.save(admin);
        log.info("Default admin account created: {}", adminEmail);
    }
}