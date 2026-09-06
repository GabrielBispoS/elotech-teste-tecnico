package com.elotech.taskmanager.auth.dto;

public record LoginResponse(String token, long expiresInSeconds, Long userId, String name, String email) {
}
