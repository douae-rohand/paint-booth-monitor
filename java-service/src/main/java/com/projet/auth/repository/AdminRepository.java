package com.projet.auth.repository;

import com.projet.auth.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
}
