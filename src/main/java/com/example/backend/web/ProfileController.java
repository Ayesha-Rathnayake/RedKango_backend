package com.example.backend.web;

import com.example.backend.domain.User;
import com.example.backend.dto.ProfileDto;
import com.example.backend.dto.ApiMessage;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        ProfileDto dto = new ProfileDto();
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());

        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<ApiMessage> updateProfile(
            Authentication authentication,
            @RequestBody ProfileDto dto) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());

        userRepository.save(user);

        return ResponseEntity.ok(new ApiMessage("Profile updated successfully"));
    }
}