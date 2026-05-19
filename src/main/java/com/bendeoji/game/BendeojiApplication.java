package com.bendeoji.game;

import com.bendeoji.game.config.GameProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(GameProperties.class)
public class BendeojiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BendeojiApplication.class, args);
    }
}
