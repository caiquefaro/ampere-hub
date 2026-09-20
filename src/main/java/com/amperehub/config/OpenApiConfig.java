package com.amperehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI amperehubOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Ampère Hub API")
                .version("1.0.0")
                .description("Cálculos de consumo, tarifas, geração fotovoltaica, "
                        + "dimensionamento de condutores e correção de fator de potência. "
                        + "Resultados são estimativas de estudo preliminar."));
    }
}
