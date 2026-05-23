package com.example.backend.dto;

import java.time.Instant;

public class AdminUserResponse {
    private Long id;
    private String userId;
    private String name;
    private String email;
    private String phone;
    private Instant joined;
    private boolean active;
    private boolean locked;

    public AdminUserResponse(
            Long id,
            String userId,
            String name,
            String email,
            String phone,
            Instant joined,
            boolean active,
            boolean locked
    ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joined = joined;
        this.active = active;
        this.locked = locked;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Instant getJoined() { return joined; }
    public boolean isActive() { return active; }
    public boolean isLocked() { return locked; }
}