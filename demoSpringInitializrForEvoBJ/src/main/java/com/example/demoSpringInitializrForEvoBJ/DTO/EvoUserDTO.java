package com.example.demoSpringInitializrForEvoBJ.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvoUserDTO {

    private String playerUUID;

    private String name;
    private String surname;
    private String nickName;
    private int balance;

    public EvoUserDTO(String playerUUID, String name, String surname, String nickName, int balance) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.surname = surname;
        this.nickName = nickName;
        this.balance = balance;
    }

    public void changeBalance(int amount) {
        this.balance += amount;
    }

    public EvoUserDTO() {
    }
}
