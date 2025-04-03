package com.example.demoSpringInitializrForEvoBJ.timer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "game.timer")
public class TimerConfig {
    private int bettingTime;
    private int decisionTime;
}
