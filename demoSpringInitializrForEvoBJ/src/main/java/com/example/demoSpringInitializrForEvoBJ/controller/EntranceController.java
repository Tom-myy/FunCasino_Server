/*
package com.example.demoSpringInitializrForEvoBJ.controller;

import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.repository.EvoUserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/player")
public class EntranceController {

    private final EvoUserRepository evoUserRepository;

    public EntranceController(EvoUserRepository evoUserRepository) {
        this.evoUserRepository = evoUserRepository;
    }

    @GetMapping("/login")
    public EvoUserDTO login(@RequestParam String login, @RequestParam String pass) {
        EvoUser user = evoUserRepository.findByLoginAndPass(login, pass)
                .orElseThrow(() -> new RuntimeException("Invalid login or password"));

        String sessionId = generateUUID();

        return new EvoUserDTO(sessionId, user.getName(), user.getSurname(), user.getNickName(), user.getBalance());
    }



    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

}

*/
