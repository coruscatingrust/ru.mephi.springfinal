package ru.mephi.booking.controller;

import ru.mephi.booking.domain.User;
import ru.mephi.booking.domain.UserRole;
import ru.mephi.booking.repo.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public User create(@RequestParam @NotBlank String username,
                       @RequestParam @NotBlank String password,
                       @RequestParam(defaultValue = "USER") UserRole role) {
        User u = User.builder().username(username).password(passwordEncoder.encode(password)).role(role).build();
        return userRepository.save(u);
    }

    @PatchMapping
    public User patch(@RequestParam Long id,
                      @RequestParam(required = false) String password,
                      @RequestParam(required = false) UserRole role) {
        User u = userRepository.findById(id).orElseThrow();
        if (password != null) u.setPassword(passwordEncoder.encode(password));
        if (role != null) u.setRole(role);
        return userRepository.save(u);
    }

    @DeleteMapping
    public void delete(@RequestParam Long id) {
        userRepository.deleteById(id);
    }
}
