package ru.mephi.booking.controller;

import ru.mephi.booking.dto.AuthDtos;
import ru.mephi.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return userService.register(req);
    }

    @PostMapping("/auth")
    public AuthDtos.AuthResponse auth(@Valid @RequestBody AuthDtos.AuthRequest req) {
        return userService.auth(req);
    }
}
