package com.example.backend;

import com.example.backend.domain.Role;
import com.example.backend.domain.User;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync // <--- enables @Async for EmailService
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> users.findByEmail("admin@yourapp.com").ifPresentOrElse(
                u -> {},
                () -> {
                    User admin = new User();
                    admin.setFirstName("System");
                    admin.setLastName("Admin");
                    admin.setEmail("admin@redkango.com");
                    admin.setPhone("0000000000");
                    admin.setPassword(encoder.encode("Admin#12345"));
                    admin.setEnabled(true);
                    admin.setRoles(Set.of(Role.ROLE_ADMIN));
                    users.save(admin);
                }
        );
    }
}