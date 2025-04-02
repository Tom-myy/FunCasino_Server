package com.example.demoSpringInitializrForEvoBJ;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ErrorCodeGenerator {
    public static String generateErrorId(String prefix) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String shortUuid = UUID.randomUUID().toString().substring(0, 6);
        return prefix + "-" + timestamp + "-" + shortUuid;
    }
}

