package com.projet.auth;

import com.projet.auth.model.Admin;
import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.AdminRepository;
import com.projet.auth.repository.SuperviseurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final SuperviseurRepository superviseurRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.initial-email}")
    private String initialEmail;

    @Value("${app.admin.initial-password}")
    private String initialPassword;

    public AdminSeeder(
            SuperviseurRepository superviseurRepository,
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.superviseurRepository = superviseurRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
    }

    @Transactional
    protected void seedAdmin() {
        // Check if admin already exists OR email already exists
        if (superviseurRepository.existsByAdminIsNotNull() || superviseurRepository.findByEmail(initialEmail).isPresent()) {
            log.debug("Admin déjà présent ou email déjà utilisé, seed ignoré");
            return;
        }

        log.info("Création de l'admin initial...");

        Superviseur superviseur = new Superviseur();
        superviseur.setEmail(initialEmail);
        superviseur.setMotDePasseHash(passwordEncoder.encode(initialPassword));
        superviseur.setNom("Rohand");
        superviseur.setPrenom("Douae");
        superviseur.setPhone("06 12 34 56 78");
        superviseur.setActif(true);
        superviseur.setMustChangePassword(true);

        Superviseur savedSuperviseur = superviseurRepository.save(superviseur);

        Admin admin = new Admin(savedSuperviseur);
        adminRepository.save(admin);

        log.info("Admin initial créé avec succès pour l'email : {}", initialEmail);
    }
}
