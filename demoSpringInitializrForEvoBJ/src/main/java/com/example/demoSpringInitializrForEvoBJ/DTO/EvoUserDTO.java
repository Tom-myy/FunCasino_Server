package com.example.demoSpringInitializrForEvoBJ.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EvoUserDTO {

    private UUID playerUUID;

    private String name;
    private String surname;
    private String nickName;
    private int balance;
    private int balanceDifference;

    public EvoUserDTO(UUID playerUUID, String name, String surname, String nickName, int balance) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.surname = surname;
        this.nickName = nickName;
        this.balance = balance;
    }

    public void changeBalance(int amount) {
        this.balance += amount;
        balanceDifference += amount;
    }

    public EvoUserDTO() {
    }
}
