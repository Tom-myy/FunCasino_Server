package com.example.demoSpringInitializrForEvoBJ.myPackage;

import com.example.demoSpringInitializrForEvoBJ.DTO.EvoUserDTO;
import com.example.demoSpringInitializrForEvoBJ.DTO.LoginRequestDTO;
import com.example.demoSpringInitializrForEvoBJ.entity.EvoUser;
import com.example.demoSpringInitializrForEvoBJ.repository.EvoUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import static java.util.UUID.randomUUID;

public class MessageProcessor {
    private final EvoUserRepository evoUserRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageProcessor(EvoUserRepository evoUserRepository) {
        this.evoUserRepository = evoUserRepository;
    }

    public MyPackage handleAuthorization(MyPackage<?> myPackage, String clientUUID) {

        LoginRequestDTO loginRequest = objectMapper.convertValue(myPackage.getMessage(), LoginRequestDTO.class);

        // Взаимодействие с БД
        Optional<EvoUser> optionalUser = evoUserRepository.findByLoginAndPass(
                loginRequest.getNickname(),
                loginRequest.getPassword()
        );

        if (optionalUser.isPresent()) {
            EvoUser user = optionalUser.get();

            // Создание ответа
            EvoUserDTO responseUser = new EvoUserDTO(
//                    randomUUID().toString(),
                    clientUUID,
                    user.getName(),
                    user.getSurname(),
                    user.getNickName(),
                    user.getBalance()
            );

            // Отправка успешного ответа
            return new MyPackage<>(responseUser, EMessageType.AUTHORIZATION);
        } else {
            // Отправка ошибки
            return new MyPackage<>("Invalid login or password", EMessageType.AUTHORIZATION_ERROR);
        }
    }

    public MyPackage handleSeat(MyPackage<?> myPackage) {

        return new MyPackage<>("", EMessageType.TAKE_SEAT);

    }
}
