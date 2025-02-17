package com.example.demoSpringInitializrForEvoBJ.service;

import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.repository.EvoUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EvoUserService {

    @Autowired
    private EvoUserRepository evoUserRepository;

    public List<EvoUser> getAllPlayers() {
        return evoUserRepository.findAll();
    }

    public EvoUser addEvoUser(String name, int balance) {
        EvoUser player = new EvoUser();
        player.setName(name);
        player.setBalance(balance);
        return evoUserRepository.save(player);
    }
}
