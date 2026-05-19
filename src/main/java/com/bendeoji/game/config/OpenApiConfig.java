package com.bendeoji.game.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bendeojiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bendeoji Whack-a-Mole API")
                        .version("v1")
                        .description("Game session, hit scoring, and leaderboard APIs."));
    }
}
