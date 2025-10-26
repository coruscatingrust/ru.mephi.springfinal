package ru.mephi.booking.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record RegisterRequest(@NotBlank String username, @NotBlank String password) {}
    public record AuthRequest(@NotBlank String username, @NotBlank String password) {}
    public record AuthResponse(String token, String role) {}
}
