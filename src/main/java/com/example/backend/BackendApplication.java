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
import com.example.backend.config.AppProperties;

import java.util.Set;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(
            UserRepository users,
            PasswordEncoder encoder,
            AppProperties props
    ) {
        return args -> users.findByEmail(props.getAdmin().getEmail())
                .ifPresentOrElse(
                        u -> {},
                        () -> {
                            User admin = new User();

                            admin.setFirstName(props.getAdmin().getFirstName());
                            admin.setLastName(props.getAdmin().getLastName());
                            admin.setEmail(props.getAdmin().getEmail());
                            admin.setPhone(props.getAdmin().getPhone());

                            admin.setPassword(
                                    encoder.encode(props.getAdmin().getPassword())
                            );

                            admin.setEnabled(true);
                            admin.setRoles(Set.of(Role.ROLE_ADMIN));

                            users.save(admin);
                        }
                );
    }
}