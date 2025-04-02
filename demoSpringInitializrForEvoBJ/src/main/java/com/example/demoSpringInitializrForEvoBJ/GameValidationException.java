package com.example.demoSpringInitializrForEvoBJ;

import lombok.Getter;

@Getter
public class GameValidationException extends RuntimeException {
    private final String code;

    public GameValidationException(String description) {
        this(description, ErrorCodeGenerator.generateErrorId("VAL"));
    }

    public GameValidationException(String description, String code) {
        super(description);
        this.code = code;
    }
}