package com.jurol.buy01.common.events;

import java.time.Instant;

public class UserRegisteredEvent {
    private String eventType = "USER_REGISTERED";
    private String userId;
    private String email;
    private String role;
    private Instant timestamp;

    public UserRegisteredEvent() {}

    public UserRegisteredEvent(String userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.timestamp = Instant.now();
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

