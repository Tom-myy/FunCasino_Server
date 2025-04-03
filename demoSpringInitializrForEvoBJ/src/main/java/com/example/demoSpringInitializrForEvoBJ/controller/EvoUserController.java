/*
package com.example.demoSpringInitializrForEvoBJ.controller;

import com.example.demoSpringInitializrForEvoBJ.player.model.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.player.repository.EvoUserRepository;
import com.example.demoSpringInitializrForEvoBJ.player.service.EvoUserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@RestController
@RequestMapping("/players")
public class EvoUserController {

*/
/*    @Autowired
    private EvoUserService evoUserService;

    // Получить всех игроков
    @GetMapping
    public List<EvoUser> getAllPlayers() {
        return evoUserService.getAllPlayers();
    }

    // Добавить нового игрока
    @PostMapping
    public EvoUser addPlayer(@RequestBody EvoUser evoUser) {
        return evoUserService.addEvoUser(evoUser.getName(), evoUser.getBalance());
    }*//*


    private final EvoUserRepository evoUserRepository;

    public EvoUserController(EvoUserRepository evoUserRepository) {
        this.evoUserRepository = evoUserRepository;
    }

    @GetMapping
    public List<EvoUser> getAllPlayers() {
        return evoUserRepository.findAll();
    }
}
*/
