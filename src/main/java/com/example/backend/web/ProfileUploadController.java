package com.example.backend.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile/upload")
public class ProfileUploadController {

    private static final String UPLOAD_DIR =
            System.getProperty("user.dir") + "/uploads/profile";

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            Files.createDirectories(Paths.get(UPLOAD_DIR));

            String originalName = file.getOriginalFilename();
            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String filename = UUID.randomUUID() + extension;

            Path destination = Paths.get(UPLOAD_DIR).resolve(filename);

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String url = "http://localhost:8080/uploads/profile/" + filename;

            return ResponseEntity.ok(Map.of("url", url));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}