package com.example.demoSpringInitializrForEvoBJ.common;

import lombok.Getter;

/*
@Getter
public class GameSystemException extends Exception {
    private static final String DEFAULT_DESCRIPTION = "Some system error on the server. Try again later or contact support. Error code: ";
    private final String code;

    public GameSystemException() {
        this(ErrorCodeGenerator.generateErrorId("SYS"));
    }

    private GameSystemException(String code) {
        super(DEFAULT_DESCRIPTION + code);
        this.code = code;
    }
}*/

@Getter
public class GameSystemException extends Exception {
    private static final String USER_DEFAULT_DESCRIPTION = "Some system error on the server. Try again later or contact support. Error code: ";
    private final String code;
    private final String internalDescription;

    public GameSystemException(String internalDescription) {
        this.code = ErrorCodeGenerator.generateErrorId("SYS");
        this.internalDescription = internalDescription;
    }

    @Override
    public String getMessage() {
        // for developers
        return "ERROR-CODE: " + code + " - " + internalDescription;
    }

    public String getUserDescription() {
        return USER_DEFAULT_DESCRIPTION + code;
    }
}