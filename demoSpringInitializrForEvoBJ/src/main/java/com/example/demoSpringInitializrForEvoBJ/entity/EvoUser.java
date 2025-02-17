package com.example.demoSpringInitializrForEvoBJ.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"EvoUser\"")
public class EvoUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"userID\"")
    private Long userID;
    @Column(name = "\"name\"")
    private String name;
    @Column(name = "\"surname\"")
    private String surname;
    @Column(name = "\"nickName\"")
    private String nickName;
    @Column(name = "\"phoneNumber\"")
    private String phoneNumber;
    @Column(name = "\"email\"")
    private String email;
    @Column(name = "\"login\"")
    private String login;
    @Column(name = "\"pass\"")
    private String pass;
    @Column(name = "\"balance\"")
    private int balance;


}
