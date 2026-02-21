package com.blackrock_hackathon.self_savings_planner.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Self Savings Planner API")
                        .version("1.0")
                        .description("Automated retirement savings through expense-based micro-investments. "
                                + "Rounds up expenses to the nearest 100, applies period-based rules (Q/P/K), "
                                + "and projects returns via NPS or Index fund strategies.")
                        .contact(new Contact().name("BlackRock Hackathon Team")));
    }
}
