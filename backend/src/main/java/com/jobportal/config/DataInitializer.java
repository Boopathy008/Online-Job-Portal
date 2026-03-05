package com.jobportal.config;

import com.jobportal.entity.Role;
import com.jobportal.entity.User;
import com.jobportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            System.out.println("--- Database Initialization Start ---");
            long userCount = userRepository.count();
            System.out.println("Total users in database: " + userCount);

            // Create Admin only if missing
            if (!userRepository.existsByEmail("admin@jobhub.com")) {
                User adminUser = new User(
                        "Admin1",
                        "admin@jobhub.com",
                        passwordEncoder.encode("1234567898"),
                        Role.ROLE_ADMIN);
                userRepository.save(adminUser);
                System.out.println("Default Admin account created: admin@jobhub.com / 1234567898");
            } else {
                System.out.println("Admin account already exists. Skipping creation.");
            }

            // Sync existing recruiters who might have NULL status
            int syncCount = 0;
            for (User u : userRepository.findAll()) {
                if (u.getRole() == Role.ROLE_RECRUITER && u.getVerificationStatus() == null) {
                    u.setVerificationStatus(com.jobportal.entity.VerificationStatus.PENDING);
                    userRepository.save(u);
                    syncCount++;
                }
            }
            if (syncCount > 0) {
                System.out.println("Synced status for " + syncCount + " recruiters.");
            }

            System.out.println("--- Database Initialization Complete ---");
        };
    }
}
