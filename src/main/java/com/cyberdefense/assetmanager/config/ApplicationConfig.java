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

    // credenziali admin lette dalle variabili d'ambiente
    // se non sono impostate uso questi default per lo sviluppo locale
    @Value("${admin.username:soc_admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            // creo i tre ruoli del sistema se non esistono già
            // ADMIN può fare tutto, ANALYST gestisce ticket e mitigazioni, VIEWER legge soltanto
            Role adminRole = getOrCreateRole(roleRepository, "ADMIN");
            Role analystRole = getOrCreateRole(roleRepository, "ANALYST");
            Role viewerRole = getOrCreateRole(roleRepository, "VIEWER");

            // creo l'utente admin di default
            if (userRepository.findByEmail(adminUsername).isEmpty()) {
                User admin = new User();
                admin.setEmail(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setNome("SOC");
                admin.setCognome("Administrator");
                admin.setRoles(Set.of(adminRole));
                userRepository.save(admin);
            }

            // creo un utente analyst di test per provare il workflow operativo
            if (userRepository.findByEmail("soc_analyst").isEmpty()) {
                User analyst = new User();
                analyst.setEmail("soc_analyst");
                analyst.setPassword(passwordEncoder.encode("analyst123"));
                analyst.setNome("Mario");
                analyst.setCognome("Rossi");
                analyst.setRoles(Set.of(analystRole));
                userRepository.save(analyst);
            }

            // creo un utente viewer di test, accesso in sola lettura
            if (userRepository.findByEmail("soc_viewer").isEmpty()) {
                User viewer = new User();
                viewer.setEmail("soc_viewer");
                viewer.setPassword(passwordEncoder.encode("viewer123"));
                viewer.setNome("Luca");
                viewer.setCognome("Bianchi");
                viewer.setRoles(Set.of(viewerRole));
                userRepository.save(viewer);
            }
        };
    }

    // helper per evitare di duplicare il codice di creazione ruoli
    private Role getOrCreateRole(RoleRepository roleRepository, String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role nuovoRuolo = new Role();
                    nuovoRuolo.setName(name);
                    return roleRepository.save(nuovoRuolo);
                });
    }
}