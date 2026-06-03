package com.example.backend.config;

import com.example.backend.security.CustomUserDetailsService;
import com.example.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            CustomUserDetailsService uds,
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = uds;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http
    ) throws Exception {

        http
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/resend-verification",
                                "/api/rental/payment/notify"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/verify",
                                "/api/auth/reset-password",
                                "/api/rental-items",
                                "/api/rental-items/categories",
                                "/api/camping-tips",
                                "/api/rental-bookings/availability",
                                "/api/camping-tips/*",
                                "/api/reviews",
                                "/api/camping-tips/slug/*",
                                "/api/terms/active",
                                "/api/products",
                                "/api/products/*",
                                "/uploads/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/reviews")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .requestMatchers("/api/admin/**")
                        .hasAuthority("ROLE_ADMIN")

                        .requestMatchers("/api/customer/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers("/api/v1/chatbot/**").permitAll()

                        .anyRequest().authenticated()
                )
                .authenticationProvider(daoAuthProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}