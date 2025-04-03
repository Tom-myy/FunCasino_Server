package com.example.demoSpringInitializrForEvoBJ.player.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class EvoUserDTO {

    private UUID playerUUID;

    private String name;
    private String surname;
    private String nickName;
    private BigDecimal balance;
    private BigDecimal balanceDifference = BigDecimal.ZERO;

    public EvoUserDTO(UUID playerUUID, String name, String surname, String nickName, BigDecimal balance) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.surname = surname;
        this.nickName = nickName;
        this.balance = balance;
    }

    public void changeBalance(BigDecimal amount) {
        this.balance = balance.add(amount);
        balanceDifference = balanceDifference.add(amount);
    }

    public EvoUserDTO() {
    }
}
