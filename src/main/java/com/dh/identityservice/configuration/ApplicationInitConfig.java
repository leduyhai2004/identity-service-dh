package com.dh.identityservice.configuration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dh.identityservice.entity.Permission;
import com.dh.identityservice.repository.PermissionRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dh.identityservice.constant.PredefinedRole;
import com.dh.identityservice.entity.Role;
import com.dh.identityservice.entity.User;
import com.dh.identityservice.repository.RoleRepository;
import com.dh.identityservice.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PermissionRepository permissionRepository;

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER_NAME = "admin";

    @NonFinal
    static final String ADMIN_PASSWORD = "admin";

    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driverClassName",
            havingValue = "com.mysql.cj.jdbc.Driver")
    ApplicationRunner applicationRunner(UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        PermissionRepository permissionRepository) {
        log.info("Initializing application.....");
        return args -> {

            //if (userRepository.count() == 0 && roleRepository.count() == 0 && permissionRepository.count() == 0) {
            // chạy init
            Permission readPermission = Permission.builder()
                    .name("READ")
                    .description("Read access")
                    .build();

            Permission writePermission = Permission.builder()
                    .name("WRITE")
                    .description("Write access")
                    .build();

            Permission deletePermission = Permission.builder()
                    .name("DELETE")
                    .description("Delete access")
                    .build();

            // Capture the saved permissions with their assigned IDs
            List<Permission> savedPermissions = permissionRepository.saveAll(List.of(readPermission, writePermission, deletePermission));
            Permission savedReadPermission = savedPermissions.get(0);
            Permission savedWritePermission = savedPermissions.get(1);
            Permission savedDeletePermission = savedPermissions.get(2);

            if (userRepository.findByUsername(ADMIN_USER_NAME).isEmpty()) {
                Role userRole = roleRepository.save(Role.builder()
                        .name(PredefinedRole.USER_ROLE)
                        .description("User role")
                        .permissions(Set.of(savedReadPermission))
                        .build());

                roleRepository.save(Role.builder()
                        .name(PredefinedRole.MANAGER_ROLE)
                        .description("Manager role")
                        .permissions(Set.of(savedReadPermission, savedWritePermission))
                        .build());

                Role adminRole = roleRepository.save(Role.builder()
                        .name(PredefinedRole.ADMIN_ROLE)
                        .description("Admin role")
                        .permissions(Set.of(savedReadPermission, savedWritePermission, savedDeletePermission))
                        .build());

                var roles = new HashSet<Role>();
                roles.add(adminRole);

                User userAdmin = User.builder()
                        .username(ADMIN_USER_NAME)
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .roles(roles)
                        .build();

                userRepository.save(userAdmin);
                log.warn("admin user has been created with default password: admin, please change it");
                List<User> testUsers = new ArrayList<>();
                for (int i = 1; i <= 1000; i++) {
                    User testUser = User.builder()
                            .username("user" + i)
                            .password(passwordEncoder.encode("admin"))
                            .firstName("Test")
                            .lastName("User" + i)
                            .dob(LocalDate.of(1990, 1, 1))
                            .roles(Set.of(userRole))
                            .build();
                    testUsers.add(testUser);
                }

                userRepository.saveAll(testUsers);
                log.info("Created 1000 test users");
                log.info("Application initialization completed .....");
            }
            //}
        };
    }
}
