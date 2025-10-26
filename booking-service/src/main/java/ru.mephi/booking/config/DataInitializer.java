package ru.mephi.booking.config;

import ru.mephi.booking.domain.User;
import ru.mephi.booking.domain.UserRole;
import ru.mephi.booking.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            users.findByUsername("admin").orElseGet(() -> {
                User admin = User.builder()
                        .username("admin")
                        .password(encoder.encode("admin"))
                        .role(UserRole.ADMIN)
                        .build();
                users.save(admin);
                log.info("Created default admin user 'admin' / 'admin'");
                return admin;
            });
        };
    }
}
