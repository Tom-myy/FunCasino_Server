/*
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
*/


package com.example.demoSpringInitializrForEvoBJ.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"EvoUser\"")
public class EvoUser {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "\"userID\"", updatable = false, nullable = false, unique = true)
    private UUID userID;

    @Column(name = "\"name\"", nullable = false, length = 128)
    private String name;

    @Column(name = "\"surname\"", nullable = false, length = 128)
    private String surname;

    @Column(name = "\"nickName\"", nullable = false, unique = true, length = 50)
    private String nickName;

    @Column(name = "\"phoneNumber\"", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "\"email\"", nullable = false, length = 100)
    private String email;

    @Column(name = "\"login\"", nullable = false, length = 64)
    private String login;

    @Column(name = "\"pass\"", nullable = false, length = 60) // Для bcrypt-хэша
    private String pass;

    @Column(name = "\"balance\"", nullable = false)
    private int balance = 1000;

    @PrePersist
    protected void onCreate() {
        if (userID == null) {
            userID = UUID.randomUUID();
        }
    }
}
