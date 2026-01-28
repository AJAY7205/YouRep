package com.learning.ytrep.config;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.learning.ytrep.model.AppRole;
import com.learning.ytrep.model.Role;
import com.learning.ytrep.model.User;
import com.learning.ytrep.repository.RoleRepository;
import com.learning.ytrep.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Create roles if they don't exist
        createRoleIfNotExists(AppRole.USER);
        createRoleIfNotExists(AppRole.ADMIN);
        createRoleIfNotExists(AppRole.GUEST);

        // Create hardcoded admin user if not exists
        createAdminUserIfNotExists();
    }

    private void createRoleIfNotExists(AppRole appRole) {
        if (roleRepository.findByAppRole(appRole).isEmpty()) {
            Role role = new Role(appRole);
            roleRepository.save(role);
            System.out.println("Created role: " + appRole);
        }
    }

    private void createAdminUserIfNotExists() {
        String adminUsername = "admin";
        String adminEmail = "admin@yourep.com";
        String adminPassword = "Admin@123"; // Change this in production!

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setCreatedAt(LocalDateTime.now());

            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByAppRole(AppRole.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            roles.add(adminRole);
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println("============================================");
            System.out.println("ADMIN USER CREATED");
            System.out.println("Username: " + adminUsername);
            System.out.println("Password: " + adminPassword);
            System.out.println("⚠️  CHANGE PASSWORD IN PRODUCTION!");
            System.out.println("============================================");
        }
    }
}