package com.example.demoSpringInitializrForEvoBJ.repository;

import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvoUserRepository extends JpaRepository<EvoUser, Integer> {
    Optional<EvoUser> findByLoginAndPass(String login, String pass);
}
