package com.cyberdefense.assetmanager.config;

import com.cyberdefense.assetmanager.entity.Role;
import com.cyberdefense.assetmanager.entity.User;
import com.cyberdefense.assetmanager.repository.RoleRepository;
import com.cyberdefense.assetmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class ApplicationConfig {

    // Recupera le credenziali admin dalle variabili d'ambiente
    @Value("${admin.username:soc_admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail(adminUsername).isEmpty()) {
                Role adminRole = roleRepository.findByName("ADMIN")
                        .orElseGet(() -> {
                            Role nuovoRuolo = new Role();
                            nuovoRuolo.setName("ADMIN");
                            return roleRepository.save(nuovoRuolo);
                        });

                User admin = new User();
                admin.setEmail(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRoles(Set.of(adminRole));

                userRepository.save(admin);
            }
        };
    }
}