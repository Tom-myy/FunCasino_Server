package com.example.demoSpringInitializrForEvoBJ.repository;

import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EvoUserRepository extends JpaRepository<EvoUser, UUID> {
    Optional<EvoUser> findByLoginAndPass(String login, String pass);
}
