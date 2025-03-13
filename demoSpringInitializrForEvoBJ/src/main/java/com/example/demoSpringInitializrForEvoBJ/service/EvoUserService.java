package com.example.demoSpringInitializrForEvoBJ.service;

import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.Player;
import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.repository.EvoUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.lang.foreign.PaddingLayout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvoUserService {

    private EvoUserRepository evoUserRepo;

    public EvoUserService (EvoUserRepository evoUserRepo) {
        this.evoUserRepo = evoUserRepo;
    }

    public List<EvoUser> getAllPlayers() {
        return evoUserRepo.findAll();
    }

    public EvoUser addEvoUser(String name, int balance) {
        EvoUser player = new EvoUser();
        player.setName(name);
        player.setBalance(balance);
        return evoUserRepo.save(player);
    }

    public Optional<EvoUser> findByLoginAndPass(String login, String pass) {
        return evoUserRepo.findByLoginAndPass(login, pass);
    }

/*    @Transactional
    public void updateBalance(UUID userId, int newBalance) {
        EvoUser user = evoUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setBalance(newBalance);
        evoUserRepo.save(user);
    }*/

    public void updateUsersAfterGame(List<Player> players) {
        for (Player p : players) {
            EvoUser user = evoUserRepo.findById(p.getEvoUserDTO().getPlayerUUID())
                    .orElseThrow(() -> new RuntimeException("User not found"));

//            user.setBalance(user.getBalance() + dto.getBalanceChange());
            user.setBalance(user.getBalance() + p.getEvoUserDTO().getBalanceDifference());
//            user.setTotalWins(dto.getTotalWins()); additional details

            evoUserRepo.save(user);
            p.resetBalanceDifference();
        }
    }

}
