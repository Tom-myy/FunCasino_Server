package com.example.demoSpringInitializrForEvoBJ.player.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {
    private String nickname;
    private String password;

    public LoginRequestDTO() {}

    public LoginRequestDTO(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
    }
}
