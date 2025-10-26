package ru.mephi.booking.service;

import ru.mephi.booking.domain.User;
import ru.mephi.booking.domain.UserRole;
import ru.mephi.booking.dto.AuthDtos;
import ru.mephi.booking.exception.ConflictException;
import ru.mephi.booking.exception.UnauthorizedException;
import ru.mephi.booking.repo.UserRepository;
import ru.mephi.booking.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        userRepository.findByUsername(req.username()).ifPresent(u -> { throw new ConflictException("Username taken"); });
        User user = User.builder().username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .role(UserRole.USER).build();
        userRepository.save(user);
        String token = jwtUtil.generate(user.getUsername(), List.of(user.getRole().name()));
        return new AuthDtos.AuthResponse(token, user.getRole().name());
    }

    public AuthDtos.AuthResponse auth(AuthDtos.AuthRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new UnauthorizedException("Bad credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) throw new UnauthorizedException("Bad credentials");
        String token = jwtUtil.generate(user.getUsername(), List.of(user.getRole().name()));
        return new AuthDtos.AuthResponse(token, user.getRole().name());
    }
}
